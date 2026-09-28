package com.uem.ambulancias.emergencias.dto;

import jakarta.validation.constraints.NotNull;

/** La unidad que la central manda a una emergencia. */
public record DespacharUnidadRequest(

		@NotNull(message = "Hay que elegir una ambulancia.")
		Long ambulanciaId) {
}
