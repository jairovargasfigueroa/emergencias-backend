package com.uem.ambulancias.emergencias.service;

/**
 * Publicación en tiempo real para las apps. Se llama siempre después del commit, para que ningún listener vea
 * datos que luego se deshagan.
 */
public interface PublicadorTiempoReal {

	/** Crea o reemplaza el incidente en el nodo de incidentes abiertos. */
	void publicarIncidenteAbierto(IncidentePublicado incidente);

	/** Quita el incidente de los abiertos cuando pasa a un estado final. */
	void retirarIncidente(Long incidenteId);

}
