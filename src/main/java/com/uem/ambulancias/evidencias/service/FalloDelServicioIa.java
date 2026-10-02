package com.uem.ambulancias.evidencias.service;

import lombok.Getter;

/**
 * El servicio de análisis no devolvió un resultado. {@code codigo} es su {@code errorCode}, o uno propio si ni
 * siquiera respondió; {@code desenlace} dice qué hacer con el trabajo.
 */
@Getter
public class FalloDelServicioIa extends RuntimeException {

	public enum Desenlace {

		/** Algo pasajero: reintentar más tarde. */
		REINTENTAR,

		/** La URL de lectura venció antes de que el servicio la usara: firmar otra y reintentar. */
		FIRMAR_DE_NUEVO,

		/** El archivo o el pedido no sirven: reintentar daría lo mismo. */
		DEFINITIVO,

		/** Token, proveedor o configuración del servicio: no se reintenta solo, lo revisa una persona. */
		REVISION

	}

	private final String codigo;
	private final Desenlace desenlace;

	public FalloDelServicioIa(String codigo, Desenlace desenlace) {
		super("El servicio de análisis respondió " + codigo + ".");
		this.codigo = codigo;
		this.desenlace = desenlace;
	}

	public FalloDelServicioIa(String codigo, Desenlace desenlace, Throwable causa) {
		super("El servicio de análisis no respondió (" + codigo + ").", causa);
		this.codigo = codigo;
		this.desenlace = desenlace;
	}

}
