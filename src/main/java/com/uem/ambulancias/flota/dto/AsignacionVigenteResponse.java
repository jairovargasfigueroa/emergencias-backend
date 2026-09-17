package com.uem.ambulancias.flota.dto;

import java.time.Instant;

import com.uem.ambulancias.flota.domain.Asignacion;

public record AsignacionVigenteResponse(Long asignacionId, Long ambulanciaId, String placa, Instant fechaInicio) {

	public static AsignacionVigenteResponse de(Asignacion asignacion) {
		return new AsignacionVigenteResponse(asignacion.getId(), asignacion.getAmbulancia().getId(),
				asignacion.getAmbulancia().getPlaca(), asignacion.getFechaInicio());
	}

}
