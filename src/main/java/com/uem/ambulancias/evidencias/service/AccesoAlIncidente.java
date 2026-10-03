package com.uem.ambulancias.evidencias.service;

import com.uem.ambulancias.comun.error.AccesoDenegadoException;
import com.uem.ambulancias.comun.error.CodigoError;
import com.uem.ambulancias.emergencias.repository.AtencionRepository;
import com.uem.ambulancias.usuarios.domain.RolUsuario;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Quién puede ver el resumen de un incidente y sus archivos. La central ve todos; un paramédico, solo los del
 * incidente que está atendiendo ahora: lo que contó el ciudadano es para quien va a buscarlo, no para toda la flota.
 */
@Component
@RequiredArgsConstructor
public class AccesoAlIncidente {

	private final AtencionRepository atenciones;

	/** 403 con {@link CodigoError#SIN_ATENCION_EN_INCIDENTE} si no puede verlo. */
	public void exigir(Long incidenteId, Long usuarioId, RolUsuario rol) {
		if (rol == RolUsuario.ADMIN) {
			return;
		}
		if (rol == RolUsuario.PARAMEDICO && atenciones.paramedicoAtiendeIncidente(usuarioId, incidenteId)) {
			return;
		}
		throw new AccesoDenegadoException(CodigoError.SIN_ATENCION_EN_INCIDENTE,
				"No tienes una atención en curso en el incidente " + incidenteId + ".");
	}

}
