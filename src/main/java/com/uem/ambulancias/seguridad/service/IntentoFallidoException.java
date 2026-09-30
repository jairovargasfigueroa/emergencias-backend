package com.uem.ambulancias.seguridad.service;

import com.uem.ambulancias.comun.error.CodigoError;
import com.uem.ambulancias.comun.error.ConflictoException;

import lombok.Getter;

/**
 * Un código de activación o un PIN que no sirvió. Va con 409 y no con 401: las apps toman todo 401 como una sesión
 * vencida y la cierran, y a quien se equivoca de PIN al iniciar su turno no hay que sacarlo de la app. Lleva cuántos
 * intentos le quedan, para que la app se lo diga antes de que haga falta un código nuevo de la central.
 */
@Getter
public class IntentoFallidoException extends ConflictoException {

	private final int intentosRestantes;

	public IntentoFallidoException(CodigoError codigo, String mensaje, int intentosRestantes) {
		super(codigo, mensaje);
		this.intentosRestantes = intentosRestantes;
	}

}
