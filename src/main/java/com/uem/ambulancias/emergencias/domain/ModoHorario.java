package com.uem.ambulancias.emergencias.domain;

/**
 * Cuándo se espera el traslado. Existe para que {@code horaCita == null} no sea ambiguo: hoy nulo significa
 * inmediato, y el día que entre la vuelta a llamado esa también irá sin hora sin ser para ahora.
 */
public enum ModoHorario {

	/** Para ahora: se busca unidad de inmediato y el límite es una ventana fija. */
	INMEDIATO,
	/** Para una fecha y una hora de cita conocidas. */
	PROGRAMADO

}
