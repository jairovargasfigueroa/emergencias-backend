package com.uem.ambulancias.flota.controller;

import com.uem.ambulancias.comun.web.Cabeceras;
import com.uem.ambulancias.flota.dto.IdentificarParamedicoRequest;
import com.uem.ambulancias.flota.dto.ParamedicoResponse;
import com.uem.ambulancias.flota.dto.ServicioActualResponse;
import com.uem.ambulancias.flota.service.ParamedicoConAsignacion;
import com.uem.ambulancias.flota.service.ServicioParamedicoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints que usa la app del paramédico para saber quién es y qué ambulancia opera.
 */
@RestController
@RequestMapping("/paramedicos")
@RequiredArgsConstructor
public class ServicioParamedicoController {

	private final ServicioParamedicoService servicioParamedicoService;

	/** Identificación provisional: la app guarda el id y lo envía en la cabecera X-Usuario-Id. */
	@PostMapping("/identificacion")
	public ParamedicoResponse identificar(@Valid @RequestBody IdentificarParamedicoRequest request) {
		ParamedicoConAsignacion identificado = servicioParamedicoService.identificar(request.telefono());
		return ParamedicoResponse.de(identificado.paramedico(), identificado.asignacionVigente());
	}

	@GetMapping("/actual")
	public ServicioActualResponse servicioActual(@RequestHeader(Cabeceras.USUARIO_ID) Long paramedicoId) {
		ParamedicoConAsignacion servicio = servicioParamedicoService.servicioActual(paramedicoId);
		return ServicioActualResponse.de(servicio.paramedico(), servicio.asignacionVigente());
	}

}
