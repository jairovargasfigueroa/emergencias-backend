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

	private static final double RADIO_TIERRA_M = 6_371_000;

	/**
	 * Distancia en línea recta entre dos puntos, en metros. Las consultas usan geography de PostGIS; esto es para
	 * cuando hace falta la distancia en Java sin ir a la base, como al estimar cuánto va a tardar un traslado.
	 */
	public static double metrosEntre(Point uno, Point otro) {
		double latUno = Math.toRadians(uno.getY());
		double latOtro = Math.toRadians(otro.getY());
		double difLat = latOtro - latUno;
		double difLon = Math.toRadians(otro.getX() - uno.getX());

		double a = Math.pow(Math.sin(difLat / 2), 2)
				+ Math.cos(latUno) * Math.cos(latOtro) * Math.pow(Math.sin(difLon / 2), 2);
		return 2 * RADIO_TIERRA_M * Math.asin(Math.sqrt(a));
	}

}
