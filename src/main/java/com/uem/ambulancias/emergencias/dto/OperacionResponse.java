package com.uem.ambulancias.emergencias.dto;

import java.util.List;

/**
 * Todo el estado de la operación en una sola respuesta: la flota, lo que está sin cubrir y lo que acaba de pasar.
 * La pantalla se refresca entera cada pocos segundos, así que pedir esto por partes sería multiplicar las llamadas
 * sin ganar nada.
 */
public record OperacionResponse(

		List<UnidadEnOperacionResponse> unidades,

		/** Incidentes a los que todavía no va nadie. */
		List<IncidenteSinCubrirResponse> incidentesSinCubrir,

		/** Traslados que están buscando unidad y no la consiguen: la misma bandeja de {@code /traslados/problemas}. */
		List<TrasladoDelPanelResponse> trasladosSinCubrir,

		/** La bitácora, del hito más nuevo al más viejo. */
		List<EventoDeOperacionResponse> eventos,

		/**
		 * A partir de cuántos segundos sin reportar posición se considera que una unidad perdió la señal. Viaja en
		 * la respuesta para que el panel no tenga que adivinar un número que en realidad se configura acá.
		 */
		int umbralSinSenalSeg) {
}
