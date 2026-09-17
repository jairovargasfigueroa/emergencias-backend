package com.uem.ambulancias.flota.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistrarParamedicoRequest(

		@NotBlank(message = "El nombre completo es obligatorio.")
		@Size(max = 255, message = "El nombre completo es demasiado largo.")
		String nombreCompleto,

		@NotBlank(message = "El teléfono es obligatorio.")
		@Size(max = 255, message = "El teléfono es demasiado largo.")
		String telefono) {
}
