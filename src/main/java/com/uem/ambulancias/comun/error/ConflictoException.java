package com.uem.ambulancias.comun.error;

import lombok.Getter;

/**
 * Operación rechazada por el estado actual de los datos. Se responde con 409 y su código.
 */
@Getter
public class ConflictoException extends RuntimeException {

	private final CodigoError codigo;

	public ConflictoException(CodigoError codigo, String mensaje) {
		super(mensaje);
		this.codigo = codigo;
	}

}
