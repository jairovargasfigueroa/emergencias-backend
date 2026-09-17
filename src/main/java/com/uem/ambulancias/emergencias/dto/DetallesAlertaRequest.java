package com.uem.ambulancias.emergencias.dto;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Datos opcionales que la app pregunta después de emitir, porque nunca bloquean la emisión (PB-02 R3). Un campo que
 * no llega deja el valor anterior sin tocar.
 */
public record DetallesAlertaRequest(

		@PositiveOrZero(message = "La cantidad de afectados no puede ser negativa.")
		Integer cantidadAfectados,

		@Size(max = 2000, message = "La descripción es demasiado larga.")
		String descripcion) {
}
