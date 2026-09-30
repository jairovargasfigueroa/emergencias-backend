package com.uem.ambulancias.seguridad.controller;

import com.uem.ambulancias.comun.web.UsuarioActual;
import com.uem.ambulancias.seguridad.dto.RenovacionSesionRequest;
import com.uem.ambulancias.seguridad.dto.SesionResponse;
import com.uem.ambulancias.seguridad.service.AutenticacionService;
import com.uem.ambulancias.seguridad.service.TokenService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * La sesión de las apps, una vez abierta. A diferencia de {@code /auth}, acá se llega con el token: se renueva el que
 * se tiene, no se entra de cero.
 */
@RestController
@RequestMapping("/sesion")
@RequiredArgsConstructor
public class SesionController {

	private final AutenticacionService autenticacionService;

	/**
	 * Cambia el token por uno nuevo antes de que venza. El ciudadano no manda cuerpo; el paramédico manda la clave de
	 * su teléfono. Un 401 quiere decir que ya no se puede renovar: la app cierra la sesión.
	 */
	@PostMapping("/renovacion")
	public SesionResponse.Renovada renovar(@UsuarioActual Long usuarioId,
			@RequestBody(required = false) RenovacionSesionRequest request) {
		TokenService.TokenEmitido token = autenticacionService.renovarSesion(usuarioId,
				request == null ? null : request.claveDispositivo());
		return new SesionResponse.Renovada(token.token(), token.venceEn());
	}

}
