package com.uem.ambulancias.emergencias.dto;

import org.locationtech.jts.geom.Point;

/**
 * Punto geográfico en latitud y longitud.
 */
public record UbicacionResponse(double latitud, double longitud) {

	/** {@code null} si no hay punto. */
	public static UbicacionResponse de(Point punto) {
		return punto == null ? null : new UbicacionResponse(punto.getY(), punto.getX());
	}

}
