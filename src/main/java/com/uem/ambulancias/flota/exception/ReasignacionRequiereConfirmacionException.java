package com.uem.ambulancias.flota.exception;

import com.uem.ambulancias.comun.error.CodigoError;
import com.uem.ambulancias.comun.error.ConflictoException;
import com.uem.ambulancias.flota.domain.Asignacion;

import lombok.Getter;

/**
 * El paramédico ya tiene una asignación vigente con otra ambulancia. Lleva esa asignación para
 * armar el contexto del 409.
 */
@Getter
public class ReasignacionRequiereConfirmacionException extends ConflictoException {

	private final Asignacion asignacionVigente;

	public ReasignacionRequiereConfirmacionException(Asignacion asignacionVigente) {
		super(CodigoError.REASIGNACION_REQUIERE_CONFIRMACION, "El paramédico ya está asignado a la ambulancia "
				+ asignacionVigente.getAmbulancia().getPlaca() + ". Confirma para reasignarlo.");
		this.asignacionVigente = asignacionVigente;
	}

}
