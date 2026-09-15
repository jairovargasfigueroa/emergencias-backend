package com.uem.ambulancias.flota.dto;

import jakarta.validation.constraints.NotBlank;

public record IdentificarParamedicoRequest(

		@NotBlank(message = "El teléfono es obligatorio.")
		String telefono) {
}
