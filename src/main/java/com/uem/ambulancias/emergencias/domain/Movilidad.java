package com.uem.ambulancias.emergencias.domain;

/**
 * Cómo se moviliza el paciente en este viaje. Es dato del traslado y no de la persona: la misma señora puede ir
 * en silla un lunes y en camilla el siguiente. Junto con el equipo que necesita, decide el tipo de unidad.
 */
public enum Movilidad {

	/** Se mueve por sus medios con apoyo. */
	CAMINA_CON_AYUDA,
	/** No camina, pero se mantiene sentado. */
	SILLA_DE_RUEDAS,
	/** Las tres cosas a la vez: no se levanta sin ayuda, no camina y no puede sentarse. */
	CAMILLA

}
