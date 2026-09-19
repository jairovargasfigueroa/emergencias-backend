package com.uem.ambulancias.flota.dto;

import com.uem.ambulancias.flota.domain.Ambulancia;
import com.uem.ambulancias.flota.domain.Asignacion;
import com.uem.ambulancias.flota.domain.Turno;
import com.uem.ambulancias.usuarios.domain.Usuario;

/**
 * {@code ambulancia} es la de la asignación vigente ({@code null} si no tiene). {@code enServicio} dice que puede
 * trabajar: tiene asignación vigente a una ambulancia activa. {@code turno} dice si está trabajando ahora mismo, que
 * es otra cosa: {@code null} significa que todavía no entró.
 */
public record ServicioActualResponse(ParamedicoResponse paramedico, AmbulanciaResponse ambulancia, boolean enServicio,
		TurnoResponse turno) {

	public static ServicioActualResponse de(Usuario paramedico, Asignacion asignacionVigente, Turno turnoAbierto) {
		Ambulancia ambulancia = asignacionVigente == null ? null : asignacionVigente.getAmbulancia();
		return new ServicioActualResponse(
				ParamedicoResponse.de(paramedico, asignacionVigente),
				ambulancia == null ? null : AmbulanciaResponse.de(ambulancia),
				ambulancia != null && ambulancia.isActiva(),
				turnoAbierto == null ? null : TurnoResponse.de(turnoAbierto));
	}

}
