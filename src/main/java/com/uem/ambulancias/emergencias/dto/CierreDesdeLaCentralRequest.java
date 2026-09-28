package com.uem.ambulancias.emergencias.dto;

import com.uem.ambulancias.emergencias.domain.CierreDesdeLaCentral;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Cómo cierra el administrador una atención trabada. El destino solo se usa al darla por entregada, y sin él queda
 * el que ya tenía.
 */
public record CierreDesdeLaCentralRequest(

		@NotNull(message = "Hay que elegir cómo se cierra la atención.")
		CierreDesdeLaCentral cierre,

		/** Si la unidad queda disponible. Si no, queda fuera de servicio hasta que la tripulación la reactive. */
		boolean dejarDisponible,

		Long centroSaludId,

		@Size(max = 255, message = "El destino es demasiado largo.")
		String destinoDescripcion) {
}
