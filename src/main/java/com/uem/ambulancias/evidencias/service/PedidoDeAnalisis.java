package com.uem.ambulancias.evidencias.service;

import java.util.UUID;

/**
 * Lo que necesita el servicio para analizar una evidencia. {@code urlLectura} es un secreto temporal: no va a ningún
 * registro.
 */
public record PedidoDeAnalisis(
		UUID trabajo,
		Long evidenciaId,
		Long alertaId,
		Long incidenteId,
		String urlLectura,
		String mimeType,
		String sha256) {

	@Override
	public String toString() {
		return "PedidoDeAnalisis[trabajo=" + trabajo + ", evidenciaId=" + evidenciaId + "]";
	}

}
