package com.uem.ambulancias.emergencias.service;

/**
 * Un traslado consiguió unidad. Se publica para avisarle por push al paramédico responsable: hasta acá el
 * traslado solo cambió en la base, y el que tiene que manejar no se enteró de nada.
 */
public record TrasladoAsignado(Long trasladoId, Long paramedicoId) {
}
