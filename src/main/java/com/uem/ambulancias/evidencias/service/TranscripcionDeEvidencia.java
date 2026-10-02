package com.uem.ambulancias.evidencias.service;

import java.util.List;

/**
 * Lo que el personal puede leer de una evidencia analizada en lugar de escucharla: la transcripción del audio o del
 * video y, en video, los momentos marcados. Cada parte viene nula si el análisis no la trae.
 */
public record TranscripcionDeEvidencia(String transcripcion, List<Momento> lineaDeTiempo) {

	public static final TranscripcionDeEvidencia VACIA = new TranscripcionDeEvidencia(null, null);

	/** Un momento del video, contado en segundos desde el inicio. */
	public record Momento(double segundo, String texto) {
	}

}
