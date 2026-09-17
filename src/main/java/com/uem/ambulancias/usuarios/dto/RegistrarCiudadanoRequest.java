package com.uem.ambulancias.usuarios.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistrarCiudadanoRequest(

		@NotBlank(message = "El nombre es obligatorio.")
		@Size(max = 255, message = "El nombre es demasiado largo.")
		String nombreCompleto,

		@NotBlank(message = "El teléfono es obligatorio.")
		@Size(max = 255, message = "El teléfono es demasiado largo.")
		String telefono) {
}
