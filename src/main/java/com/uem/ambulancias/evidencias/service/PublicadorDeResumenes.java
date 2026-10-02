package com.uem.ambulancias.evidencias.service;

/**
 * Aviso en tiempo real de que un incidente tiene una versión nueva del resumen. Solo viaja la señal, nunca el
 * contenido: quien la escucha pide el resumen a la API, que es la que controla quién puede verlo.
 */
public interface PublicadorDeResumenes {

	void publicarResumenNuevo(Long incidenteId, int version);

}
