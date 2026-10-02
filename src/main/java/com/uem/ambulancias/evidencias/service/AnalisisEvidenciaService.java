package com.uem.ambulancias.evidencias.service;

import java.time.Instant;
import java.util.Optional;

import com.uem.ambulancias.comun.error.NoEncontradoException;
import com.uem.ambulancias.evidencias.domain.AnalisisEvidencia;
import com.uem.ambulancias.evidencias.domain.EstadoEvidencia;
import com.uem.ambulancias.evidencias.domain.Evidencia;
import com.uem.ambulancias.evidencias.domain.TrabajoIa;
import com.uem.ambulancias.evidencias.repository.AnalisisEvidenciaRepository;
import com.uem.ambulancias.evidencias.repository.EvidenciaRepository;
import com.uem.ambulancias.evidencias.repository.TrabajoIaRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Las dos puntas de un análisis, cada una en su transacción corta: armar el pedido antes de llamar al servicio y
 * guardar lo que respondió. La llamada, que puede tardar minutos, queda en el medio y sin transacción.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalisisEvidenciaService {

	private final TrabajoIaRepository trabajos;
	private final EvidenciaRepository evidencias;
	private final AnalisisEvidenciaRepository analisis;
	private final AlmacenDeEvidencias almacen;
	private final EvidenciaProperties config;
	private final ColaDeTrabajosIa cola;

	/**
	 * El pedido con una URL de lectura recién firmada, que dura más que la llamada más larga. Vacío si la evidencia ya
	 * no espera análisis: el trabajo se cierra sin llamar a nadie.
	 */
	@Transactional
	public Optional<PedidoDeAnalisis> prepararPedido(Long trabajoId) {
		TrabajoIa trabajo = trabajos.findById(trabajoId)
				.orElseThrow(() -> new NoEncontradoException("No existe el trabajo " + trabajoId + "."));
		Evidencia evidencia = trabajo.getEvidencia();
		if (evidencia.getEstado() != EstadoEvidencia.SUBIDA) {
			log.info("La evidencia {} ya no espera análisis ({}): se cierra el trabajo {}.", evidencia.getId(),
					evidencia.getEstado(), trabajoId);
			trabajo.completar(Instant.now());
			return Optional.empty();
		}
		LecturaFirmada lectura = almacen.firmarLectura(evidencia.getClaveObjeto(), config.vigenciaLectura());
		return Optional.of(new PedidoDeAnalisis(
				trabajo.getReferencia(),
				evidencia.getId(),
				evidencia.getAlerta().getId(),
				trabajo.getIncidente().getId(),
				lectura.url(),
				evidencia.getMimeType(),
				evidencia.getSha256()));
	}

	/**
	 * Guarda el análisis, deja la evidencia ANALIZADA, cierra el trabajo y pide rehacer el resumen del incidente, todo
	 * junto. Si mientras tanto el trabajo dejó de ser de este worker (su bloqueo venció y otro lo tomó), no se toca
	 * nada: el otro va a guardar lo suyo.
	 */
	@Transactional
	public void guardar(Long trabajoId, AnalisisRecibido recibido) {
		TrabajoIa trabajo = trabajos.buscarParaActualizar(trabajoId)
				.orElseThrow(() -> new NoEncontradoException("No existe el trabajo " + trabajoId + "."));
		if (!trabajo.isEnCurso()) {
			log.warn("El trabajo {} ya no estaba en curso al llegar su análisis: se descarta.", trabajoId);
			return;
		}
		Instant ahora = Instant.now();
		Long evidenciaId = trabajo.getEvidencia().getId();
		Evidencia evidencia = evidencias.buscarParaActualizar(evidenciaId)
				.orElseThrow(() -> new NoEncontradoException("No existe la evidencia " + evidenciaId + "."));
		if (evidencia.getEstado() == EstadoEvidencia.SUBIDA && !analisis.existsByEvidenciaId(evidenciaId)) {
			analisis.save(AnalisisEvidencia.registrar(evidencia, trabajo.getReferencia(), recibido, ahora));
			evidencia.marcarAnalizada(ahora);
		}
		trabajo.completar(ahora);
		cola.pedirResumen(trabajo.getIncidente().getId());
	}

}
