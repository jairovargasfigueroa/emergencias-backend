package com.uem.ambulancias.evidencias.dto;

import java.time.Instant;

import com.uem.ambulancias.evidencias.service.LecturaFirmada;

/** URL temporal para leer el archivo. Pasado {@code venceEn} se pide otra. */
public record LecturaEvidenciaResponse(String url, Instant venceEn) {

	public static LecturaEvidenciaResponse de(LecturaFirmada lectura) {
		return new LecturaEvidenciaResponse(lectura.url(), lectura.venceEn());
	}

}
