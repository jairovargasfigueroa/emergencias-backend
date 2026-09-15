package com.uem.ambulancias.flota.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record RegistrarPosicionRequest(

		@NotNull(message = "La latitud es obligatoria.")
		@DecimalMin(value = "-90", message = "La latitud debe estar entre -90 y 90.")
		@DecimalMax(value = "90", message = "La latitud debe estar entre -90 y 90.")
		Double latitud,

		@NotNull(message = "La longitud es obligatoria.")
		@DecimalMin(value = "-180", message = "La longitud debe estar entre -180 y 180.")
		@DecimalMax(value = "180", message = "La longitud debe estar entre -180 y 180.")
		Double longitud) {
}
