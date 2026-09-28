package com.uem.ambulancias.emergencias.service;

/**
 * Algo que cambió en un traslado y que quien lo pidió tiene que saber aunque tenga la app cerrada. Que tiene unidad
 * se avisa con {@link TrasladoAsignado}, que ya existe para la tripulación.
 */
public record NovedadDelTraslado(Long trasladoId, Tipo tipo) {

	public enum Tipo {

		/** La unidad llegó a buscar al paciente. */
		UNIDAD_EN_LA_PUERTA,
		/** Una unidad lo devolvió y se busca otra: la hora de recogida pudo cambiar. */
		NUEVA_BUSQUEDA,
		/** Se pasó la última salida posible sin conseguir unidad. */
		NO_CUBIERTO,
		/** La noche anterior de un traslado programado, como la llamada de confirmación de una central. */
		RECORDATORIO

	}

}
