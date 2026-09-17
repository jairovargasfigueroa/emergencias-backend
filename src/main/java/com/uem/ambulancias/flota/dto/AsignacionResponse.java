package com.uem.ambulancias.flota.dto;

import java.time.Instant;

import com.uem.ambulancias.flota.domain.Asignacion;

public record AsignacionResponse(
		Long id,
		Long paramedicoId,
		String paramedicoNombre,
		Long ambulanciaId,
		String placa,
		Instant fechaInicio,
		Instant fechaFin,
		boolean vigente) {

	public static AsignacionResponse de(Asignacion asignacion) {
		return new AsignacionResponse(asignacion.getId(), asignacion.getParamedico().getId(),
				asignacion.getParamedico().getNombreCompleto(), asignacion.getAmbulancia().getId(),
				asignacion.getAmbulancia().getPlaca(), asignacion.getFechaInicio(), asignacion.getFechaFin(),
				asignacion.isVigente());
	}

}
