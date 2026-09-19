package com.uem.ambulancias.seguridad.dto;

import com.uem.ambulancias.flota.dto.ParamedicoResponse;
import com.uem.ambulancias.usuarios.domain.Usuario;
import com.uem.ambulancias.usuarios.dto.CiudadanoResponse;

/**
 * Lo que recibe un cliente al entrar: el token que tiene que mandar en adelante y los datos que ya usaba antes, para
 * que no tenga que preguntarlos otra vez.
 */
public final class SesionResponse {

	private SesionResponse() {
	}

	/** El panel muestra el nombre de quien entró y no necesita nada más. */
	public record Admin(String token, Long id, String nombreCompleto, String correo) {
		public static Admin de(String token, Usuario admin) {
			return new Admin(token, admin.getId(), admin.getNombreCompleto(), admin.getCorreo());
		}
	}

	public record Paramedico(String token, ParamedicoResponse paramedico) {
	}

	public record Ciudadano(String token, CiudadanoResponse ciudadano) {
	}

}
