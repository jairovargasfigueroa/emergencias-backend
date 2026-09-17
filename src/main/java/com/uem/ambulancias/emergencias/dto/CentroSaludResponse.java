package com.uem.ambulancias.emergencias.dto;

import com.uem.ambulancias.emergencias.domain.CentroSalud;

public record CentroSaludResponse(Long id, String nombre, String direccion) {

	public static CentroSaludResponse de(CentroSalud centroSalud) {
		return new CentroSaludResponse(centroSalud.getId(), centroSalud.getNombre(), centroSalud.getDireccion());
	}

}
