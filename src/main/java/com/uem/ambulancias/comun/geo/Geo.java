package com.uem.ambulancias.comun.geo;

/**
 * Convenciones geográficas del modelo: puntos PostGIS en SRID 4326 (lon/lat).
 * Las distancias en metros se calculan sobre geography en las consultas.
 */
public final class Geo {

	public static final int SRID = 4326;

	/** Tipo de columna de todos los atributos Point de las entidades. */
	public static final String COLUMNA_PUNTO = "geometry(Point," + SRID + ")";

	private Geo() {
	}

}
