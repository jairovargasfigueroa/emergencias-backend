package com.uem.ambulancias.flota.dto;

import com.uem.ambulancias.flota.domain.Asignacion;
import com.uem.ambulancias.usuarios.domain.Usuario;

public record ParamedicoResponse(
		Long id,
		String nombreCompleto,
		String telefono,
		boolean activo,
		AsignacionVigenteResponse asignacionVigente,
		/** Si tiene un turno abierto ahora. Mientras lo tenga, no se lo puede desactivar ni cambiar de unidad. */
		boolean enTurno,
		/** Tiene PIN y un teléfono vinculado: ya puede entrar a su app. Si no, necesita un código de activación. */
		boolean activado,
		/** Su PIN quedó bloqueado por intentos fallidos: no entra hasta que se le genere un código nuevo. */
		boolean bloqueado) {

	/** {@code asignacionVigente} puede ser {@code null} si el paramédico no tiene una. */
	public static ParamedicoResponse de(Usuario paramedico, Asignacion asignacionVigente, boolean enTurno) {
		return new ParamedicoResponse(paramedico.getId(), paramedico.getNombreCompleto(), paramedico.getTelefono(),
				paramedico.isActivo(), asignacionVigente == null ? null : AsignacionVigenteResponse.de(asignacionVigente),
				enTurno, paramedico.isAccesoActivado(), paramedico.isPinBloqueado());
	}

}
