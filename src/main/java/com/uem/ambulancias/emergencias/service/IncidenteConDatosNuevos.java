package com.uem.ambulancias.emergencias.service;

/**
 * El incidente sumó una alerta o una alerta sumó detalles. Se publica dentro de la transacción, con el incidente ya
 * bloqueado: quien lo escuche trabaja en esa misma transacción.
 */
public record IncidenteConDatosNuevos(Long incidenteId) {
}
