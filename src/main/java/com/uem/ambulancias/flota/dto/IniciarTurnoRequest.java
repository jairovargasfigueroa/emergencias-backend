package com.uem.ambulancias.flota.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Para entrar de turno se vuelve a pedir el PIN, desde el teléfono vinculado. */
public record IniciarTurnoRequest(

		@NotNull(message = "El PIN es obligatorio.")
		@Pattern(regexp = "\\d{6}", message = "El PIN tiene que tener 6 dígitos.")
		String pin,

		@NotBlank(message = "La clave del dispositivo es obligatoria.")
		@Size(max = 64, message = "La clave del dispositivo no es válida.")
		String claveDispositivo) {
}
