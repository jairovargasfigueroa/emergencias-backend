package com.uem.ambulancias.emergencias.dto;

import java.time.Instant;

import com.uem.ambulancias.emergencias.domain.EstadoIncidente;
import com.uem.ambulancias.emergencias.domain.Incidente;

/**
 * Un incidente al que todavía no va nadie. Es la lista que el administrador mira para decidir a quién mandar, así
 * que lleva lo justo para ubicarlo en el mapa y reconocerlo.
 */
public record IncidenteSinCubrirResponse(

		Long id,
		double latitud,
		double longitud,
		EstadoIncidente estado,

		/**
		 * Desde cuándo existe el incidente. Cuándo se quedó sin unidad no se guarda en ninguna parte, así que lo
		 * que se muestra es su creación: para el despacho es la misma pregunta, cuánto hace que nadie va.
		 */
		Instant desde,

		/** Lo que contó quien avisó, si dejó algo escrito. */
		String referencia) {

	public static IncidenteSinCubrirResponse de(Incidente incidente, String referencia) {
		return new IncidenteSinCubrirResponse(
				incidente.getId(),
				incidente.getUbicacion().getY(),
				incidente.getUbicacion().getX(),
				incidente.getEstado(),
				incidente.getFechaHoraCreacion(),
				referencia);
	}

}
