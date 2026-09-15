package com.uem.ambulancias.emergencias.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Recogida del paciente. Nombre y documento son opcionales.
 */
public record RecogidaRequest(

		@NotNull(message = "La latitud es obligatoria.")
		@DecimalMin(value = "-90", message = "La latitud debe estar entre -90 y 90.")
		@DecimalMax(value = "90", message = "La latitud debe estar entre -90 y 90.")
		Double latitud,

		@NotNull(message = "La longitud es obligatoria.")
		@DecimalMin(value = "-180", message = "La longitud debe estar entre -180 y 180.")
		@DecimalMax(value = "180", message = "La longitud debe estar entre -180 y 180.")
		Double longitud,

		@Size(max = 255, message = "El nombre del paciente es demasiado largo.")
		String nombrePaciente,

		@Size(max = 255, message = "El documento del paciente es demasiado largo.")
		String documentoPaciente) {
}
