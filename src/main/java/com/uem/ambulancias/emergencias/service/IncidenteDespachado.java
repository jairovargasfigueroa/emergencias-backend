package com.uem.ambulancias.emergencias.service;

/**
 * La central mandó una unidad a una emergencia. Se publica para avisarle por push a la tripulación: no la eligió
 * ella, así que no tiene por qué estar mirando la app.
 */
public record IncidenteDespachado(Long incidenteId, Long ambulanciaId) {
}
