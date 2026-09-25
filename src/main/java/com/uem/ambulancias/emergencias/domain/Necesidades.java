package com.uem.ambulancias.emergencias.domain;

/**
 * Lo que este viaje necesita. No es triaje: no pregunta qué tan grave está el paciente, sino qué hace falta para
 * moverlo. De acá sale el tipo de unidad, y por eso son datos del traslado y no de la persona.
 */
public record Necesidades(

		Movilidad movilidad,

		boolean oxigeno,

		/** Vía, sonda o monitoreo durante el viaje. */
		boolean equipo,

		/** La tripulación tiene que saberlo antes de llegar, y la unidad se limpia distinto después. */
		boolean aislamiento,

		/** En kilos. Decide si hace falta camilla reforzada y cuánta gente para cargar. */
		Integer pesoAproximado,

		/** Cuántos van con el paciente: si no hay lugar, hay que saberlo antes de salir. */
		int acompanantes,

		String observaciones) {

	public Necesidades {
		if (acompanantes < 0) {
			throw new IllegalArgumentException("Los acompañantes no pueden ser negativos.");
		}
		if (pesoAproximado != null && pesoAproximado <= 0) {
			throw new IllegalArgumentException("El peso aproximado tiene que ser mayor que cero.");
		}
	}

}
