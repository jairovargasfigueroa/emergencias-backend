package com.uem.ambulancias.evidencias.domain;

/**
 * PENDIENTE espera su turno, EN_CURSO lo tiene un worker. HECHO y FALLIDO son finales. EN_REVISION también para de
 * reintentar, pero no es culpa de la evidencia: es la configuración del servicio y la tiene que mirar una persona.
 */
public enum EstadoTrabajoIa {

	PENDIENTE,
	EN_CURSO,
	HECHO,
	FALLIDO,
	EN_REVISION

}
