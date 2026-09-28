package com.uem.ambulancias.emergencias.dto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.uem.ambulancias.emergencias.domain.Atencion;
import com.uem.ambulancias.emergencias.domain.EstadoAtencion;
import com.uem.ambulancias.emergencias.domain.OrigenAtencion;
import com.uem.ambulancias.emergencias.domain.TipoEventoAtencion;

/**
 * Lo que una unidad está haciendo ahora mismo, como lo mira el centro de control: en qué punto va, desde cuándo, y
 * el rastro de hitos que ya dejó.
 *
 * <p>{@code desde} no es la hora en que empezó la atención sino la del hito que la dejó en el estado actual: lo que
 * el despacho necesita saber es cuánto lleva así, no cuánto lleva en total.
 */
public record AtencionEnCursoResponse(

		Long id,
		EstadoAtencion estado,
		Instant desde,
		OrigenAtencion origen,

		/** Exactamente uno de los dos tiene valor, según el origen. */
		Long incidenteId,
		Long trasladoId,

		/** Con qué nombrarla en pantalla: el pasajero del traslado, o lo que contó quien avisó del incidente. */
		String etiqueta,

		List<Hito> hitos) {

	/** Un hito que ya ocurrió. Los que todavía no, simplemente no están en la lista. */
	public record Hito(TipoEventoAtencion clave, Instant hora) {
	}

	/**
	 * {@code atencion} con su ambulancia traída. {@code referenciaIncidente} es la descripción del incidente y solo
	 * se usa cuando la atención viene de uno; en un traslado el nombre del pasajero ya viaja en la propia atención.
	 */
	public static AtencionEnCursoResponse de(Atencion atencion, String referenciaIncidente) {
		OrigenAtencion origen = OrigenAtencion.de(atencion);
		return new AtencionEnCursoResponse(
				atencion.getId(),
				atencion.getEstado(),
				TipoEventoAtencion.delEstado(atencion.getEstado()).horaEn(atencion),
				origen,
				atencion.getIncidente() == null ? null : atencion.getIncidente().getId(),
				atencion.getTraslado() == null ? null : atencion.getTraslado().getId(),
				origen == OrigenAtencion.TRASLADO ? atencion.getNombrePaciente() : referenciaIncidente,
				hitosDe(atencion));
	}

	/**
	 * Los hitos ocurridos, del más viejo al más nuevo. Se ordena por hora y no por el orden de ME-1 porque el
	 * aviso de paciente no listo no tiene lugar fijo en la escalera: cae donde la unidad lo haya marcado.
	 */
	public static List<Hito> hitosDe(Atencion atencion) {
		List<Hito> ocurridos = new ArrayList<>();
		for (TipoEventoAtencion tipo : TipoEventoAtencion.values()) {
			Instant hora = tipo.horaEn(atencion);
			if (hora != null) {
				ocurridos.add(new Hito(tipo, hora));
			}
		}
		ocurridos.sort(Comparator.comparing(Hito::hora));
		return List.copyOf(ocurridos);
	}

}
