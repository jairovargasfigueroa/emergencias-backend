package com.uem.ambulancias.cobros.domain;

import com.uem.ambulancias.emergencias.domain.Atencion;

/** Lo que se cobra y para qué hay una tarifa de referencia: una emergencia o un traslado programado. */
public enum TipoServicio {

	EMERGENCIA,
	TRASLADO;

	public static TipoServicio de(Atencion atencion) {
		return atencion.esDeTraslado() ? TRASLADO : EMERGENCIA;
	}

}
