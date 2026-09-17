package com.uem.ambulancias.integracion.firebase;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Conexión a Firebase Realtime Database ({@code sga.firebase.*}). Mientras esté apagado, nada se publica.
 */
@ConfigurationProperties(prefix = "sga.firebase")
public record FirebaseProperties(

		@DefaultValue("false") boolean habilitado,

		/** Ruta al JSON de la cuenta de servicio. Debe quedar fuera del repositorio. */
		String credenciales,

		/** URL de la base, por ejemplo https://mi-proyecto-default-rtdb.firebaseio.com */
		String urlBaseDatos) {
}
