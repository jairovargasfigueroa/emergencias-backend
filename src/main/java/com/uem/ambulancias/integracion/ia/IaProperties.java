package com.uem.ambulancias.integracion.ia;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Conexión al servicio de análisis ({@code sga.ia.*}). Apagado, no corre ningún worker: las evidencias se suben y
 * quedan con su análisis en la cola hasta que se encienda.
 */
@ConfigurationProperties(prefix = "sga.ia")
public record IaProperties(

		@DefaultValue("false") boolean habilitado,

		/** URL base del servicio, sin la ruta {@code /v1}. */
		String url,

		/** El mismo {@code AI_SERVICE_TOKEN} que tiene configurado el servicio. Nunca se versiona. */
		String token,

		@DefaultValue("10") int segundosConexion,

		/**
		 * Cuánto se espera la respuesta. El peor caso del servicio ronda los 265 segundos con un video (ffmpeg más sus propios reintentos);
		 * cortar antes sería tirar un análisis que estaba por llegar.
		 */
		@DefaultValue("300") int segundosLectura) {

	public Duration conexion() {
		return Duration.ofSeconds(segundosConexion);
	}

	public Duration lectura() {
		return Duration.ofSeconds(segundosLectura);
	}

}
