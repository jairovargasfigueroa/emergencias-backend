package com.uem.ambulancias.evidencias.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.uem.ambulancias.comun.error.NoEncontradoException;
import com.uem.ambulancias.emergencias.domain.Incidente;
import com.uem.ambulancias.emergencias.repository.IncidenteRepository;
import com.uem.ambulancias.evidencias.domain.EstadoEvidencia;
import com.uem.ambulancias.evidencias.domain.EstadoTrabajoIa;
import com.uem.ambulancias.evidencias.domain.Evidencia;
import com.uem.ambulancias.evidencias.domain.TipoTrabajoIa;
import com.uem.ambulancias.evidencias.domain.TrabajoIa;
import com.uem.ambulancias.evidencias.repository.TrabajoIaRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * La cola de pedidos al servicio de análisis. Encolar va siempre en la transacción de quien lo pide, así el trabajo
 * existe si y solo si existe lo que lo originó. Tomar y cerrar van cada uno en su transacción corta: la llamada al
 * servicio, que puede tardar minutos, queda afuera de las dos.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ColaDeTrabajosIa {

	private final TrabajoIaRepository trabajos;
	private final IncidenteRepository incidentes;
	private final TrabajoIaProperties config;

	/** Una evidencia recién confirmada se analiza. */
	@Transactional
	public TrabajoIa encolarAnalisis(Evidencia evidencia) {
		return trabajos.save(TrabajoIa.analizar(evidencia, Instant.now()));
	}

	/**
	 * Pide rehacer el resumen del incidente. Si ya hay uno esperando, alcanza con ese: cuando corra va a juntar todo lo
	 * que haya en ese momento, incluido lo nuevo. El incidente se bloquea para que dos pedidos simultáneos no dejen dos
	 * en la cola. Uno que ya está EN_CURSO no cuenta, porque juntó sus datos antes de esta novedad.
	 */
	@Transactional
	public void pedirResumen(Long incidenteId) {
		Incidente incidente = incidentes.buscarParaActualizar(incidenteId)
				.orElseThrow(() -> new NoEncontradoException("No existe el incidente " + incidenteId + "."));
		if (trabajos.existsByIncidenteIdAndTipoAndEstado(incidenteId, TipoTrabajoIa.RESUMEN,
				EstadoTrabajoIa.PENDIENTE)) {
			return;
		}
		trabajos.save(TrabajoIa.resumir(incidente, Instant.now()));
	}

	/**
	 * Toma hasta {@code limite} trabajos de ese tipo y los deja EN_CURSO. Uno que vuelve a la cola porque su worker se
	 * cayó y ya gastó todos sus intentos no se toma: se da por fallido.
	 */
	@Transactional
	public List<TrabajoIa> tomar(TipoTrabajoIa tipo, int limite) {
		Instant ahora = Instant.now();
		List<TrabajoIa> tomados = new ArrayList<>();
		for (TrabajoIa trabajo : trabajos.tomarDisponibles(tipo.name(), ahora, limite)) {
			if (trabajo.isEnCurso() && trabajo.agotoIntentos(config.maximoIntentos())) {
				log.warn("Trabajo {} ({}) abandonado a mitad demasiadas veces: se da por fallido.", trabajo.getId(),
						tipo);
				terminarSinExito(trabajo, "worker_sin_respuesta", ahora);
				continue;
			}
			trabajo.tomar(ahora, config.bloqueo());
			tomados.add(trabajo);
		}
		return tomados;
	}

	/** El trabajo salió bien. */
	@Transactional
	public void completar(Long trabajoId) {
		trabajos.buscarParaActualizar(trabajoId).ifPresent(trabajo -> trabajo.completar(Instant.now()));
	}

	/** Falló algo pasajero: vuelve a la cola con una espera más larga, salvo que ya no le queden intentos. */
	@Transactional
	public void reintentar(Long trabajoId, String error) {
		trabajos.buscarParaActualizar(trabajoId).ifPresent(trabajo -> {
			Instant ahora = Instant.now();
			if (trabajo.agotoIntentos(config.maximoIntentos())) {
				log.warn("Trabajo {} sin más intentos tras {}: se da por fallido.", trabajoId, error);
				terminarSinExito(trabajo, error, ahora);
			} else {
				trabajo.reintentar(error, ahora, config.esperaTras(trabajo.getIntentos()));
			}
		});
	}

	/** Falló por la evidencia o por el pedido: reintentar daría lo mismo. */
	@Transactional
	public void fallar(Long trabajoId, String error) {
		trabajos.buscarParaActualizar(trabajoId)
				.ifPresent(trabajo -> terminarSinExito(trabajo, error, Instant.now()));
	}

	/** Falló por la configuración del servicio: no se reintenta solo, lo tiene que revisar una persona. */
	@Transactional
	public void pasarARevision(Long trabajoId, String error) {
		trabajos.buscarParaActualizar(trabajoId).ifPresent(trabajo -> {
			log.error("Trabajo {} detenido para revisión: {}.", trabajoId, error);
			trabajo.pasarARevision(error, Instant.now());
		});
	}

	/** Un análisis que no se va a hacer deja la evidencia FALLIDA: el archivo sigue para el personal, sin análisis. */
	private static void terminarSinExito(TrabajoIa trabajo, String error, Instant ahora) {
		trabajo.fallar(error, ahora);
		Evidencia evidencia = trabajo.getEvidencia();
		if (evidencia != null && evidencia.getEstado() == EstadoEvidencia.SUBIDA) {
			evidencia.marcarFallida(ahora);
		}
	}

}
