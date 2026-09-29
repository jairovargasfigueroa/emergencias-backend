package com.uem.ambulancias.usuarios.dto;

import com.uem.ambulancias.usuarios.domain.Usuario;

/**
 * Una persona de la agenda del ciudadano. No es una cuenta: no inicia sesión ni tiene el número verificado.
 */
public record PersonaResponse(Long id, String nombreCompleto, String telefono) {

	public static PersonaResponse de(Usuario usuario) {
		return new PersonaResponse(usuario.getId(), usuario.getNombreCompleto(), usuario.getTelefono());
	}

}
