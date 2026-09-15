package com.uem.ambulancias.flota.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistrarDispositivoRequest(

		@NotBlank(message = "El token push es obligatorio.")
		@Size(max = 512, message = "El token push es demasiado largo.")
		String tokenPush) {
}
