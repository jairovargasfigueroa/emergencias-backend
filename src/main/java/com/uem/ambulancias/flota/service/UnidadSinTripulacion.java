package com.uem.ambulancias.flota.service;

/**
 * Salió de turno el último paramédico de la unidad: ya no hay nadie adentro que pueda reportar dónde está. Se
 * difunde después del commit, igual que las posiciones.
 */
public record UnidadSinTripulacion(Long ambulanciaId) {
}
