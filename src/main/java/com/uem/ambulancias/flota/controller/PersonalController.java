package com.uem.ambulancias.flota.controller;

import java.util.List;

import com.uem.ambulancias.flota.dto.AsignacionResponse;
import com.uem.ambulancias.flota.dto.ParamedicoResponse;
import com.uem.ambulancias.flota.dto.RegistrarParamedicoRequest;
import com.uem.ambulancias.flota.service.ParamedicoConAsignacion;
import com.uem.ambulancias.flota.service.PersonalService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Personal: no existe operación de borrado, solo baja lógica.
 */
@RestController
@RequestMapping("/paramedicos")
@RequiredArgsConstructor
public class PersonalController {

	private final PersonalService personalService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ParamedicoResponse registrar(@Valid @RequestBody RegistrarParamedicoRequest request) {
		return aRespuesta(personalService.registrarParamedico(request.nombreCompleto(), request.telefono()));
	}

	@GetMapping
	public List<ParamedicoResponse> listar() {
		return personalService.listarParamedicos().stream().map(PersonalController::aRespuesta).toList();
	}

	@PostMapping("/{id}/desactivar")
	public ParamedicoResponse desactivar(@PathVariable Long id) {
		return aRespuesta(personalService.desactivarParamedico(id));
	}

	@GetMapping("/{id}/asignaciones")
	public List<AsignacionResponse> historialDeAsignaciones(@PathVariable Long id) {
		return personalService.historialDeAsignaciones(id).stream().map(AsignacionResponse::de).toList();
	}

	private static ParamedicoResponse aRespuesta(ParamedicoConAsignacion personal) {
		return ParamedicoResponse.de(personal.paramedico(), personal.asignacionVigente());
	}

}
