package com.uem.ambulancias.emergencias.service;

import com.uem.ambulancias.flota.domain.EstadoAmbulancia;

/**
 * Lo que se publica de una unidad: su estado y la atención que la tiene ocupada, nula si está libre. La app de la
 * tripulación no lo toma como dato: le avisa que algo cambió, y el detalle lo vuelve a pedir a la API.
 */
public record UnidadPublicada(Long ambulanciaId, EstadoAmbulancia estado, Long atencionId) {
}
