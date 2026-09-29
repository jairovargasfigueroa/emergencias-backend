package com.uem.ambulancias.emergencias.domain;

import java.time.Instant;

/**
 * Los hitos de una atención vistos como lo que le pasó a la unidad. No es un estado más: cada hito ya está guardado
 * como una hora en {@link Atencion}, y esto solo dice qué columna es cada uno.
 *
 * <p>Existe para que el panel pueda leer la bitácora sin conocer el nombre de las columnas y sin que haya que
 * guardar los eventos por segunda vez en una tabla aparte.
 */
public enum TipoEventoAtencion {

	TOMA,
	LLEGADA,
	RECOGIDA,
	HOSPITAL,
	ENTREGA,
	SIN_TRASLADO,
	LIBERACION,
	CANCELACION,
	AVISO_NO_LISTO;

	/** Cuándo ocurrió este hito en esa atención, o {@code null} si todavía no ocurrió. */
	public Instant horaEn(Atencion atencion) {
		return switch (this) {
			case TOMA -> atencion.getHoraToma();
			case LLEGADA -> atencion.getHoraLlegada();
			case RECOGIDA -> atencion.getHoraRecogida();
			case HOSPITAL -> atencion.getHoraLlegadaHospital();
			case ENTREGA -> atencion.getHoraEntrega();
			case SIN_TRASLADO -> atencion.getHoraSinTraslado();
			case LIBERACION -> atencion.getHoraLiberacion();
			case CANCELACION -> atencion.getHoraCancelacion();
			case AVISO_NO_LISTO -> atencion.getHoraAvisoNoListo();
		};
	}

	/**
	 * El hito que dejó a la atención en el estado que tiene ahora. Es lo que contesta "¿desde cuándo está así?",
	 * que es la pregunta del despacho cuando una unidad lleva demasiado tiempo en el mismo estado.
	 */
	public static TipoEventoAtencion delEstado(EstadoAtencion estado) {
		return switch (estado) {
			case EN_CAMINO -> TOMA;
			case EN_EL_LUGAR -> LLEGADA;
			case PACIENTE_RECOGIDO -> RECOGIDA;
			case EN_HOSPITAL -> HOSPITAL;
			case PACIENTE_ENTREGADO -> ENTREGA;
			case SIN_TRASLADO -> SIN_TRASLADO;
			case CANCELADA -> CANCELACION;
		};
	}

}
