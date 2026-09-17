package com.uem.ambulancias.comun.web;

/**
 * Cabeceras HTTP propias de la API.
 */
public final class Cabeceras {

	/**
	 * Identifica al usuario que hace la petición mientras no exista autenticación. Es provisional: la
	 * reemplazará el token.
	 */
	public static final String USUARIO_ID = "X-Usuario-Id";

	private Cabeceras() {
	}

}
