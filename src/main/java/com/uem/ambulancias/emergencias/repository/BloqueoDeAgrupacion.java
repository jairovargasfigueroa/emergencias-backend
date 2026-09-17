package com.uem.ambulancias.emergencias.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Serializa la agrupación de alertas con un advisory lock de PostgreSQL, para que dos alertas simultáneas del
 * mismo suceso no creen dos incidentes (PB-02 R8).
 */
@Component
@RequiredArgsConstructor
public class BloqueoDeAgrupacion {

	/** Clave fija del bloqueo; solo la usa la agrupación de alertas. */
	private static final long CLAVE = 2_026_091_501L;

	private final JdbcTemplate jdbc;

	/** Espera el bloqueo exclusivo. PostgreSQL lo libera solo al terminar la transacción actual. */
	public void adquirir() {
		jdbc.execute("select pg_advisory_xact_lock(" + CLAVE + ")");
	}

}
