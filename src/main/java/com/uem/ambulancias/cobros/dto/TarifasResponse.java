package com.uem.ambulancias.cobros.dto;

import java.util.List;

/** Las que rigen hoy, una por tipo de servicio (nula si nunca se cargó), y todas las registradas. */
public record TarifasResponse(TarifaResponse emergencia, TarifaResponse traslado, List<TarifaResponse> historial) {
}
