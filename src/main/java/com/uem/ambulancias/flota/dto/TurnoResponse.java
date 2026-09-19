package com.uem.ambulancias.flota.dto;

import java.time.Instant;

import com.uem.ambulancias.flota.domain.Turno;

/** Turno abierto del paramédico: desde cuándo está trabajando y con qué unidad. */
public record TurnoResponse(Long id, Instant inicio, Long ambulanciaId, String placa) {

	public static TurnoResponse de(Turno turno) {
		return new TurnoResponse(turno.getId(), turno.getInicio(), turno.getAmbulancia().getId(),
				turno.getAmbulancia().getPlaca());
	}

}
