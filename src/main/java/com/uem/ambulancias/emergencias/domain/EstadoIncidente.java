package com.uem.ambulancias.emergencias.domain;

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

	public boolean isAbierto() {
		return ABIERTOS.contains(this);
	}

}
