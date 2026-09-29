package com.uem.ambulancias.emergencias.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Parámetros del centro de control ({@code sga.operacion.*}). Son decisiones de pantalla, no reglas del negocio:
 * cuánto pasado mostrar y cuándo dar por perdida la señal de una unidad se ajustan mirando la pantalla en uso.
 */
@ConfigurationProperties(prefix = "sga.operacion")
public record OperacionProperties(

		/** Cuánto hacia atrás se miran los hitos para armar la bitácora. */
		@DefaultValue("12") int ventanaEventosHoras,

		/** Cuántos eventos devuelve la bitácora, contando desde el más nuevo. */
		@DefaultValue("40") int maximoEventos,

		/**
		 * Segundos sin reportar posición a partir de los cuales la unidad se marca sin señal. El valor por defecto
		 * es el doble de lo que tarda en guardarse una posición: un reporte perdido no debería encender la alarma.
		 */
		@DefaultValue("60") int sinSenalSeg) {
}
