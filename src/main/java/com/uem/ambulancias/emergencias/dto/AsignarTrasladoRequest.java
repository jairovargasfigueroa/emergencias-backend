package com.uem.ambulancias.emergencias.dto;

import jakarta.validation.constraints.NotNull;

/** Asignación a mano desde el panel, cuando el sistema no encontró unidad. */
public record AsignarTrasladoRequest(

		@NotNull(message = "Hay que elegir una ambulancia.")
		Long ambulanciaId) {
}
