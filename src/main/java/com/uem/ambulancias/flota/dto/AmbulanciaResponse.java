package com.uem.ambulancias.flota.dto;

import com.uem.ambulancias.flota.domain.Ambulancia;
import com.uem.ambulancias.flota.domain.EstadoAmbulancia;
import com.uem.ambulancias.flota.domain.TipoUnidad;

public record AmbulanciaResponse(Long id, String placa, TipoUnidad tipoUnidad, EstadoAmbulancia estado,
		boolean activa,
		/** Cuántos paramédicos tienen turno abierto en ella. Con alguien adentro no se la puede desactivar. */
		long tripulantesEnTurno) {

	public static AmbulanciaResponse de(Ambulancia ambulancia, long tripulantesEnTurno) {
		return new AmbulanciaResponse(ambulancia.getId(), ambulancia.getPlaca(), ambulancia.getTipoUnidad(),
				ambulancia.getEstado(), ambulancia.isActiva(), tripulantesEnTurno);
	}

}
