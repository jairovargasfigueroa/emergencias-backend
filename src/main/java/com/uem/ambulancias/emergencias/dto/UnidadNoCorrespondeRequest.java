package com.uem.ambulancias.emergencias.dto;

import com.uem.ambulancias.emergencias.domain.Movilidad;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/**
 * Lo que el paramédico encuentra cuando el paciente no está como decía la ficha. No elige un vehículo: corrige
 * lo que ve, y el sistema vuelve a derivar el tipo de unidad con la misma regla de siempre.
 */
public record UnidadNoCorrespondeRequest(

		@NotNull(message = "La latitud es obligatoria.")
		@DecimalMin(value = "-90", message = "La latitud debe estar entre -90 y 90.")
		@DecimalMax(value = "90", message = "La latitud debe estar entre -90 y 90.")
		Double latitud,

		@NotNull(message = "La longitud es obligatoria.")
		@DecimalMin(value = "-180", message = "La longitud debe estar entre -180 y 180.")
		@DecimalMax(value = "180", message = "La longitud debe estar entre -180 y 180.")
		Double longitud,

		@NotNull(message = "Hay que indicar cómo se moviliza el paciente.")
		Movilidad movilidad,

		boolean oxigeno,

		boolean equipo) {
}
