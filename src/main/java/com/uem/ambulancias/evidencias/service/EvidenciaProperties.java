package com.uem.ambulancias.evidencias.service;

import java.time.Duration;

import com.uem.ambulancias.evidencias.domain.Modalidad;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Parámetros de las evidencias ({@code sga.evidencias.*}). Los límites de tamaño son los mismos que aplica el servicio
 * de análisis: si acá se aceptara más, el archivo se subiría para que después el análisis lo rechace.
 */
@ConfigurationProperties(prefix = "sga.evidencias")
public record EvidenciaProperties(

		/** Cuántas evidencias puede adjuntar un ciudadano a una misma alerta. */
		@DefaultValue("5") int maximoPorAlerta,

		/** Límites por modalidad, en MiB. */
		@DefaultValue("10") int mibImagen,
		@DefaultValue("20") int mibAudio,
		@DefaultValue("20") int mibVideo,

		/** Cuánto sirve la URL con la que la app sube el archivo. */
		@DefaultValue("15") int minutosSubida,

		/**
		 * Cuánto sirve una URL de lectura. Tiene que cubrir la peor llamada al análisis, que ronda los cuatro minutos,
		 * así que nunca menos de cinco.
		 */
		@DefaultValue("10") int minutosLectura) {

	public long limiteBytes(Modalidad modalidad) {
		int mib = switch (modalidad) {
			case IMAGEN -> mibImagen;
			case AUDIO -> mibAudio;
			case VIDEO -> mibVideo;
		};
		return mib * 1024L * 1024L;
	}

	public Duration vigenciaSubida() {
		return Duration.ofMinutes(minutosSubida);
	}

	public Duration vigenciaLectura() {
		return Duration.ofMinutes(Math.max(minutosLectura, 5));
	}

}
