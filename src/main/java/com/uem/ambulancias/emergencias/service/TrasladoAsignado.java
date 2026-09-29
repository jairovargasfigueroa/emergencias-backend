package com.uem.ambulancias.emergencias.service;

/**
 * Un traslado consiguió unidad. Se publica para avisarle por push a la tripulación: hasta acá el traslado solo
 * cambió en la base, y los que tienen que salir no se enteraron de nada.
 */
public record TrasladoAsignado(Long trasladoId, Long ambulanciaId) {
}
