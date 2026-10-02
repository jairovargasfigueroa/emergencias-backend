package com.uem.ambulancias.evidencias.service;

/** Un incidente tiene una versión nueva del resumen. Se avisa después del commit. */
public record ResumenActualizado(Long incidenteId, int version) {
}
