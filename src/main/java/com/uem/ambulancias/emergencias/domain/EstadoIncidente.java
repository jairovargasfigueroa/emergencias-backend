package com.uem.ambulancias.emergencias.domain;

import java.util.Map;
import java.util.Set;

public enum EstadoIncidente {

	ACTIVO,
	EN_ATENCION,
	ATENDIDO,
	FALSA_ALARMA,
	ATENDIDO_EXTERNAMENTE,
	CANCELADO;

	/** Estados abiertos: el incidente se muestra y admite tomas. Los demás son finales. */
	public static final Set<EstadoIncidente> ABIERTOS = Set.of(ACTIVO, EN_ATENCION);

	/** Transiciones de ME-1 (I1 a I7). Los estados finales no tienen salida. */
	private static final Map<EstadoIncidente, Set<EstadoIncidente>> TRANSICIONES = Map.of(
			ACTIVO, Set.of(EN_ATENCION, FALSA_ALARMA, ATENDIDO_EXTERNAMENTE, CANCELADO),
			EN_ATENCION, Set.of(ACTIVO, ATENDIDO, FALSA_ALARMA, ATENDIDO_EXTERNAMENTE, CANCELADO),
			ATENDIDO, Set.of(),
			FALSA_ALARMA, Set.of(),
			ATENDIDO_EXTERNAMENTE, Set.of(),
			CANCELADO, Set.of());

	public boolean isAbierto() {
		return ABIERTOS.contains(this);
	}

	public boolean puedePasarA(EstadoIncidente nuevo) {
		return TRANSICIONES.get(this).contains(nuevo);
	}

}
