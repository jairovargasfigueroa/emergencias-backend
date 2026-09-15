package com.uem.ambulancias.emergencias.domain;

import java.util.Set;

public enum EstadoAtencion {

	EN_CAMINO,
	EN_EL_LUGAR,
	PACIENTE_RECOGIDO,
	PACIENTE_ENTREGADO,
	CANCELADA;

	/** Atención activa: la unidad sigue trabajando en el incidente. */
	public static final Set<EstadoAtencion> ACTIVOS = Set.of(EN_CAMINO, EN_EL_LUGAR, PACIENTE_RECOGIDO);

	public boolean isActiva() {
		return ACTIVOS.contains(this);
	}

}
