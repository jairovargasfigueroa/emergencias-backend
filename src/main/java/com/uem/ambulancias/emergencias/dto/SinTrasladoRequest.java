package com.uem.ambulancias.emergencias.dto;

import com.uem.ambulancias.emergencias.domain.MotivoSinTraslado;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/**
 * Cierre de una salida que no trasladó a nadie: dónde se cerró y por qué. El motivo decide con qué estado queda el
 * incidente, así que es obligatorio.
 */
public record SinTrasladoRequest(

		@NotNull(message = "La latitud es obligatoria.")
		@DecimalMin(value = "-90", message = "La latitud debe estar entre -90 y 90.")
		@DecimalMax(value = "90", message = "La latitud debe estar entre -90 y 90.")
		Double latitud,

		@NotNull(message = "La longitud es obligatoria.")
		@DecimalMin(value = "-180", message = "La longitud debe estar entre -180 y 180.")
		@DecimalMax(value = "180", message = "La longitud debe estar entre -180 y 180.")
		Double longitud,

		@NotNull(message = "El motivo es obligatorio.")
		MotivoSinTraslado motivo) {
}
