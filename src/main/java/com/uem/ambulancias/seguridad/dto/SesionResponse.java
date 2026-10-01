package com.uem.ambulancias.seguridad.dto;

import java.time.Instant;

import com.uem.ambulancias.flota.dto.ParamedicoResponse;
import com.uem.ambulancias.usuarios.domain.Usuario;
import com.uem.ambulancias.usuarios.dto.CiudadanoResponse;

/**
 * Lo que recibe un cliente al entrar: el token que tiene que mandar en adelante y los datos que ya usaba antes, para
 * que no tenga que preguntarlos otra vez. Las apps reciben además cuándo vence el token, para renovarlo a tiempo.
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

	public record Paramedico(String token, Instant venceEn, ParamedicoResponse paramedico) {
	}

	/**
	 * Lo mismo que al entrar, más la clave del teléfono recién vinculado. Es la única vez que viaja: la app la guarda
	 * en el almacenamiento seguro del teléfono y el servidor se queda solo con su versión cifrada.
	 */
	public record ParamedicoActivado(String token, Instant venceEn, ParamedicoResponse paramedico,
			String claveDispositivo) {
	}

	public record Ciudadano(String token, Instant venceEn, CiudadanoResponse ciudadano) {
	}

	/** El token nuevo de una app, que reemplaza al que estaba por vencer. */
	public record Renovada(String token, Instant venceEn) {
	}

}
