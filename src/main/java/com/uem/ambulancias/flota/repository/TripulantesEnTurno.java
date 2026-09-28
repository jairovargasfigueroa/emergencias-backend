package com.uem.ambulancias.flota.repository;

/**
 * Cuántos paramédicos tienen turno abierto en una unidad, resultado de una consulta agrupada.
 */
public record TripulantesEnTurno(Long ambulanciaId, Long cantidad) {
}
