package com.uem.ambulancias.comun.error;

import lombok.Getter;

/**
 * El usuario tiene el rol que pide el endpoint, pero no puede ver este recurso en particular. Se responde con 403 y
 * su código.
 */
@Getter
public class AccesoDenegadoException extends RuntimeException {

	private final CodigoError codigo;

	public AccesoDenegadoException(CodigoError codigo, String mensaje) {
		super(mensaje);
		this.codigo = codigo;
	}

}
