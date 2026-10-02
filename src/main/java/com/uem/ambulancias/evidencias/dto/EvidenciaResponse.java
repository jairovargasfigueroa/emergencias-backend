package com.uem.ambulancias.evidencias.dto;

import com.uem.ambulancias.evidencias.domain.EstadoEvidencia;
import com.uem.ambulancias.evidencias.domain.Evidencia;
import com.uem.ambulancias.evidencias.domain.Modalidad;

/** Cómo quedó una evidencia. */
public record EvidenciaResponse(Long evidenciaId, Long alertaId, Modalidad modalidad, EstadoEvidencia estado) {

	public static EvidenciaResponse de(Evidencia evidencia) {
		return new EvidenciaResponse(evidencia.getId(), evidencia.getAlerta().getId(), evidencia.getModalidad(),
				evidencia.getEstado());
	}

}
