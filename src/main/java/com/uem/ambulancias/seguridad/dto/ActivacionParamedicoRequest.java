package com.uem.ambulancias.seguridad.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Activación de la app del paramédico en un teléfono: el código que le entregó la central y el PIN que crea. El
 * código se acepta con o sin guion.
 */
public record ActivacionParamedicoRequest(

		@NotBlank(message = "El teléfono es obligatorio.")
		String telefono,

		@NotBlank(message = "El código de activación es obligatorio.")
		@Size(max = 20, message = "El código de activación no es válido.")
		String codigo,

		@NotNull(message = "El PIN es obligatorio.")
		@Pattern(regexp = "\\d{6}", message = "El PIN tiene que tener 6 dígitos.")
		String pin) {
}
