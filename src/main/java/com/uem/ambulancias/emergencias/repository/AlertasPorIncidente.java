package com.uem.ambulancias.emergencias.repository;

/**
 * Cantidad de alertas de un incidente, resultado de una consulta agrupada.
 */
public record AlertasPorIncidente(Long incidenteId, Long cantidad) {
}
