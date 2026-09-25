package com.uem.ambulancias.emergencias.domain;

import java.util.Map;
import java.util.Set;

/**
 * Estados del pedido de traslado. De {@code ASIGNADO} en adelante el detalle fino lo lleva la atención: acá solo
 * interesa si el pedido sigue vivo, si terminó y cómo terminó.
 */
public enum EstadoTraslado {

	/** Agendado con fecha, todavía sin unidad. Confirmar no es reservar: nadie guardó una ambulancia. */
	PROGRAMADO,
	/** Llegó la hora de salir: se busca unidad y se reintenta hasta la última salida posible. */
	BUSCANDO_UNIDAD,
	/** Ya existe la atención y la unidad quedó tomada. */
	ASIGNADO,
	/** Se entregó al paciente en el destino. */
	COMPLETADO,
	/** Fue una unidad pero nadie viajó. El porqué está en el motivo de la atención. */
	NO_REALIZADO,
	/** Se pasó la última salida posible sin conseguir unidad. */
	NO_CUBIERTO,
	/** Lo retiró quien lo pidió. */
	CANCELADO;

	/** Estados vivos: el pedido todavía espera algo. Los demás son finales. */
	public static final Set<EstadoTraslado> VIGENTES = Set.of(PROGRAMADO, BUSCANDO_UNIDAD, ASIGNADO);

	/**
	 * La vuelta de {@code ASIGNADO} a {@code BUSCANDO_UNIDAD} no es un error: es el paramédico que rechaza o la
	 * unidad que no corresponde, y el pedido sigue vivo buscando otra.
	 */
	private static final Map<EstadoTraslado, Set<EstadoTraslado>> TRANSICIONES = Map.of(
			PROGRAMADO, Set.of(BUSCANDO_UNIDAD, CANCELADO),
			BUSCANDO_UNIDAD, Set.of(ASIGNADO, NO_CUBIERTO, CANCELADO),
			ASIGNADO, Set.of(COMPLETADO, NO_REALIZADO, BUSCANDO_UNIDAD, CANCELADO),
			COMPLETADO, Set.of(),
			NO_REALIZADO, Set.of(),
			NO_CUBIERTO, Set.of(),
			CANCELADO, Set.of());

	public boolean isVigente() {
		return VIGENTES.contains(this);
	}

	public boolean puedePasarA(EstadoTraslado nuevo) {
		return TRANSICIONES.get(this).contains(nuevo);
	}

}
