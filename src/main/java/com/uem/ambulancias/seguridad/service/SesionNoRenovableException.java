package com.uem.ambulancias.seguridad.service;

/**
 * La sesión de una app ya no se puede renovar: el ciudadano no verificó su número, o el teléfono del paramédico ya no
 * es el vinculado. Va con 401 a propósito: la app lo toma como una sesión vencida, la cierra y lo manda a entrar de
 * nuevo, que es justo lo que hace falta.
 */
public class SesionNoRenovableException extends RuntimeException {

	public SesionNoRenovableException(String mensaje) {
		super(mensaje);
	}

}
