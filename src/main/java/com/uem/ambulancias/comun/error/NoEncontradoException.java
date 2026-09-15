package com.uem.ambulancias.comun.error;

/**
 * El recurso pedido no existe. Se responde con 404.
 */
public class NoEncontradoException extends RuntimeException {

	public NoEncontradoException(String mensaje) {
		super(mensaje);
	}

}
