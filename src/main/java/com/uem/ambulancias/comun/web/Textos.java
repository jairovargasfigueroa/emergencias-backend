package com.uem.ambulancias.comun.web;

/**
 * Normalización de textos opcionales que llegan en las peticiones.
 */
public final class Textos {

	private Textos() {
	}

	/** Sin espacios sobrantes; un texto vacío se guarda como {@code null}. */
	public static String opcional(String texto) {
		return texto == null || texto.isBlank() ? null : texto.trim();
	}

}
