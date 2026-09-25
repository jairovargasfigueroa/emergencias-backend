package com.uem.ambulancias.emergencias.domain;

public enum MotivoCancelacionAtencion {

	AVERIA,
	NO_SE_ENCONTRO_PACIENTE,
	DESVIADA,
	/** Solo en traslados: el paramédico devuelve el traslado que se le asignó y vuelve a buscarse otra unidad. */
	RECHAZADA_POR_PARAMEDICO,
	/** Solo en traslados: el solicitante retiró el pedido con la unidad ya en camino. */
	CANCELADA_POR_SOLICITANTE,
	OTRO

}
