package com.uem.ambulancias.evidencias.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.uem.ambulancias.comun.error.NoEncontradoException;
import com.uem.ambulancias.emergencias.domain.Incidente;
import com.uem.ambulancias.emergencias.repository.AlertaRepository;
import com.uem.ambulancias.emergencias.repository.IncidenteRepository;
import com.uem.ambulancias.evidencias.domain.AnalisisEvidencia;
import com.uem.ambulancias.evidencias.domain.DatosClaveDelResumen;
import com.uem.ambulancias.evidencias.domain.ResumenIncidente;
import com.uem.ambulancias.evidencias.domain.TrabajoIa;
import com.uem.ambulancias.evidencias.repository.AnalisisEvidenciaRepository;
import com.uem.ambulancias.evidencias.repository.EvidenciaRepository;
import com.uem.ambulancias.evidencias.repository.ResumenIncidenteRepository;
import com.uem.ambulancias.evidencias.repository.TrabajoIaRepository;
import com.uem.ambulancias.usuarios.domain.RolUsuario;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

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
	private final AccesoAlIncidente acceso;
	private final ApplicationEventPublisher eventos;
	private final JsonMapper json;

	/**
	 * Lo que ve el personal: la versión vigente, si hay, las evidencias subidas y la transcripción y la línea de tiempo
	 * de las que ya tienen análisis. 404 si el incidente no existe; 403 si es un paramédico que no lo está atendiendo.
	 */
	@Transactional(readOnly = true)
	public ResumenConEvidencias consultar(Long incidenteId, Long usuarioId, RolUsuario rol) {
		if (!incidentes.existsById(incidenteId)) {
			throw new NoEncontradoException("No existe el incidente " + incidenteId + ".");
		}
		acceso.exigir(incidenteId, usuarioId, rol);
		Map<Long, TranscripcionDeEvidencia> transcripciones = analisis.buscarVigentesPorIncidente(incidenteId).stream()
				.collect(Collectors.toMap(uno -> uno.getEvidencia().getId(), this::transcripcionDe));
		return new ResumenConEvidencias(resumenes.findFirstByIncidenteIdOrderByVersionDesc(incidenteId).orElse(null),
				evidencias.buscarSubidasPorIncidente(incidenteId), transcripciones);
	}

	/**
	 * Saca {@code transcript} y {@code timeline} del JSON guardado. Un texto vacío o una lista sin momentos cuentan
	 * como ausentes. Si el JSON no se puede leer, la evidencia se muestra igual, sin esas partes.
	 */
	private TranscripcionDeEvidencia transcripcionDe(AnalisisEvidencia uno) {
		JsonNode raiz;
		try {
			raiz = json.readTree(uno.getAnalisis());
		} catch (JacksonException e) {
			log.warn("No se pudo leer el análisis de la evidencia {}: se muestra sin transcripción.",
					uno.getEvidencia().getId());
			return TranscripcionDeEvidencia.VACIA;
		}
		JsonNode transcript = raiz.path("transcript");
		String transcripcion = transcript.isString() && !transcript.asString().isBlank() ? transcript.asString()
				: null;
		List<TranscripcionDeEvidencia.Momento> momentos = new ArrayList<>();
		for (JsonNode momento : raiz.path("timeline")) {
			JsonNode segundo = momento.path("startSecond");
			JsonNode texto = momento.path("text");
			if (segundo.isNumber() && texto.isString() && !texto.asString().isBlank()) {
				momentos.add(new TranscripcionDeEvidencia.Momento(segundo.asDouble(), texto.asString()));
			}
		}
		return new TranscripcionDeEvidencia(transcripcion, momentos.isEmpty() ? null : List.copyOf(momentos));
	}

	/**
	 * Todas las alertas del incidente y todos sus análisis vigentes, con el JSON de cada análisis tal como se guardó.
	 * Vacío si todavía no hay ningún análisis: el trabajo se cierra sin llamar al servicio, que no tendría qué resumir.
	 *
	 * <p>Una alerta retirada o descartada va sin su descripción ni su cantidad de afectados: ese texto ya no describe
	 * el incidente. La alerta sigue en el pedido porque sus evidencias, si las tiene, siguen siendo lo que se vio, y
	 * porque la regla de versiones exige que una propuesta conserve las fuentes de la vigente.
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
						alerta.sigueEnPie() ? alerta.getDescripcion() : null,
						alerta.sigueEnPie() ? alerta.getCantidadAfectados() : null, alerta.getEmisorEsPaciente()))
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
	 * versión nueva se avisa después del commit, marcada como importante si cambió algo que la tripulación tiene que
	 * saber ({@link DatosClaveDelResumen}).
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
		boolean importante = vigente == null || cambioImportante(nuevo, vigente);
		eventos.publishEvent(new ResumenActualizado(incidenteId, nuevo.getVersion(), importante));
		return Optional.of(nuevo);
	}

	/** Si alguna de las dos versiones no se puede leer, se toma como importante: ante la duda, se avisa. */
	private boolean cambioImportante(ResumenIncidente nuevo, ResumenIncidente anterior) {
		Optional<DatosClaveDelResumen> datosNuevos = datosClaveDe(nuevo);
		Optional<DatosClaveDelResumen> datosAnteriores = datosClaveDe(anterior);
		if (datosNuevos.isEmpty() || datosAnteriores.isEmpty()) {
			return true;
		}
		return datosNuevos.get().cambioImportanteRespectoDe(datosAnteriores.get());
	}

	private Optional<DatosClaveDelResumen> datosClaveDe(ResumenIncidente version) {
		JsonNode raiz;
		try {
			raiz = json.readTree(version.getResumen());
		} catch (JacksonException e) {
			log.warn("No se pudo leer la versión {} del resumen del incidente {}.", version.getVersion(),
					version.getIncidente().getId());
			return Optional.empty();
		}
		Set<String> peligros = new TreeSet<>();
		for (JsonNode peligro : raiz.path("hazards")) {
			if (peligro.isString()) {
				peligros.add(peligro.asString());
			}
		}
		JsonNode personas = raiz.path("people");
		return Optional.of(new DatosClaveDelResumen(texto(raiz.path("severity").path("level")),
				texto(raiz.path("eventType")), entero(personas.path("min")), entero(personas.path("max")), peligros));
	}

	private static String texto(JsonNode nodo) {
		return nodo.isString() ? nodo.asString() : null;
	}

	private static Integer entero(JsonNode nodo) {
		return nodo.isIntegralNumber() ? nodo.asInt() : null;
	}

}
