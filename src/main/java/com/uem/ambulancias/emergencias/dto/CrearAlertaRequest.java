package com.uem.ambulancias.emergencias.dto;

import com.uem.ambulancias.emergencias.domain.OrigenUbicacion;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Ubicación (GPS o pin manual) y origen son obligatorios. {@code cantidadAfectados} y {@code descripcion} son
 * opcionales y nunca bloquean la emisión (PB-02 R3).
 */
public record CrearAlertaRequest(

		@NotNull(message = "La latitud es obligatoria.")
		@DecimalMin(value = "-90", message = "La latitud debe estar entre -90 y 90.")
		@DecimalMax(value = "90", message = "La latitud debe estar entre -90 y 90.")
		Double latitud,

		@NotNull(message = "La longitud es obligatoria.")
		@DecimalMin(value = "-180", message = "La longitud debe estar entre -180 y 180.")
		@DecimalMax(value = "180", message = "La longitud debe estar entre -180 y 180.")
		Double longitud,

		@NotNull(message = "El origen de la ubicación es obligatorio.")
		OrigenUbicacion origenUbicacion,

		@PositiveOrZero(message = "La cantidad de afectados no puede ser negativa.")
		Integer cantidadAfectados,

		@Size(max = 2000, message = "La descripción es demasiado larga.")
		String descripcion) {
}
