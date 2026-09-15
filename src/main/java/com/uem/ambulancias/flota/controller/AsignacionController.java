package com.uem.ambulancias.flota.controller;

import com.uem.ambulancias.comun.error.ManejadorErrores;
import com.uem.ambulancias.flota.dto.AsignacionResponse;
import com.uem.ambulancias.flota.dto.AsignacionVigenteResponse;
import com.uem.ambulancias.flota.dto.AsignarParamedicoRequest;
import com.uem.ambulancias.flota.exception.ReasignacionRequiereConfirmacionException;
import com.uem.ambulancias.flota.service.AsignacionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/asignaciones")
@RequiredArgsConstructor
public class AsignacionController {

	private final AsignacionService asignacionService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public AsignacionResponse asignar(@Valid @RequestBody AsignarParamedicoRequest request) {
		return AsignacionResponse.de(asignacionService.asignar(request.paramedicoId(), request.ambulanciaId(),
				request.reasignacionConfirmada()));
	}

	/** 409 con la asignación vigente, para que el panel pida confirmar la reasignación. */
	@ExceptionHandler(ReasignacionRequiereConfirmacionException.class)
	ProblemDetail reasignacionRequiereConfirmacion(ReasignacionRequiereConfirmacionException e) {
		ProblemDetail problema = ManejadorErrores.problema(HttpStatus.CONFLICT, e.getCodigo(), e.getMessage());
		problema.setProperty("asignacionVigente", AsignacionVigenteResponse.de(e.getAsignacionVigente()));
		return problema;
	}

}
