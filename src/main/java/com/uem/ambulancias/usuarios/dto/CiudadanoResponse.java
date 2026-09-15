package com.uem.ambulancias.usuarios.dto;

import com.uem.ambulancias.usuarios.domain.Usuario;

public record CiudadanoResponse(Long id, String nombreCompleto, String telefono) {

	public static CiudadanoResponse de(Usuario ciudadano) {
		return new CiudadanoResponse(ciudadano.getId(), ciudadano.getNombreCompleto(), ciudadano.getTelefono());
	}

}
