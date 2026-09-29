package com.uem.ambulancias.usuarios.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Alta de una persona a cargo de un ciudadano. El teléfono puede ser el suyo propio: la señora que viaja no
 * necesita tener celular.
 */
public record RegistrarPersonaRequest(

		@NotBlank(message = "El nombre es obligatorio.")
		@Size(max = 255, message = "El nombre es demasiado largo.")
		String nombreCompleto,

		@NotBlank(message = "El teléfono es obligatorio.")
		@Size(max = 30, message = "El teléfono es demasiado largo.")
		String telefono) {
}
