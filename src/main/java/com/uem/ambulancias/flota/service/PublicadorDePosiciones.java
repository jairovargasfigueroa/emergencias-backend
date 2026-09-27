package com.uem.ambulancias.flota.service;

/**
 * Publicación en tiempo real de la posición de las ambulancias. Se llama después del commit.
 */
public interface PublicadorDePosiciones {

	void publicarPosicion(PosicionActualizada posicion);

	/**
	 * Borra la posición publicada de una unidad que dejó de estar en servicio. Sin esto el nodo guarda para
	 * siempre el último punto de toda ambulancia que alguna vez reportó, y el mapa termina mostrando unidades
	 * quietas en donde estaban hace meses como si siguieran ahí.
	 */
	void retirarPosicion(Long ambulanciaId);

}
