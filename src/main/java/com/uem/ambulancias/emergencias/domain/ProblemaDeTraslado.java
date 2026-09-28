package com.uem.ambulancias.emergencias.domain;

import java.time.Instant;

/**
 * Por qué un traslado necesita que el administrador haga algo, como lo haría el despachador de una central. No
 * se guarda: sale del estado del traslado y de su unidad en este momento.
 */
public enum ProblemaDeTraslado {

	/** Ya es hora de salir y no hay unidad. El barrido sigue intentando hasta la última salida posible. */
	SIN_UNIDAD,
	/** Se pasó la última salida posible sin unidad. Alguien tiene que avisarle a la familia. */
	NO_CUBIERTO,
	/**
	 * Terminó la ventana de recogida y la unidad asignada todavía no llegó a la puerta. Qué hacer lo decide una
	 * persona: puede estar a dos cuadras, o puede haberse quedado sin forma de avisar.
	 */
	UNIDAD_ATRASADA;

	/** {@code atencion} es la que tiene el traslado ahora, o nula. Nulo si el traslado no necesita nada. */
	public static ProblemaDeTraslado de(Traslado traslado, Atencion atencion, Instant ahora) {
		return switch (traslado.getEstado()) {
			case BUSCANDO_UNIDAD -> SIN_UNIDAD;
			case NO_CUBIERTO -> traslado.getHoraFamiliaAvisada() == null ? NO_CUBIERTO : null;
			case ASIGNADO -> atencion != null && atencion.getEstado() == EstadoAtencion.EN_CAMINO
					&& ahora.isAfter(traslado.getHoraRecogidaHasta()) ? UNIDAD_ATRASADA : null;
			default -> null;
		};
	}

}
