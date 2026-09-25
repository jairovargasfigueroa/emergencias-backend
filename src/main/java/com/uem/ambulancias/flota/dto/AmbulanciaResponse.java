package com.uem.ambulancias.flota.dto;

import com.uem.ambulancias.flota.domain.Ambulancia;
import com.uem.ambulancias.flota.domain.EstadoAmbulancia;
import com.uem.ambulancias.flota.domain.TipoUnidad;

public record AmbulanciaResponse(Long id, String placa, TipoUnidad tipoUnidad, EstadoAmbulancia estado,
		boolean activa) {

	public static AmbulanciaResponse de(Ambulancia ambulancia) {
		return new AmbulanciaResponse(ambulancia.getId(), ambulancia.getPlaca(), ambulancia.getTipoUnidad(),
				ambulancia.getEstado(), ambulancia.isActiva());
	}

}
