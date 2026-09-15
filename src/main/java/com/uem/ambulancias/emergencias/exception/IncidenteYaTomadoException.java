package com.uem.ambulancias.emergencias.exception;

import com.uem.ambulancias.comun.error.CodigoError;
import com.uem.ambulancias.comun.error.ConflictoException;
import com.uem.ambulancias.emergencias.domain.Incidente;

import lombok.Getter;

/**
 * SEC-B.1: otra unidad ya acude al incidente. Lleva el incidente para armar el contexto del 409, con el que la app
 * ofrece sumarse.
 */
@Getter
public class IncidenteYaTomadoException extends ConflictoException {

	private final Incidente incidente;

	public IncidenteYaTomadoException(Incidente incidente) {
		super(CodigoError.INCIDENTE_YA_TOMADO, "Otra unidad ya acude al incidente " + incidente.getId() + ".");
		this.incidente = incidente;
	}

}
