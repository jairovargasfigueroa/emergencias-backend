package com.uem.ambulancias.comun.geo;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

/**
 * Convenciones geográficas del modelo: puntos PostGIS en SRID 4326 (lon/lat).
 * Las distancias en metros se calculan sobre geography en las consultas.
 */
public final class Geo {

	public static final int SRID = 4326;

	/** Tipo de columna de todos los atributos Point de las entidades. */
	public static final String COLUMNA_PUNTO = "geometry(Point," + SRID + ")";

	private static final GeometryFactory FABRICA = new GeometryFactory(new PrecisionModel(), SRID);

	private Geo() {
	}

	/** Crea un punto con SRID 4326. En PostGIS la x es la longitud y la y es la latitud. */
	public static Point punto(double latitud, double longitud) {
		return FABRICA.createPoint(new Coordinate(longitud, latitud));
	}

}
