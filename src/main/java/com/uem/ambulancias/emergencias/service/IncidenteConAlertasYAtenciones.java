package com.uem.ambulancias.emergencias.service;

import java.util.List;

import com.uem.ambulancias.emergencias.domain.Alerta;
import com.uem.ambulancias.emergencias.domain.Atencion;
import com.uem.ambulancias.emergencias.domain.Incidente;

/**
 * Un incidente con sus alertas, en orden de emisión, y sus atenciones, en orden de toma.
 */
public record IncidenteConAlertasYAtenciones(Incidente incidente, List<Alerta> alertas, List<Atencion> atenciones) {
}
