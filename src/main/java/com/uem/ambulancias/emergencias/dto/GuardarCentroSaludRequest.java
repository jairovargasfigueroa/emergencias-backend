package com.uem.ambulancias.emergencias.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Alta o corrección de un centro de salud: el mismo formulario sirve para las dos cosas. */
public record GuardarCentroSaludRequest(
		@NotBlank(message = "El nombre del centro es obligatorio.")
		@Size(max = 255, message = "El nombre es demasiado largo.")
		String nombre,
		@Size(max = 255, message = "La dirección es demasiado larga.")
		String direccion,
		@NotNull(message = "La latitud es obligatoria.")
		@DecimalMin(value = "-90", message = "La latitud debe estar entre -90 y 90.")
		@DecimalMax(value = "90", message = "La latitud debe estar entre -90 y 90.")
		Double latitud,
		@NotNull(message = "La longitud es obligatoria.")
		@DecimalMin(value = "-180", message = "La longitud debe estar entre -180 y 180.")
		@DecimalMax(value = "180", message = "La longitud debe estar entre -180 y 180.")
		Double longitud) {
}
