package com.uem.ambulancias.emergencias.dto;

import java.time.Instant;

import com.uem.ambulancias.emergencias.domain.Atencion;
import com.uem.ambulancias.emergencias.domain.EstadoAtencion;

public record AtencionResponse(
		Long id,
		Long incidenteId,
		Long ambulanciaId,
		String placa,
		EstadoAtencion estado,
		Instant horaToma) {

	public static AtencionResponse de(Atencion atencion) {
		return new AtencionResponse(atencion.getId(), atencion.getIncidente().getId(), atencion.getAmbulancia().getId(),
				atencion.getAmbulancia().getPlaca(), atencion.getEstado(), atencion.getHoraToma());
	}

}
