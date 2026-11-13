package com.uem.ambulancias.emergencias.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Desactivar o reactivar una cuenta siempre lleva su motivo: queda en la historia de la cuenta. */
public record DecisionSobreCuentaRequest(

		@NotBlank(message = "Escribe el motivo: queda registrado en la cuenta.")
		@Size(max = 1000, message = "El motivo es demasiado largo.")
		String motivo) {
}
