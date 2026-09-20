package com.uem.ambulancias.emergencias.service;

/**
 * Un incidente se creó o cambió. Se difunde en tiempo real después del commit.
 *
 * @param avisar el incidente quedó esperando unidad, porque acaba de nacer o porque su última atención activa se fue
 *               (volvió a ACTIVO). Además del tiempo real hay que avisar por push a las unidades disponibles: si no,
 *               el caso solo lo ve quien tenga la app abierta.
 */
public record IncidenteActualizado(Long incidenteId, boolean avisar) {
}
