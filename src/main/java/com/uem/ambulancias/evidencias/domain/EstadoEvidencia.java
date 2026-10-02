package com.uem.ambulancias.evidencias.domain;

/**
 * Vida de una evidencia. Nace PENDIENTE_SUBIDA cuando se firma la subida, pasa a SUBIDA cuando el servidor comprueba
 * que el archivo está en el almacén, y termina ANALIZADA o FALLIDA según lo que responda el análisis.
 */
public enum EstadoEvidencia {

	PENDIENTE_SUBIDA,
	SUBIDA,
	ANALIZADA,
	FALLIDA,
	DESCARTADA

}
