package com.uem.ambulancias.emergencias.dto;

import java.time.Instant;

import com.uem.ambulancias.emergencias.domain.Alerta;
import com.uem.ambulancias.emergencias.domain.EstadoAlerta;
import com.uem.ambulancias.emergencias.domain.OrigenUbicacion;
import com.uem.ambulancias.usuarios.domain.Usuario;

import org.locationtech.jts.geom.Point;

/**
 * Alerta en el detalle de un incidente. {@code latitud} y {@code longitud} son las de la ubicación efectiva: la
 * ajustada si existe; si no, la original.
 */
public record AlertaDeIncidenteResponse(
		Long id,
		Instant fechaHora,
		EstadoAlerta estado,
		OrigenUbicacion origenUbicacion,
		double latitud,
		double longitud,
		Integer cantidadAfectados,
		String descripcion,
		Emisor emisor) {

	/** Ciudadano que emitió la alerta. */
	public record Emisor(Long id, String nombreCompleto, String telefono) {
	}

	/** {@code alerta} con su emisor. */
	public static AlertaDeIncidenteResponse de(Alerta alerta) {
		Point ubicacion = alerta.getUbicacionEfectiva();
		Usuario emisor = alerta.getEmisor();
		return new AlertaDeIncidenteResponse(
				alerta.getId(),
				alerta.getFechaHora(),
				alerta.getEstado(),
				alerta.getOrigenUbicacion(),
				ubicacion.getY(),
				ubicacion.getX(),
				alerta.getCantidadAfectados(),
				alerta.getDescripcion(),
				new Emisor(emisor.getId(), emisor.getNombreCompleto(), emisor.getTelefono()));
	}

}
