package com.uem.ambulancias.emergencias.dto;

import java.time.Instant;
import java.util.List;

import com.uem.ambulancias.emergencias.domain.Atencion;
import com.uem.ambulancias.emergencias.domain.EstadoIncidente;
import com.uem.ambulancias.emergencias.domain.Incidente;
import com.uem.ambulancias.emergencias.domain.MotivoCierreIncidente;

/**
 * Incidente en la consulta del panel. {@code unidades} son las placas distintas de todas sus atenciones, en orden de
 * toma; {@code unidadesAcudiendo}, cuántas de sus atenciones siguen activas.
 */
public record IncidenteResumenResponse(
		Long id,
		EstadoIncidente estado,
		Instant fechaHoraCreacion,
		Instant fechaHoraCierre,
		MotivoCierreIncidente motivoCierre,
		double latitud,
		double longitud,
		Integer cantidadAfectados,
		long cantidadAlertas,
		List<String> unidades,
		long unidadesAcudiendo) {

	/** {@code atenciones}: todas las del incidente con su ambulancia, en orden de toma. */
	public static IncidenteResumenResponse de(Incidente incidente, long cantidadAlertas, List<Atencion> atenciones) {
		return new IncidenteResumenResponse(
				incidente.getId(),
				incidente.getEstado(),
				incidente.getFechaHoraCreacion(),
				incidente.getFechaHoraCierre(),
				incidente.getMotivoCierre(),
				incidente.getUbicacion().getY(),
				incidente.getUbicacion().getX(),
				incidente.getCantidadAfectados(),
				cantidadAlertas,
				atenciones.stream().map(atencion -> atencion.getAmbulancia().getPlaca()).distinct().toList(),
				atenciones.stream().filter(atencion -> atencion.getEstado().isActiva()).count());
	}

}
