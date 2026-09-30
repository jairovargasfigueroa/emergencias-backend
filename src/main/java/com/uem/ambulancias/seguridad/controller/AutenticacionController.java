package com.uem.ambulancias.seguridad.controller;

import com.uem.ambulancias.flota.dto.ParamedicoResponse;
import com.uem.ambulancias.flota.service.ParamedicoConAsignacion;
import com.uem.ambulancias.flota.service.TurnoService;
import com.uem.ambulancias.seguridad.dto.ActivacionParamedicoRequest;
import com.uem.ambulancias.seguridad.dto.IngresoAdminRequest;
import com.uem.ambulancias.seguridad.dto.IngresoParamedicoRequest;
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
	private final TurnoService turnoService;

	/** Panel del administrador. */
	@PostMapping("/admin")
	public SesionResponse.Admin admin(@Valid @RequestBody IngresoAdminRequest request) {
		AutenticacionService.SesionAdmin sesion = autenticacionService.ingresarAdmin(request.correo(), request.clave());
		return SesionResponse.Admin.de(sesion.token(), sesion.admin());
	}

	/** App del paramédico: su teléfono, su PIN y la clave del teléfono vinculado. */
	@PostMapping("/paramedico")
	public SesionResponse.Paramedico paramedico(@Valid @RequestBody IngresoParamedicoRequest request) {
		AutenticacionService.SesionParamedico sesion = autenticacionService.ingresarParamedico(request.telefono(),
				request.pin(), request.claveDispositivo());
		return new SesionResponse.Paramedico(sesion.token(), aRespuesta(sesion.identificado()));
	}

	/**
	 * App del paramédico, primera vez en un teléfono: el código que le entregó la central y el PIN que crea. La
	 * respuesta trae la clave del teléfono, que no se vuelve a mostrar.
	 */
	@PostMapping("/paramedico/activacion")
	public SesionResponse.ParamedicoActivado activarParamedico(
			@Valid @RequestBody ActivacionParamedicoRequest request) {
		AutenticacionService.SesionParamedicoActivado sesion = autenticacionService
				.activarParamedico(request.telefono(), request.codigo(), request.pin());
		return new SesionResponse.ParamedicoActivado(sesion.token(), aRespuesta(sesion.identificado()),
				sesion.claveDispositivo());
	}

	/** App del ciudadano: el registro ligero (PB-02 R1) es su entrada; repetirlo devuelve el mismo usuario. */
	@PostMapping("/ciudadano")
	@ResponseStatus(HttpStatus.CREATED)
	public SesionResponse.Ciudadano ciudadano(@Valid @RequestBody RegistrarCiudadanoRequest request) {
		AutenticacionService.SesionCiudadano sesion =
				autenticacionService.registrarCiudadano(request.nombreCompleto(), request.telefono());
		return new SesionResponse.Ciudadano(sesion.token(), CiudadanoResponse.de(sesion.ciudadano()));
	}

	private ParamedicoResponse aRespuesta(ParamedicoConAsignacion identificado) {
		return ParamedicoResponse.de(identificado.paramedico(), identificado.asignacionVigente(),
				turnoService.turnoAbierto(identificado.paramedico().getId()).isPresent());
	}

}
