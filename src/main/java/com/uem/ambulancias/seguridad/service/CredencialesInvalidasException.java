package com.uem.ambulancias.seguridad.service;

/**
 * Las credenciales no sirven o el token falta. Nunca se dice cuál de las dos cosas falló: a quien prueba claves no
 * se le regala la pista de que el correo existe.
 */
public class CredencialesInvalidasException extends RuntimeException {

	public CredencialesInvalidasException() {
		super("El correo o la clave no son correctos.");
	}

}
