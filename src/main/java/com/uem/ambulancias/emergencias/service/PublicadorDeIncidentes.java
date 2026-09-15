package com.uem.ambulancias.emergencias.service;

import com.uem.ambulancias.flota.service.PosicionActualizada;

/**
 * Publicación en tiempo real de los incidentes para las apps. Se llama siempre después del commit, para que ningún
 * listener vea datos que luego se deshagan.
 */
public interface PublicadorDeIncidentes {

	/** Crea o reemplaza el incidente en el nodo de incidentes abiertos, que escuchan los paramédicos. */
	void publicarIncidenteAbierto(IncidentePublicado incidente);

	/** Quita el incidente de los abiertos cuando pasa a un estado final. */
	void retirarIncidente(Long incidenteId);

	/** Crea o reemplaza el seguimiento del incidente, que escucha la app del ciudadano. */
	void publicarSeguimiento(SeguimientoPublicado seguimiento);

	/** Posición en vivo de una unidad dentro del seguimiento. Solo se llama si su atención sigue activa. */
	void publicarPosicionEnSeguimiento(Long incidenteId, PosicionActualizada posicion);

}
