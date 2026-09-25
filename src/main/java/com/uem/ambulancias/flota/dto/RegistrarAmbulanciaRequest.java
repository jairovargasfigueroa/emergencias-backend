package com.uem.ambulancias.flota.dto;

import com.uem.ambulancias.flota.domain.TipoUnidad;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegistrarAmbulanciaRequest(

		@NotBlank(message = "La placa es obligatoria.")
		@Size(max = 255, message = "La placa es demasiado larga.")
		String placa,

		@NotNull(message = "El tipo de unidad es obligatorio.")
		TipoUnidad tipoUnidad) {
}
