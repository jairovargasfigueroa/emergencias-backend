package com.uem.ambulancias.emergencias.domain;

/**
 * Cómo cierra la central una atención que la tripulación no puede cerrar, según dónde quedó. Es lo que haría un
 * despachador después de hablar con la tripulación por otro medio.
 */
public enum CierreDesdeLaCentral {

	/** Todavía no tenía al paciente: se cancela y el caso sigue su camino, con otra unidad si hace falta. */
	CANCELAR,
	/** Llevaba al paciente a bordo: el viaje terminó en el destino. */
	DAR_POR_ENTREGADA,
	/** Ya había resuelto y solo le faltó quedar libre. */
	LIBERAR

}
