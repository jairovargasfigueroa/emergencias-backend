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

	/** La unidad ya llegó al lugar: lo que reporte el ciudadano desde la app ya no cambia lo que encuentra allí. */
	public static final Set<EstadoAtencion> EN_ESCENA = Set.of(EN_EL_LUGAR, PACIENTE_RECOGIDO);

	public boolean isActiva() {
		return ACTIVOS.contains(this);
	}

	/** Siguiente hito de ME-1 ({@code null} en los estados finales). */
	public EstadoAtencion siguienteHito() {
		return switch (this) {
			case EN_CAMINO -> EN_EL_LUGAR;
			case EN_EL_LUGAR -> PACIENTE_RECOGIDO;
			case PACIENTE_RECOGIDO -> PACIENTE_ENTREGADO;
			case PACIENTE_ENTREGADO, CANCELADA -> null;
		};
	}

}
