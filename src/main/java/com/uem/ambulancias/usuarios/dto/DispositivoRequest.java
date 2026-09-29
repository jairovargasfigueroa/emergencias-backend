package com.uem.ambulancias.usuarios.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** El teléfono al que se le mandan los avisos del ciudadano. */
public record DispositivoRequest(

		@NotBlank(message = "El token push es obligatorio.")
		@Size(max = 512, message = "El token push es demasiado largo.")
		String tokenPush) {
}
