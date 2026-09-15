package com.uem.ambulancias.flota.service;

/**
 * Publicación en tiempo real de la posición de las ambulancias. Se llama después del commit.
 */
public interface PublicadorDePosiciones {

	void publicarPosicion(PosicionActualizada posicion);

}
