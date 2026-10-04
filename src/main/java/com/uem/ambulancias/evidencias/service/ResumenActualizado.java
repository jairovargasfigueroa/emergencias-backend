package com.uem.ambulancias.evidencias.service;

/**
 * Un incidente tiene una versión nueva del resumen. Se avisa después del commit.
 *
 * @param importante si cambió algo que la tripulación tiene que saber ({@code DatosClaveDelResumen}); la primera
 *                   versión siempre lo es
 */
public record ResumenActualizado(Long incidenteId, int version, boolean importante) {
}
