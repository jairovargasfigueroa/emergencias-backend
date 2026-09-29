package com.uem.ambulancias.emergencias.service;

/**
 * Algo que cambió en un incidente y que quien pidió la ambulancia tiene que saber aunque tenga la app cerrada. Son
 * pocos momentos a propósito: los que tranquilizan o piden algo, no cada movimiento de la unidad.
 */
public record NovedadDelIncidente(Long incidenteId, Tipo tipo) {

	public enum Tipo {

		/** Una unidad tomó el caso: la ambulancia va en camino. */
		UNIDAD_EN_CAMINO,
		/** La primera unidad llegó al lugar. */
		UNIDAD_LLEGO,
		/** La unidad que iba no puede llegar y se busca otra. */
		BUSCANDO_OTRA_UNIDAD,
		/** La central cerró el caso sin atenderlo. */
		CERRADO_POR_LA_CENTRAL

	}

}
