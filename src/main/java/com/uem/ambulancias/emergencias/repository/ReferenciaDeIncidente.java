package com.uem.ambulancias.emergencias.repository;

/**
 * Una descripción que dejó el emisor de una alerta, junto al incidente al que pertenece. Sirve para traer de una
 * sola vez el texto con el que se nombra a varios incidentes en una pantalla.
 */
public record ReferenciaDeIncidente(Long incidenteId, String descripcion) {
}
