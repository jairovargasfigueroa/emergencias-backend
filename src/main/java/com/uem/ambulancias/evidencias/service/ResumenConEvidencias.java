package com.uem.ambulancias.evidencias.service;

import java.util.List;

import com.uem.ambulancias.evidencias.domain.Evidencia;
import com.uem.ambulancias.evidencias.domain.ResumenIncidente;

/** La versión vigente del resumen, o {@code null} si todavía no hay, con las evidencias subidas del incidente. */
public record ResumenConEvidencias(ResumenIncidente resumen, List<Evidencia> evidencias) {
}
