package com.uem.ambulancias.emergencias.dto;

import com.uem.ambulancias.flota.domain.Ambulancia;

public record UnidadAcudiendoResponse(Long ambulanciaId, String placa) {

	public static UnidadAcudiendoResponse de(Ambulancia ambulancia) {
		return new UnidadAcudiendoResponse(ambulancia.getId(), ambulancia.getPlaca());
	}

}
