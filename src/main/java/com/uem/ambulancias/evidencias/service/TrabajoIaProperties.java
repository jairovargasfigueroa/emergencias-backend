package com.uem.ambulancias.evidencias.service;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Cómo se reintentan los pedidos al servicio de análisis ({@code sga.ia.trabajos.*}). La espera se duplica en cada
 * intento, desde la inicial hasta la máxima.
 */
@ConfigurationProperties(prefix = "sga.ia.trabajos")
public record TrabajoIaProperties(

		/** Cuántas veces se intenta un trabajo antes de darlo por fallido. */
		@DefaultValue("5") int maximoIntentos,

		@DefaultValue("30") int esperaInicialSeg,

		@DefaultValue("900") int esperaMaximaSeg,

		/**
		 * Cuánto tiempo es de un worker el trabajo que tomó. Tiene que pasar la llamada más larga al servicio (unos
		 * cuatro minutos): si no, otro worker lo tomaría mientras el primero todavía espera la respuesta.
		 */
		@DefaultValue("6") int minutosBloqueo) {

	public Duration bloqueo() {
		return Duration.ofMinutes(minutosBloqueo);
	}

	/** Espera antes del próximo intento, sabiendo cuántos ya se hicieron. */
	public Duration esperaTras(int intentos) {
		long segundos = (long) esperaInicialSeg << Math.min(Math.max(intentos - 1, 0), 20);
		return Duration.ofSeconds(Math.min(segundos, esperaMaximaSeg));
	}

}
