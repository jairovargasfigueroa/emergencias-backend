package com.uem.ambulancias.flota.dto;

import jakarta.validation.constraints.NotNull;

/**
 * {@code confirmarReasignacion} es opcional: si falta, se toma como {@code false}.
 */
public record AsignarParamedicoRequest(

		@NotNull(message = "El paramédico es obligatorio.")
		Long paramedicoId,

		@NotNull(message = "La ambulancia es obligatoria.")
		Long ambulanciaId,

		Boolean confirmarReasignacion) {

	public boolean reasignacionConfirmada() {
		return Boolean.TRUE.equals(confirmarReasignacion);
	}

}
