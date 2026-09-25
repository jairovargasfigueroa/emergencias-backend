package com.uem.ambulancias.emergencias.service;

/**
 * Una ambulancia quedó libre. Se publica para que un traslado que está esperando la agarre en el momento, sin
 * tener que aguantar hasta el próximo barrido.
 */
public record UnidadLiberada(Long ambulanciaId) {
}
