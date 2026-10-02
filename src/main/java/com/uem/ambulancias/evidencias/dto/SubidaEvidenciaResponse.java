package com.uem.ambulancias.evidencias.dto;

import java.time.Instant;
import java.util.Map;

import com.uem.ambulancias.evidencias.service.EvidenciaConSubida;

/**
 * A dónde subir el archivo. La app hace un PUT a {@code urlSubida} con el archivo como cuerpo y exactamente estas
 * {@code cabeceras}; si cambia alguna, el almacén lo rechaza. Pasado {@code venceEn} se pide otra URL.
 */
public record SubidaEvidenciaResponse(Long evidenciaId, String urlSubida, Map<String, String> cabeceras,
		Instant venceEn) {

	public static SubidaEvidenciaResponse de(EvidenciaConSubida registrada) {
		return new SubidaEvidenciaResponse(registrada.evidencia().getId(), registrada.subida().url(),
				registrada.subida().cabeceras(), registrada.subida().venceEn());
	}

}
