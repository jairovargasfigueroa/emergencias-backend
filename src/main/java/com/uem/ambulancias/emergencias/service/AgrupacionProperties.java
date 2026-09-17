package com.uem.ambulancias.emergencias.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Parámetros de agrupación de alertas ({@code sga.agrupacion.radio-m} y {@code sga.agrupacion.ventana-min}).
 * Cambiarlos no requiere tocar la lógica.
 */
@ConfigurationProperties(prefix = "sga.agrupacion")
public record AgrupacionProperties(

		/** Distancia máxima, en metros, entre la alerta y el incidente. */
		@DefaultValue("150") int radioM,

		/** Minutos desde la creación del incidente durante los que todavía agrupa alertas. */
		@DefaultValue("30") int ventanaMin) {
}
