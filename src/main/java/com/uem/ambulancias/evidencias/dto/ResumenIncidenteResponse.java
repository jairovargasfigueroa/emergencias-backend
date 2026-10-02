package com.uem.ambulancias.evidencias.dto;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.uem.ambulancias.evidencias.domain.ResumenIncidente;
import com.uem.ambulancias.evidencias.service.ResumenConEvidencias;

/**
 * El resumen preliminar vigente del incidente y las evidencias que tiene. Es apoyo para el personal: no es un
 * diagnóstico ni un triaje. Mientras no haya ningún resumen, {@code version} y {@code resumen} vienen nulos y las
 * evidencias se pueden ver igual, con la transcripción y la línea de tiempo de las que ya se analizaron.
 *
 * @param resumen el objeto {@code summary} del servicio de análisis, sin cambios
 */
public record ResumenIncidenteResponse(
		Long incidenteId,
		Integer version,
		@JsonRawValue String resumen,
		List<Long> evidenciasUsadas,
		List<Long> alertasUsadas,
		String metodo,
		String modelo,
		String versionPrompt,
		Instant generadoEn,
		List<EvidenciaDelIncidenteResponse> evidencias) {

	public static ResumenIncidenteResponse de(Long incidenteId, ResumenConEvidencias consulta) {
		List<EvidenciaDelIncidenteResponse> archivos = consulta.evidencias().stream()
				.map(evidencia -> EvidenciaDelIncidenteResponse.de(evidencia,
						consulta.transcripcionDe(evidencia.getId())))
				.toList();
		ResumenIncidente resumen = consulta.resumen();
		if (resumen == null) {
			return new ResumenIncidenteResponse(incidenteId, null, null, List.of(), List.of(), null, null, null, null,
					archivos);
		}
		return new ResumenIncidenteResponse(incidenteId, resumen.getVersion(), resumen.getResumen(),
				resumen.getEvidenciasUsadas(), resumen.getAlertasUsadas(), resumen.getMetodo(), resumen.getModelo(),
				resumen.getVersionPrompt(), resumen.getGeneradoEn(), archivos);
	}

}
