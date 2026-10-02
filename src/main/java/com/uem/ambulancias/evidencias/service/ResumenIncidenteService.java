package com.uem.ambulancias.evidencias.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.uem.ambulancias.comun.error.NoEncontradoException;
import com.uem.ambulancias.emergencias.domain.Incidente;
import com.uem.ambulancias.emergencias.repository.AlertaRepository;
import com.uem.ambulancias.emergencias.repository.IncidenteRepository;
import com.uem.ambulancias.evidencias.domain.AnalisisEvidencia;
import com.uem.ambulancias.evidencias.domain.ResumenIncidente;
import com.uem.ambulancias.evidencias.domain.TrabajoIa;
import com.uem.ambulancias.evidencias.repository.AnalisisEvidenciaRepository;
import com.uem.ambulancias.evidencias.repository.EvidenciaRepository;
import com.uem.ambulancias.evidencias.repository.ResumenIncidenteRepository;
import com.uem.ambulancias.evidencias.repository.TrabajoIaRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Las dos puntas de un resumen, cada una en su transacción corta: juntar las fuentes antes de llamar al servicio y
 * decidir después si la propuesta pasa a ser la versión vigente.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResumenIncidenteService {

	private final TrabajoIaRepository trabajos;
	private final IncidenteRepository incidentes;
	private final AlertaRepository alertas;
	private final AnalisisEvidenciaRepository analisis;
	private final ResumenIncidenteRepository resumenes;
	private final EvidenciaRepository evidencias;
	private final ApplicationEventPublisher eventos;

	/** Lo que ve el personal: la versión vigente, si hay, y las evidencias subidas. 404 si el incidente no existe. */
	@Transactional(readOnly = true)
	public ResumenConEvidencias consultar(Long incidenteId) {
		if (!incidentes.existsById(incidenteId)) {
			throw new NoEncontradoException("No existe el incidente " + incidenteId + ".");
		}
		return new ResumenConEvidencias(resumenes.findFirstByIncidenteIdOrderByVersionDesc(incidenteId).orElse(null),
				evidencias.buscarSubidasPorIncidente(incidenteId));
	}

	/**
	 * Todas las alertas del incidente y todos sus análisis vigentes, con el JSON de cada análisis tal como se guardó.
	 * Vacío si todavía no hay ningún análisis: el trabajo se cierra sin llamar al servicio, que no tendría qué resumir.
	 */
	@Transactional
	public Optional<PedidoDeResumen> prepararPedido(Long trabajoId) {
		TrabajoIa trabajo = trabajos.findById(trabajoId)
				.orElseThrow(() -> new NoEncontradoException("No existe el trabajo " + trabajoId + "."));
		Long incidenteId = trabajo.getIncidente().getId();
		List<AnalisisEvidencia> vigentes = analisis.buscarVigentesPorIncidente(incidenteId);
		if (vigentes.isEmpty()) {
			log.info("El incidente {} no tiene análisis todavía: se cierra el trabajo {} sin resumir.", incidenteId,
					trabajoId);
			trabajo.completar(Instant.now());
			return Optional.empty();
		}
		List<PedidoDeResumen.AlertaDelIncidente> alertasDelIncidente = alertas.buscarPorIncidente(incidenteId).stream()
				.map(alerta -> new PedidoDeResumen.AlertaDelIncidente(alerta.getId(), alerta.getFechaHora(),
						alerta.getDescripcion(), alerta.getCantidadAfectados(), alerta.getEmisorEsPaciente()))
				.toList();
		List<PedidoDeResumen.EvidenciaAnalizada> analizadas = vigentes.stream()
				.map(uno -> new PedidoDeResumen.EvidenciaAnalizada(uno.getEvidencia().getId(),
						uno.getEvidencia().getAlerta().getId(), uno.getModalidad(), uno.getEvidencia().getSubidaEn(),
						uno.getAnalisis()))
				.toList();
		return Optional.of(new PedidoDeResumen(incidenteId, alertasDelIncidente, analizadas));
	}

	/**
	 * Guarda la propuesta como versión nueva si mejora a la vigente ({@link ResumenIncidente#seReemplazaCon}) y cierra
	 * el trabajo. Con el incidente bloqueado: dos resúmenes del mismo incidente que terminan a la vez se comparan de a
	 * uno contra la versión que dejó el otro. Devuelve la versión guardada, o vacío si la propuesta no la mejoraba. La
	 * versión nueva se avisa después del commit.
	 */
	@Transactional
	public Optional<ResumenIncidente> guardar(Long trabajoId, ResumenRecibido recibido) {
		TrabajoIa trabajo = trabajos.buscarParaActualizar(trabajoId)
				.orElseThrow(() -> new NoEncontradoException("No existe el trabajo " + trabajoId + "."));
		if (!trabajo.isEnCurso()) {
			log.warn("El trabajo {} ya no estaba en curso al llegar su resumen: se descarta.", trabajoId);
			return Optional.empty();
		}
		Instant ahora = Instant.now();
		Long incidenteId = trabajo.getIncidente().getId();
		Incidente incidente = incidentes.buscarParaActualizar(incidenteId)
				.orElseThrow(() -> new NoEncontradoException("No existe el incidente " + incidenteId + "."));
		trabajo.completar(ahora);

		ResumenIncidente vigente = resumenes.findFirstByIncidenteIdOrderByVersionDesc(incidenteId).orElse(null);
		if (vigente != null && !vigente.seReemplazaCon(recibido.evidenciasUsadas(), recibido.alertasUsadas())) {
			log.info("El resumen del trabajo {} no mejora la versión {} del incidente {}: no se guarda.", trabajoId,
					vigente.getVersion(), incidenteId);
			return Optional.empty();
		}
		ResumenIncidente nuevo = resumenes.save(
				ResumenIncidente.nuevaVersion(incidente, vigente, trabajo.getReferencia(), recibido, ahora));
		eventos.publishEvent(new ResumenActualizado(incidenteId, nuevo.getVersion()));
		return Optional.of(nuevo);
	}

}
