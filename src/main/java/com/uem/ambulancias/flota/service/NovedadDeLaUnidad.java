package com.uem.ambulancias.flota.service;

/**
 * Algo que la central le hizo a la unidad sin que la tripulación lo pidiera. Además del tiempo real se le avisa por
 * push: con la app cerrada, es la única forma de que se entere.
 */
public record NovedadDeLaUnidad(Long ambulanciaId, Tipo tipo) {

	public enum Tipo {

		/** Canceló su atención antes de que tuviera al paciente a bordo. */
		ATENCION_CANCELADA_POR_CENTRAL,
		/** Dio por entregado al paciente que llevaba, y con eso la liberó. */
		ATENCION_ENTREGADA_POR_CENTRAL,
		/** La liberó cuando ya había resuelto su atención. */
		UNIDAD_LIBERADA_POR_CENTRAL,
		/** La marcó fuera de servicio. */
		FUERA_DE_SERVICIO,
		/** La volvió a poner en servicio. */
		REACTIVADA

	}

}
