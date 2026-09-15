package com.uem.ambulancias.emergencias.dto;

import java.time.Instant;

import com.uem.ambulancias.emergencias.domain.Alerta;
import com.uem.ambulancias.emergencias.domain.Incidente;

/**
 * Alerta creada. {@code incidenteId} es el incidente al que quedó vinculada: la app lo usa para el seguimiento.
 */
public record AlertaResponse(Long alertaId, Long incidenteId, Instant fechaHora) {

	public static AlertaResponse de(Alerta alerta, Incidente incidente) {
		return new AlertaResponse(alerta.getId(), incidente.getId(), alerta.getFechaHora());
	}

}
