package com.uem.ambulancias.cobros.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnularCobroRequest(

		@NotBlank(message = "Escribe por qué se anula.")
		@Size(max = 1000, message = "El motivo es demasiado largo.")
		String motivo) {
}
