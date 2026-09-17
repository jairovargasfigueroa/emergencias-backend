package com.uem.ambulancias.emergencias.service;

import java.util.List;

import com.uem.ambulancias.emergencias.domain.Atencion;
import com.uem.ambulancias.emergencias.domain.Incidente;

/**
 * Un incidente de la consulta del panel con la cantidad de sus alertas y todas sus atenciones, en orden de toma.
 */
public record IncidenteConAtenciones(Incidente incidente, long cantidadAlertas, List<Atencion> atenciones) {
}
