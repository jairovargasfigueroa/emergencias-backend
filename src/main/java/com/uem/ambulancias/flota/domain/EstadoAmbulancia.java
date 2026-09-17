package com.uem.ambulancias.flota.domain;

import java.util.Map;
import java.util.Set;

public enum EstadoAmbulancia {

	DISPONIBLE,
	EN_ATENCION,
	FUERA_DE_SERVICIO;

	/** Transiciones de ME-1: M1 y M4 desde DISPONIBLE, M2 y M3 desde EN_ATENCION, M5 desde FUERA_DE_SERVICIO. */
	private static final Map<EstadoAmbulancia, Set<EstadoAmbulancia>> TRANSICIONES = Map.of(
			DISPONIBLE, Set.of(EN_ATENCION, FUERA_DE_SERVICIO),
			EN_ATENCION, Set.of(DISPONIBLE, FUERA_DE_SERVICIO),
			FUERA_DE_SERVICIO, Set.of(DISPONIBLE));

	public boolean puedePasarA(EstadoAmbulancia nuevo) {
		return TRANSICIONES.get(this).contains(nuevo);
	}

}
