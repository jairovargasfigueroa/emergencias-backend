package com.uem.ambulancias.evidencias.dto;

import java.util.List;

import com.uem.ambulancias.evidencias.domain.EstadoEvidencia;
import com.uem.ambulancias.evidencias.domain.Evidencia;
import com.uem.ambulancias.evidencias.domain.Modalidad;
import com.uem.ambulancias.evidencias.service.TranscripcionDeEvidencia;

/**
 * Una evidencia del incidente tal como la ve el personal: además de su estado, lo que se puede leer en vez de
 * escucharla. El ciudadano que la sube recibe {@link EvidenciaResponse}, sin estas partes.
 *
 * @param transcripcion lo que se habla en el audio o el video; nula si no hay o si todavía no se analizó
 * @param lineaDeTiempo los momentos marcados en un video; nula en audio, imagen o sin análisis
 */
public record EvidenciaDelIncidenteResponse(Long evidenciaId, Long alertaId, Modalidad modalidad,
		EstadoEvidencia estado, String transcripcion, List<Momento> lineaDeTiempo) {

	/** Un momento del video, en segundos desde el inicio. */
	public record Momento(double segundo, String texto) {
	}

	public static EvidenciaDelIncidenteResponse de(Evidencia evidencia, TranscripcionDeEvidencia leido) {
		List<Momento> momentos = leido.lineaDeTiempo() == null ? null
				: leido.lineaDeTiempo().stream().map(uno -> new Momento(uno.segundo(), uno.texto())).toList();
		return new EvidenciaDelIncidenteResponse(evidencia.getId(), evidencia.getAlerta().getId(),
				evidencia.getModalidad(), evidencia.getEstado(), leido.transcripcion(), momentos);
	}

}
