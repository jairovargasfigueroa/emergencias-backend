package com.uem.ambulancias.evidencias.service;

import java.util.List;
import java.util.Map;

import com.uem.ambulancias.evidencias.domain.Evidencia;
import com.uem.ambulancias.evidencias.domain.ResumenIncidente;

/**
 * La versión vigente del resumen, o {@code null} si todavía no hay, con las evidencias subidas del incidente y, por id
 * de evidencia, lo que se puede leer de las que ya tienen análisis.
 */
public record ResumenConEvidencias(ResumenIncidente resumen, List<Evidencia> evidencias,
		Map<Long, TranscripcionDeEvidencia> transcripciones) {

	public TranscripcionDeEvidencia transcripcionDe(Long evidenciaId) {
		return transcripciones.getOrDefault(evidenciaId, TranscripcionDeEvidencia.VACIA);
	}

}
