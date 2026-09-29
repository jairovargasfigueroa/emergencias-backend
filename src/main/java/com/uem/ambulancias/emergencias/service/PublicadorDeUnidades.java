package com.uem.ambulancias.emergencias.service;

/**
 * Publica en tiempo real el estado de cada unidad. Solo escribe el servidor; la app de la tripulación escucha el de
 * su unidad.
 */
public interface PublicadorDeUnidades {

	/** Crea o reemplaza el nodo de la unidad. */
	void publicarUnidad(UnidadPublicada unidad);

}
