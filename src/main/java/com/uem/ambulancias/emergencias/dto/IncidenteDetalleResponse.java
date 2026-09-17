package com.uem.ambulancias.emergencias.dto;

import java.time.Instant;
import java.util.List;

import com.uem.ambulancias.emergencias.domain.Alerta;
import com.uem.ambulancias.emergencias.domain.Atencion;
import com.uem.ambulancias.emergencias.domain.EstadoIncidente;
import com.uem.ambulancias.emergencias.domain.Incidente;
import com.uem.ambulancias.emergencias.domain.MotivoCierreIncidente;

/**
 * Detalle de un incidente en la consulta del panel: los mismos campos del resumen, sus alertas en orden de emisión y
 * sus atenciones en orden de toma.
 */
public record IncidenteDetalleResponse(
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
		long unidadesAcudiendo,
		List<AlertaDeIncidenteResponse> alertas,
		List<AtencionDeIncidenteResponse> atenciones) {

	/** {@code alertas} con su emisor y {@code atenciones} con su ambulancia y su centro de salud, ya ordenadas. */
	public static IncidenteDetalleResponse de(Incidente incidente, List<Alerta> alertas, List<Atencion> atenciones) {
		IncidenteResumenResponse resumen = IncidenteResumenResponse.de(incidente, alertas.size(), atenciones);
		return new IncidenteDetalleResponse(
				resumen.id(),
				resumen.estado(),
				resumen.fechaHoraCreacion(),
				resumen.fechaHoraCierre(),
				resumen.motivoCierre(),
				resumen.latitud(),
				resumen.longitud(),
				resumen.cantidadAfectados(),
				resumen.cantidadAlertas(),
				resumen.unidades(),
				resumen.unidadesAcudiendo(),
				alertas.stream().map(AlertaDeIncidenteResponse::de).toList(),
				atenciones.stream().map(AtencionDeIncidenteResponse::de).toList());
	}

}
