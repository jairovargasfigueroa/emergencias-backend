package com.uem.ambulancias.evidencias.service;

import java.time.Instant;
import java.util.List;

/**
 * Todo lo que se sabe de un incidente: todas sus alertas y todos los análisis vigentes. El servicio no recibe el
 * resumen anterior, así que el resultado depende solo de estas fuentes y no del orden en que llegaron.
 */
public record PedidoDeResumen(Long incidenteId, List<AlertaDelIncidente> alertas,
		List<EvidenciaAnalizada> evidencias) {

	public record AlertaDelIncidente(Long alertaId, Instant emitidaEn, String descripcion, Integer cantidadAfectados,
			Boolean emisorEsPaciente) {
	}

	/** {@code analisisJson} es el mismo objeto que devolvió el análisis, sin cambios. */
	public record EvidenciaAnalizada(Long evidenciaId, Long alertaId, String modalidad, Instant recibidaEn,
			String analisisJson) {
	}

}
