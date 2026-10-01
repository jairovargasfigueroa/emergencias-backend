package com.uem.ambulancias.seguridad.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Entrada del paramédico a su app. El teléfono dice quién es; el PIN y la clave del teléfono vinculado prueban que
 * es él.
 */
public record IngresoParamedicoRequest(

		@NotBlank(message = "El teléfono es obligatorio.")
		String telefono,

		@NotNull(message = "El PIN es obligatorio.")
		@Pattern(regexp = "\\d{6}", message = "El PIN tiene que tener 6 dígitos.")
		String pin,

		@NotBlank(message = "La clave del dispositivo es obligatoria.")
		@Size(max = 64, message = "La clave del dispositivo no es válida.")
		String claveDispositivo) {
}
