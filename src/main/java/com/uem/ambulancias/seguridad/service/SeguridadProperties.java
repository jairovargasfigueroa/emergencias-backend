package com.uem.ambulancias.seguridad.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Parámetros de los tokens ({@code sga.seguridad}). El secreto firma y verifica: si cambia, todas las sesiones
 * abiertas dejan de valer.
 */
@ConfigurationProperties(prefix = "sga.seguridad")
public record SeguridadProperties(
		/** Secreto para firmar, de 32 caracteres o más. Nunca se versiona: viaja por variable de entorno. */
		String secreto,
		/** Cuánto dura la sesión del panel. Corta, porque se usa desde una computadora compartida. */
		@DefaultValue("12") int horasPanel,
		/** Cuánto dura la sesión de las apps. Larga: pedirle credenciales a alguien en una emergencia no es opción. */
		@DefaultValue("180") int diasApp) {
}
