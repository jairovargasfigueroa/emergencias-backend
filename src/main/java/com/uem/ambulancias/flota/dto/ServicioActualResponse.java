package com.uem.ambulancias.flota.dto;

import com.uem.ambulancias.flota.domain.Ambulancia;
import com.uem.ambulancias.flota.domain.Asignacion;
import com.uem.ambulancias.usuarios.domain.Usuario;

/**
 * {@code ambulancia} es la de la asignación vigente ({@code null} si no tiene). {@code enServicio}: tiene asignación
 * vigente a una ambulancia activa.
 */
public record ServicioActualResponse(ParamedicoResponse paramedico, AmbulanciaResponse ambulancia, boolean enServicio) {

	public static ServicioActualResponse de(Usuario paramedico, Asignacion asignacionVigente) {
		Ambulancia ambulancia = asignacionVigente == null ? null : asignacionVigente.getAmbulancia();
		return new ServicioActualResponse(
				ParamedicoResponse.de(paramedico, asignacionVigente),
				ambulancia == null ? null : AmbulanciaResponse.de(ambulancia),
				ambulancia != null && ambulancia.isActiva());
	}

}
