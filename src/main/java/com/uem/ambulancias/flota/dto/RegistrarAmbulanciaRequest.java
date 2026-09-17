package com.uem.ambulancias.flota.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistrarAmbulanciaRequest(

		@NotBlank(message = "La placa es obligatoria.")
		@Size(max = 255, message = "La placa es demasiado larga.")
		String placa,

		@NotBlank(message = "El tipo de unidad es obligatorio.")
		@Size(max = 255, message = "El tipo de unidad es demasiado largo.")
		String tipoUnidad) {
}
