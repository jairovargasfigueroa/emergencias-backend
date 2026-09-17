package com.uem.ambulancias.flota.dto;

import com.uem.ambulancias.flota.domain.Asignacion;
import com.uem.ambulancias.usuarios.domain.Usuario;

public record ParamedicoResponse(
		Long id,
		String nombreCompleto,
		String telefono,
		boolean activo,
		AsignacionVigenteResponse asignacionVigente) {

	/** {@code asignacionVigente} puede ser {@code null} si el paramédico no tiene una. */
	public static ParamedicoResponse de(Usuario paramedico, Asignacion asignacionVigente) {
		return new ParamedicoResponse(paramedico.getId(), paramedico.getNombreCompleto(), paramedico.getTelefono(),
				paramedico.isActivo(), asignacionVigente == null ? null : AsignacionVigenteResponse.de(asignacionVigente));
	}

}
