package com.uem.ambulancias.integracion.ia;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonRawValue;
import tools.jackson.databind.JsonNode;

/**
 * Los cuerpos de {@code /v1/analyses} y {@code /v1/summaries} tal como los define el servicio, con sus nombres en
 * inglés. Las respuestas ignoran los campos que no conocen, para que el servicio pueda sumar datos sin romper nada.
 */
final class ContratoIa {

	private ContratoIa() {
	}

	record AnalysisRequest(String jobId, Long evidenceId, Long alertId, Long incidentId, String downloadUrl,
			String mimeType, String checksumSha256) {

		@Override
		public String toString() {
			// La URL firmada es un secreto temporal: no puede terminar en un registro por un toString.
			return "AnalysisRequest[jobId=" + jobId + ", evidenceId=" + evidenceId + "]";
		}

	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record AnalysisResponse(String jobId, Long evidenceId, Long alertId, Long incidentId, String modality,
			String schemaVersion, JsonNode analysis, Provenance provenance) {
	}

	record SummaryRequest(Long incidentId, List<Alert> alerts, List<Evidence> evidences) {
	}

	record Alert(Long alertId, String reportedAt, String description, Integer affectedCount,
			Boolean reporterIsPatient) {
	}

	/** {@code analysis} ya es JSON: se escribe tal cual, sin volver a pasarlo a texto. */
	record Evidence(Long evidenceId, Long alertId, String modality, String receivedAt,
			@JsonRawValue String analysis) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record SummaryResponse(JsonNode summary, List<Long> usedEvidenceIds, List<Long> usedAlertIds,
			Provenance provenance) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record Provenance(String provider, String model, String promptVersion, String generatedAt, String method) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record ErrorResponse(String errorCode, Boolean retryable) {
	}

}
