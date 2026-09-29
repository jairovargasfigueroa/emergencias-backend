package com.uem.ambulancias.flota.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Lo único que se corrige de un paramédico. El rol y la ambulancia se cambian por otros caminos. */
public record EditarParamedicoRequest(

		@NotBlank(message = "El nombre completo es obligatorio.")
		@Size(max = 255, message = "El nombre completo es demasiado largo.")
		String nombreCompleto,

		@NotBlank(message = "El teléfono es obligatorio.")
		@Size(max = 255, message = "El teléfono es demasiado largo.")
		String telefono) {
}
