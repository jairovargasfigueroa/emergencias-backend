package com.uem.ambulancias.emergencias.service;

import com.uem.ambulancias.flota.domain.TipoUnidad;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Parámetros de los traslados ({@code sga.traslados.*}). Son supuestos, no verdades: a los pocos meses los hitos
 * que ya se guardan van a dar los tiempos reales medidos, y entonces estos números se ajustan sin tocar código.
 *
 * <p>Los tres mínimos de tipo de unidad dependen de cómo esté equipada la flota, así que también viven acá: si
 * una unidad de soporte básico no llevara oxígeno, se cambia una línea y no una regla.
 */
@ConfigurationProperties(prefix = "sga.traslados")
public record TrasladoProperties(

		/** Cuánto más largo es el recorrido por calle que la línea recta. */
		@DefaultValue("1.4") double factorCalle,

		/** Velocidad promedio de una unidad en ciudad, en km/h. */
		@DefaultValue("25") double velocidadKmH,

		/** Cuánto se tarda en llegar al origen, mientras no se sepa qué unidad va a ir. */
		@DefaultValue("15") int minutosAcercamiento,

		/** Sacar al paciente de la casa y acomodarlo en la unidad. */
		@DefaultValue("15") int minutosRecogida,

		/** Colchón sobre la última salida posible. Es también la ventana que se le promete a la familia. */
		@DefaultValue("20") int minutosMargen,

		/** Cuánto se busca una unidad para un pedido inmediato antes de darlo por no cubierto. */
		@DefaultValue("60") int minutosVentanaInmediato,

		/** Con cuánta anticipación se avisa al administrador de un traslado que pinta mal. */
		@DefaultValue("120") int minutosAvisoTemprano,

		/** Unidad mínima para trasladar a alguien en camilla. */
		@DefaultValue("II") TipoUnidad minimoParaCamilla,

		/** Unidad mínima para alguien que necesita oxígeno durante el viaje. */
		@DefaultValue("II") TipoUnidad minimoParaOxigeno,

		/** Unidad mínima para alguien con vía, sonda o monitoreo. */
		@DefaultValue("III") TipoUnidad minimoParaEquipo) {
}
