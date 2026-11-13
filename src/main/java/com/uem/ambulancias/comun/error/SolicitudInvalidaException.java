package com.uem.ambulancias.comun.error;

/** Un pedido que no tiene sentido tal como viene, por ejemplo un período al revés. Se responde con 400. */
public class SolicitudInvalidaException extends RuntimeException {

	public SolicitudInvalidaException(String mensaje) {
		super(mensaje);
	}

}
