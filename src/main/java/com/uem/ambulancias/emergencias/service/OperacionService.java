package com.uem.ambulancias.emergencias.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.uem.ambulancias.emergencias.domain.Atencion;
import com.uem.ambulancias.emergencias.domain.Incidente;
import com.uem.ambulancias.emergencias.dto.AtencionEnCursoResponse;
import com.uem.ambulancias.emergencias.dto.EventoDeOperacionResponse;
import com.uem.ambulancias.emergencias.dto.IncidenteSinCubrirResponse;
import com.uem.ambulancias.emergencias.dto.OperacionResponse;
import com.uem.ambulancias.emergencias.dto.TrasladoDelPanelResponse;
import com.uem.ambulancias.emergencias.dto.UnidadEnOperacionResponse;
import com.uem.ambulancias.emergencias.repository.AlertaRepository;
import com.uem.ambulancias.emergencias.repository.AtencionRepository;
import com.uem.ambulancias.emergencias.repository.IncidenteRepository;
import com.uem.ambulancias.emergencias.repository.ReferenciaDeIncidente;
import com.uem.ambulancias.flota.domain.Ambulancia;
import com.uem.ambulancias.flota.domain.Asignacion;
import com.uem.ambulancias.flota.domain.Turno;
import com.uem.ambulancias.flota.repository.AsignacionRepository;
import com.uem.ambulancias.flota.repository.TurnoRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * La foto completa de la operación para el centro de control. Vive en emergencias porque casi todo lo que muestra
 * —atenciones, incidentes y traslados— es de acá, y porque este módulo ya depende de flota: al revés habría que
 * meter incidentes y traslados adentro de la flota, que no tiene por qué saber de ellos.
 *
 * <p>Todo se arma por lotes. La pantalla se refresca sola cada pocos segundos, así que una consulta por unidad no
 * es una consulta de más: es una por unidad multiplicada por cada refresco, para siempre.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OperacionService {

	private final AsignacionRepository asignaciones;
	private final TurnoRepository turnos;
	private final AtencionRepository atenciones;
	private final IncidenteRepository incidentes;
	private final AlertaRepository alertas;
	private final TrasladoService traslados;
	private final OperacionProperties config;

	public OperacionResponse estadoActual() {
		List<Ambulancia> unidades = unidadesEnServicio();
		List<Long> unidadIds = unidades.stream().map(Ambulancia::getId).toList();

		Map<Long, List<Turno>> tripulaciones = tripulacionesPorUnidad(unidadIds);
		Map<Long, Atencion> enCurso = atencionesPorUnidad(unidadIds);
		List<Incidente> sinCubrir = incidentes.buscarSinCubrir();

		// Los incidentes de las dos listas se nombran igual, así que sus descripciones se traen de una sola vez.
		Map<Long, String> referencias = referenciasDe(sinCubrir, enCurso.values());

		return new OperacionResponse(
				unidades.stream()
						.map(unidad -> UnidadEnOperacionResponse.de(unidad,
								tripulaciones.getOrDefault(unidad.getId(), List.of()),
								atencionEnCurso(enCurso.get(unidad.getId()), referencias)))
						.toList(),
				sinCubrir.stream()
						.map(incidente -> IncidenteSinCubrirResponse.de(incidente,
								referencias.get(incidente.getId())))
						.toList(),
				traslados.problemas().stream()
						.map(fila -> TrasladoDelPanelResponse.de(fila.traslado(), fila.atencion()))
						.toList(),
				bitacora(),
				config.sinSenalSeg());
	}

	/**
	 * Las unidades que el centro de control supervisa: las que tienen a alguien asignado. Una ambulancia sin
	 * paramédico asignado todavía no es una unidad operable, así que no ocupa lugar en la pantalla.
	 */
	private List<Ambulancia> unidadesEnServicio() {
		return asignaciones.buscarVigentes().stream()
				.map(Asignacion::getAmbulancia)
				// Varios paramédicos comparten unidad: de las asignaciones vigentes salen ambulancias repetidas.
				.collect(Collectors.toMap(Ambulancia::getId, Function.identity(), (uno, otro) -> uno,
						LinkedHashMap::new))
				.values().stream()
				.sorted(Comparator.comparing(Ambulancia::getPlaca))
				.toList();
	}

	private Map<Long, List<Turno>> tripulacionesPorUnidad(List<Long> unidadIds) {
		if (unidadIds.isEmpty()) {
			return Map.of();
		}
		return turnos.buscarAbiertosPorAmbulancias(unidadIds).stream()
				.collect(Collectors.groupingBy(turno -> turno.getAmbulancia().getId()));
	}

	/** ME-1 garantiza a lo sumo una atención activa por unidad; ante un empate imposible manda la más reciente. */
	private Map<Long, Atencion> atencionesPorUnidad(List<Long> unidadIds) {
		if (unidadIds.isEmpty()) {
			return Map.of();
		}
		return atenciones.buscarActivasPorAmbulancias(unidadIds).stream()
				.collect(Collectors.toMap(atencion -> atencion.getAmbulancia().getId(), Function.identity(),
						(uno, otro) -> uno));
	}

	private AtencionEnCursoResponse atencionEnCurso(Atencion atencion, Map<Long, String> referencias) {
		if (atencion == null) {
			return null;
		}
		Long incidenteId = atencion.getIncidente() == null ? null : atencion.getIncidente().getId();
		return AtencionEnCursoResponse.de(atencion, incidenteId == null ? null : referencias.get(incidenteId));
	}

	/** La primera descripción de cada incidente: la escribió quien vio el hecho antes que nadie. */
	private Map<Long, String> referenciasDe(List<Incidente> sinCubrir, Iterable<Atencion> enCurso) {
		Set<Long> ids = new LinkedHashSet<>();
		sinCubrir.forEach(incidente -> ids.add(incidente.getId()));
		for (Atencion atencion : enCurso) {
			if (atencion.getIncidente() != null) {
				ids.add(atencion.getIncidente().getId());
			}
		}
		if (ids.isEmpty()) {
			return Map.of();
		}
		return alertas.buscarDescripcionesPorIncidentes(ids).stream()
				.collect(Collectors.toMap(ReferenciaDeIncidente::incidenteId, ReferenciaDeIncidente::descripcion,
						(primera, siguiente) -> primera));
	}

	/**
	 * La bitácora sale de aplanar en memoria las columnas de hito de las atenciones con movimiento reciente. Es a
	 * propósito: a esta escala son unas pocas decenas de filas, y un UNION en SQL nativo costaría más de mantener
	 * de lo que ahorraría.
	 */
	private List<EventoDeOperacionResponse> bitacora() {
		Instant desde = Instant.now().minus(Duration.ofHours(config.ventanaEventosHoras()));
		return atenciones.buscarConHitosDesde(desde).stream()
				.flatMap(atencion -> EventoDeOperacionResponse.bitacoraDe(atencion, desde).stream())
				.sorted(Comparator.comparing(EventoDeOperacionResponse::hora).reversed())
				.limit(config.maximoEventos())
				.toList();
	}

}
