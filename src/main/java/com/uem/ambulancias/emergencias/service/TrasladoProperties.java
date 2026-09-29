package com.uem.ambulancias.emergencias.service;

import java.time.Duration;

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

		/**
		 * El barrido solo le da un traslado a una unidad que reportó su posición hace menos de esto. Sin GPS
		 * reciente no se sabe si está cerca, ni si el teléfono de la tripulación sigue prendido para enterarse.
		 */
		@DefaultValue("5") int minutosPosicionVigente,

		/**
		 * Cuánto espera la tripulación en la puerta a un paciente que no está listo antes de poder retirarse. Es la
		 * tolerancia de cualquier servicio de traslados: pasado ese tiempo, el viaje se da por fallido.
		 */
		@DefaultValue("15") int minutosEspera,

		/** A qué hora del día anterior se le recuerda a la familia un traslado programado (0 a 23). */
		@DefaultValue("19") int horaRecordatorio,

		/** Unidad mínima para trasladar a alguien en camilla. */
		@DefaultValue("II") TipoUnidad minimoParaCamilla,

		/** Unidad mínima para alguien que necesita oxígeno durante el viaje. */
		@DefaultValue("II") TipoUnidad minimoParaOxigeno,

		/** Unidad mínima para alguien con vía, sonda o monitoreo. */
		@DefaultValue("III") TipoUnidad minimoParaEquipo,

		/** Zona horaria con la que el panel decide qué es "hoy". El servidor puede estar en UTC. */
		@DefaultValue("America/La_Paz") String zona) {

	/** La búsqueda que se le asegura a un traslado devuelto: la misma que tiene un pedido para ahora. */
	public Duration busquedaTrasDevolucion() {
		return Duration.ofMinutes(minutosVentanaInmediato);
	}

	public Duration acercamiento() {
		return Duration.ofMinutes(minutosAcercamiento);
	}

	public Duration posicionVigente() {
		return Duration.ofMinutes(minutosPosicionVigente);
	}

	public Duration espera() {
		return Duration.ofMinutes(minutosEspera);
	}

}
