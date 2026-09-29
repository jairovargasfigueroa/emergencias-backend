package com.uem.ambulancias.emergencias.dto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.uem.ambulancias.emergencias.domain.Atencion;
import com.uem.ambulancias.emergencias.domain.TipoEventoAtencion;

/**
 * Una línea de la bitácora del centro de control: qué unidad hizo qué y cuándo. Sale de aplanar los hitos que ya
 * están guardados en cada atención, no de una tabla de eventos.
 *
 * <p>El texto que lee el operador lo arma el panel. Acá solo viaja el dato: el tipo de hito y, cuando el hito
 * guarda un porqué o un destino, ese valor crudo en {@code detalle}.
 */
public record EventoDeOperacionResponse(

		Instant hora,
		Long ambulanciaId,
		String placa,
		TipoEventoAtencion tipo,
		Long atencionId,

		/** Exactamente uno de los dos tiene valor, según de dónde salió la atención. */
		Long incidenteId,
		Long trasladoId,

		/** El motivo del cierre o de la cancelación, o el destino de la entrega. {@code null} en los demás. */
		String detalle) {

	/**
	 * Los hitos de esa atención que ocurrieron desde {@code desde}, uno por evento y sin ordenar: quien llama
	 * junta los de todas las atenciones y recién ahí los ordena.
	 *
	 * <p>{@code atencion} tiene que venir con su ambulancia y su centro de salud traídos.
	 */
	public static List<EventoDeOperacionResponse> bitacoraDe(Atencion atencion, Instant desde) {
		List<EventoDeOperacionResponse> eventos = new ArrayList<>();
		for (TipoEventoAtencion tipo : TipoEventoAtencion.values()) {
			Instant hora = tipo.horaEn(atencion);
			if (hora != null && !hora.isBefore(desde)) {
				eventos.add(de(atencion, tipo, hora));
			}
		}
		return eventos;
	}

	private static EventoDeOperacionResponse de(Atencion atencion, TipoEventoAtencion tipo, Instant hora) {
		return new EventoDeOperacionResponse(
				hora,
				atencion.getAmbulancia().getId(),
				atencion.getAmbulancia().getPlaca(),
				tipo,
				atencion.getId(),
				atencion.getIncidente() == null ? null : atencion.getIncidente().getId(),
				atencion.getTraslado() == null ? null : atencion.getTraslado().getId(),
				detalle(atencion, tipo));
	}

	private static String detalle(Atencion atencion, TipoEventoAtencion tipo) {
		return switch (tipo) {
			case ENTREGA -> atencion.getCentroSalud() != null ? atencion.getCentroSalud().getNombre()
					: atencion.getDestinoDescripcion();
			case SIN_TRASLADO -> nombre(atencion.getMotivoSinTraslado());
			case CANCELACION -> nombre(atencion.getMotivoCancelacion());
			case TOMA, LLEGADA, RECOGIDA, HOSPITAL, LIBERACION, AVISO_NO_LISTO -> null;
		};
	}

	private static String nombre(Enum<?> motivo) {
		return motivo == null ? null : motivo.name();
	}

}
