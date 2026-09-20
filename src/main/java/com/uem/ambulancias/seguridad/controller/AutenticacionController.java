package com.uem.ambulancias.seguridad.controller;

import com.uem.ambulancias.flota.dto.IdentificarParamedicoRequest;
import com.uem.ambulancias.flota.dto.ParamedicoResponse;
import com.uem.ambulancias.seguridad.dto.IngresoAdminRequest;
import com.uem.ambulancias.seguridad.dto.SesionResponse;
import com.uem.ambulancias.seguridad.service.AutenticacionService;
import com.uem.ambulancias.usuarios.dto.CiudadanoResponse;
import com.uem.ambulancias.usuarios.dto.RegistrarCiudadanoRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Las únicas rutas abiertas de la API: por acá se entra y se sale con un token. Todo lo demás lo exige.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AutenticacionController {

	private final AutenticacionService autenticacionService;

	/** Panel del administrador. */
	@PostMapping("/admin")
	public SesionResponse.Admin admin(@Valid @RequestBody IngresoAdminRequest request) {
		AutenticacionService.SesionAdmin sesion = autenticacionService.ingresarAdmin(request.correo(), request.clave());
		return SesionResponse.Admin.de(sesion.token(), sesion.admin());
	}

	/** App del paramédico: sigue entrando con su teléfono, ahora a cambio de un token. */
	@PostMapping("/paramedico")
	public SesionResponse.Paramedico paramedico(@Valid @RequestBody IdentificarParamedicoRequest request) {
		AutenticacionService.SesionParamedico sesion = autenticacionService.ingresarParamedico(request.telefono());
		return new SesionResponse.Paramedico(sesion.token(),
				ParamedicoResponse.de(sesion.identificado().paramedico(), sesion.identificado().asignacionVigente()));
	}

	/** App del ciudadano: el registro ligero (PB-02 R1) es su entrada; repetirlo devuelve el mismo usuario. */
	@PostMapping("/ciudadano")
	@ResponseStatus(HttpStatus.CREATED)
	public SesionResponse.Ciudadano ciudadano(@Valid @RequestBody RegistrarCiudadanoRequest request) {
		AutenticacionService.SesionCiudadano sesion =
				autenticacionService.registrarCiudadano(request.nombreCompleto(), request.telefono());
		return new SesionResponse.Ciudadano(sesion.token(), CiudadanoResponse.de(sesion.ciudadano()));
	}

}
