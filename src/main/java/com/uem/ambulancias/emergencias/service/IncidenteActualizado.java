package com.uem.ambulancias.emergencias.service;

/**
 * Un incidente se creó o cambió. Se difunde en tiempo real después del commit.
 */
public record IncidenteActualizado(Long incidenteId) {
}
