package com.uem.ambulancias.flota.controller;

import com.uem.ambulancias.comun.web.UsuarioActual;
import com.uem.ambulancias.flota.dto.RegistrarDispositivoRequest;
import com.uem.ambulancias.flota.dto.ServicioActualResponse;
import com.uem.ambulancias.flota.service.ParamedicoConAsignacion;
import com.uem.ambulancias.flota.service.ServicioParamedicoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lo que la app del paramédico consulta de su propio servicio. Entrar es cosa de /auth.
 */
@RestController
@RequestMapping("/paramedicos")
@RequiredArgsConstructor
public class ServicioParamedicoController {

	private final ServicioParamedicoService servicioParamedicoService;

	@GetMapping("/actual")
	public ServicioActualResponse servicioActual(@UsuarioActual Long paramedicoId) {
		ParamedicoConAsignacion servicio = servicioParamedicoService.servicioActual(paramedicoId);
		return ServicioActualResponse.de(servicio.paramedico(), servicio.asignacionVigente());
	}

	@PostMapping("/actual/dispositivo")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void registrarDispositivo(@UsuarioActual Long paramedicoId,
			@Valid @RequestBody RegistrarDispositivoRequest request) {
		servicioParamedicoService.registrarDispositivo(paramedicoId, request.tokenPush());
	}

}
