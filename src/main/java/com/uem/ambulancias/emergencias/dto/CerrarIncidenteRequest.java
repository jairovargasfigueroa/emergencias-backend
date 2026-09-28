package com.uem.ambulancias.emergencias.dto;

import com.uem.ambulancias.emergencias.domain.MotivoCierreIncidente;

import jakarta.validation.constraints.NotNull;

/** Por qué la central cierra un incidente que no se va a atender. */
public record CerrarIncidenteRequest(

		@NotNull(message = "Hay que decir por qué se cierra.")
		MotivoCierreIncidente motivo) {
}
