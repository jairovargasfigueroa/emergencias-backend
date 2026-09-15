package com.uem.ambulancias.emergencias.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Entrega del paciente. La ubicación es obligatoria; el centro del catálogo y la descripción del destino son
 * opcionales, para que la entrega nunca se bloquee por el catálogo (PB-05 R4).
 */
public record EntregaRequest(

		@NotNull(message = "La latitud es obligatoria.")
		@DecimalMin(value = "-90", message = "La latitud debe estar entre -90 y 90.")
		@DecimalMax(value = "90", message = "La latitud debe estar entre -90 y 90.")
		Double latitud,

		@NotNull(message = "La longitud es obligatoria.")
		@DecimalMin(value = "-180", message = "La longitud debe estar entre -180 y 180.")
		@DecimalMax(value = "180", message = "La longitud debe estar entre -180 y 180.")
		Double longitud,

		Long centroSaludId,

		@Size(max = 2000, message = "La descripción del destino es demasiado larga.")
		String destinoDescripcion) {
}
