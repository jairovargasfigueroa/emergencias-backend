package com.uem.ambulancias.emergencias.dto;

import com.uem.ambulancias.emergencias.domain.MotivoCancelacionAtencion;

import jakarta.validation.constraints.NotNull;

public record CancelarAtencionRequest(

		@NotNull(message = "El motivo de cancelación es obligatorio.")
		MotivoCancelacionAtencion motivo) {
}
