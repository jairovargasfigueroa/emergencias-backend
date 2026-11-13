package com.uem.ambulancias.emergencias.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.uem.ambulancias.emergencias.domain.MotivoCierreIncidente;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Por qué la central cierra un incidente que no se va a atender. Una falsa alarma lleva además cómo se verificó:
 * es lo que después sirve para evaluar el mal uso de una cuenta, y sin fundamento no se puede usar para eso.
 */
public record CerrarIncidenteRequest(

		@NotNull(message = "Hay que decir por qué se cierra.")
		MotivoCierreIncidente motivo,

		@Size(max = 1000, message = "El detalle es demasiado largo.")
		String detalle) {

	@JsonIgnore
	@AssertTrue(message = "Para cerrarlo como falsa alarma, escribe cómo se verificó.")
	public boolean isConFundamento() {
		return motivo != MotivoCierreIncidente.FALSA_ALARMA_VERIFICADA || (detalle != null && !detalle.isBlank());
	}
}
