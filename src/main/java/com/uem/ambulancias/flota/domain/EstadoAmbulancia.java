package com.uem.ambulancias.flota.domain;

import java.util.Map;
import java.util.Set;

public enum EstadoAmbulancia {

	/** Sana, pero sin nadie de turno adentro: no es una falla, es que no hay tripulación ahora. */
	SIN_TURNO,
	DISPONIBLE,
	EN_ATENCION,
	FUERA_DE_SERVICIO;

	/**
	 * Transiciones de ME-1: M1 y M4 desde DISPONIBLE, M2 y M3 desde EN_ATENCION, M5 desde FUERA_DE_SERVICIO. SIN_TURNO
	 * no lo pone nadie a mano: entra al salir el último de turno y sale cuando alguien entra.
	 */
	private static final Map<EstadoAmbulancia, Set<EstadoAmbulancia>> TRANSICIONES = Map.of(
			SIN_TURNO, Set.of(DISPONIBLE, FUERA_DE_SERVICIO),
			DISPONIBLE, Set.of(EN_ATENCION, FUERA_DE_SERVICIO, SIN_TURNO),
			EN_ATENCION, Set.of(DISPONIBLE, FUERA_DE_SERVICIO),
			FUERA_DE_SERVICIO, Set.of(DISPONIBLE, SIN_TURNO));

	public boolean puedePasarA(EstadoAmbulancia nuevo) {
		return TRANSICIONES.get(this).contains(nuevo);
	}

}
