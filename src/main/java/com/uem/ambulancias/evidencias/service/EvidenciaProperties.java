package com.uem.ambulancias.evidencias.service;

import java.time.Duration;

import com.uem.ambulancias.evidencias.domain.Modalidad;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Parámetros de las evidencias ({@code sga.evidencias.*}). Los límites de tamaño nunca pasan los del servicio de
 * análisis: si acá se aceptara más, el archivo se subiría para que después el análisis lo rechace.
 */
@ConfigurationProperties(prefix = "sga.evidencias")
public record EvidenciaProperties(

		/** Cuántas evidencias puede adjuntar un ciudadano a una misma alerta. */
		@DefaultValue("5") int maximoPorAlerta,

		/**
		 * Cuántas evidencias puede juntar un incidente sumando todas sus alertas. Varios testigos del mismo hecho
		 * agregan poco después de cierto punto, y cada archivo es un análisis más que pagar y esperar.
		 */
		@DefaultValue("15") int maximoPorIncidente,

		/** Límites por modalidad, en MiB. Un audio de 5 MiB ya son varios minutos de grabación. */
		@DefaultValue("10") int mibImagen,
		@DefaultValue("5") int mibAudio,
		@DefaultValue("20") int mibVideo,

		/** Cuánto sirve la URL con la que la app sube el archivo. */
		@DefaultValue("15") int minutosSubida,

		/**
		 * Cuánto sirve una URL de lectura. Tiene que cubrir la peor llamada al análisis, que ronda los cuatro minutos,
		 * así que nunca menos de cinco.
		 */
		@DefaultValue("10") int minutosLectura,

		/** Cada cuántos minutos corre la limpieza de evidencias. */
		@DefaultValue("60") int limpiezaCadaMin,

		/**
		 * Horas tras las cuales una evidencia que nunca se confirmó se da por abandonada. La URL de subida vence mucho
		 * antes, así que pasado este tiempo ya no puede llegar.
		 */
		@DefaultValue("24") int horasAbandono,

		/** Días que se conserva una evidencia. Después se descarta, y S3 borra el archivo con su propia regla. */
		@DefaultValue("90") int diasRetencion,

		/**
		 * Si se aceptan videos. Por ahora no: la foto y el audio cubren lo que necesita la tripulación, y un video pesa
		 * más, tarda más en analizarse y cuesta más. El análisis de video sigue disponible para volver a activarlo.
		 */
		@DefaultValue("false") boolean videoHabilitado) {

	public boolean admite(Modalidad modalidad) {
		return modalidad != Modalidad.VIDEO || videoHabilitado;
	}

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

	public Duration abandono() {
		return Duration.ofHours(horasAbandono);
	}

	public Duration retencion() {
		return Duration.ofDays(diasRetencion);
	}

	public Duration vigenciaLectura() {
		return Duration.ofMinutes(Math.max(minutosLectura, 5));
	}

}
