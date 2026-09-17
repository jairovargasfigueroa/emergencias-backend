package com.uem.ambulancias.flota.service;

import java.time.Instant;

/**
 * Nueva posición de una ambulancia en servicio. Se difunde después del commit.
 */
public record PosicionActualizada(Long ambulanciaId, double latitud, double longitud, Instant momento) {
}
