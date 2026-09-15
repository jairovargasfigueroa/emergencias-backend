package com.uem.ambulancias.flota.dto;

import com.uem.ambulancias.flota.domain.Ambulancia;
import com.uem.ambulancias.flota.domain.EstadoAmbulancia;

public record AmbulanciaResponse(Long id, String placa, String tipoUnidad, EstadoAmbulancia estado, boolean activa) {

	public static AmbulanciaResponse de(Ambulancia ambulancia) {
		return new AmbulanciaResponse(ambulancia.getId(), ambulancia.getPlaca(), ambulancia.getTipoUnidad(),
				ambulancia.getEstado(), ambulancia.isActiva());
	}

}
