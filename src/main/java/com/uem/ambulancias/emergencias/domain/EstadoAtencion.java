package com.uem.ambulancias.emergencias.domain;

import java.util.Set;

/**
 * ME-1, lado de la atención. La fase del paciente termina en {@code PACIENTE_ENTREGADO} o en {@code SIN_TRASLADO};
 * la unidad, en cambio, sigue ocupada hasta que se libera, que es un hito aparte y no un estado.
 */
public enum EstadoAtencion {

	EN_CAMINO,
	EN_EL_LUGAR,
	PACIENTE_RECOGIDO,
	EN_HOSPITAL,
	PACIENTE_ENTREGADO,
	SIN_TRASLADO,
	CANCELADA;

	/** Atención activa: la unidad sigue trabajando en el incidente. */
	public static final Set<EstadoAtencion> ACTIVOS =
			Set.of(EN_CAMINO, EN_EL_LUGAR, PACIENTE_RECOGIDO, EN_HOSPITAL);

	/** La unidad ya llegó al lugar: lo que reporte el ciudadano desde la app ya no cambia lo que encuentra allí. */
	public static final Set<EstadoAtencion> EN_ESCENA = Set.of(EN_EL_LUGAR, PACIENTE_RECOGIDO);

	/** La unidad fue y resolvió: el incidente ya no espera a nadie, aunque no haya trasladado a nadie. */
	public static final Set<EstadoAtencion> RESUELTOS = Set.of(PACIENTE_ENTREGADO, SIN_TRASLADO);

	public boolean isActiva() {
		return ACTIVOS.contains(this);
	}

	/** Terminó la fase del paciente, pero la unidad no queda libre hasta que se libera. */
	public boolean isResuelta() {
		return RESUELTOS.contains(this);
	}

	/** Siguiente hito de ME-1 ({@code null} en los estados finales). */
	public EstadoAtencion siguienteHito() {
		return switch (this) {
			case EN_CAMINO -> EN_EL_LUGAR;
			case EN_EL_LUGAR -> PACIENTE_RECOGIDO;
			case PACIENTE_RECOGIDO -> EN_HOSPITAL;
			case EN_HOSPITAL -> PACIENTE_ENTREGADO;
			case PACIENTE_ENTREGADO, SIN_TRASLADO, CANCELADA -> null;
		};
	}

}
