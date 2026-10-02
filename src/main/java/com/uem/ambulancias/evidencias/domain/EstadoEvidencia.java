package com.uem.ambulancias.evidencias.domain;

/**
 * Vida de una evidencia. Nace PENDIENTE_SUBIDA cuando se firma la subida, pasa a SUBIDA cuando el servidor comprueba
 * que el archivo está en el almacén, y termina ANALIZADA o FALLIDA según lo que responda el análisis. La limpieza
 * periódica la pasa a DESCARTADA si la subida nunca se confirmó o si ya pasó el tiempo de retención: desde ahí no
 * cuenta para los límites ni se le muestra al personal.
 */
public enum EstadoEvidencia {

	PENDIENTE_SUBIDA,
	SUBIDA,
	ANALIZADA,
	FALLIDA,
	DESCARTADA

}
