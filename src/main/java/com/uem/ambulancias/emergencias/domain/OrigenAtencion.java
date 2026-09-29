package com.uem.ambulancias.emergencias.domain;

/**
 * De dónde salió la atención. En la base es un {@code check} entre dos columnas ({@code incidente_id} y
 * {@code traslado_id}, exactamente una puesta); hacia afuera conviene un solo dato que diga cuál de las dos es,
 * para que el cliente no tenga que deducirlo mirando cuál llegó nula.
 */
public enum OrigenAtencion {

	/** Una emergencia: alguien pidió auxilio y la unidad salió a atenderla. */
	INCIDENTE,

	/** Un traslado programado o inmediato, pedido con anticipación. */
	TRASLADO;

	public static OrigenAtencion de(Atencion atencion) {
		return atencion.esDeTraslado() ? TRASLADO : INCIDENTE;
	}

}
