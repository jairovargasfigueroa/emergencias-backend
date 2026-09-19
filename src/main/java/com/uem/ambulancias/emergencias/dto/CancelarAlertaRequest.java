package com.uem.ambulancias.emergencias.dto;

import com.uem.ambulancias.emergencias.domain.MotivoCancelacionAlerta;

import jakarta.validation.constraints.NotNull;

/**
 * El ciudadano retira el pedido que hizo. {@code emisorEsPaciente} responde «¿sos vos quien necesitaba la
 * ambulancia?»: un tercero que avisó por otro no habla por el estado de esa persona.
 */
public record CancelarAlertaRequest(

		@NotNull(message = "El motivo es obligatorio.")
		MotivoCancelacionAlerta motivo,

		Boolean emisorEsPaciente) {
}
