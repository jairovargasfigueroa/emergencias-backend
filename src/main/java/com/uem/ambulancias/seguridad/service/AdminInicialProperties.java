package com.uem.ambulancias.seguridad.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Primer administrador ({@code sga.admin}). Sin él no habría por dónde entrar al panel, porque el sistema no tiene
 * registro público de administradores: los demás los crea este primero.
 */
@ConfigurationProperties(prefix = "sga.admin")
public record AdminInicialProperties(
		String correo,
		String clave,
		@DefaultValue("Administrador") String nombre,
		@DefaultValue("-") String telefono) {

	/** Sin correo o sin clave no se crea nada: el arranque no falla, pero queda avisado en el log. */
	public boolean configurado() {
		return correo != null && !correo.isBlank() && clave != null && !clave.isBlank();
	}

}
