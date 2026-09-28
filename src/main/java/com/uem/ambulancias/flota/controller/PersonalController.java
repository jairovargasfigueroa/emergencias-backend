package com.uem.ambulancias.flota.controller;

import java.util.List;
import java.util.Set;

import com.uem.ambulancias.flota.dto.AsignacionResponse;
import com.uem.ambulancias.flota.dto.EditarParamedicoRequest;
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
import org.springframework.web.bind.annotation.PutMapping;
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
		Set<Long> enTurno = personalService.paramedicosEnTurno();
		return personalService.listarParamedicos().stream()
				.map(personal -> ParamedicoResponse.de(personal.paramedico(), personal.asignacionVigente(),
						enTurno.contains(personal.paramedico().getId())))
				.toList();
	}

	@PutMapping("/{id}")
	public ParamedicoResponse editar(@PathVariable Long id, @Valid @RequestBody EditarParamedicoRequest request) {
		return aRespuesta(personalService.editarParamedico(id, request.nombreCompleto(), request.telefono()));
	}

	@PostMapping("/{id}/desactivar")
	public ParamedicoResponse desactivar(@PathVariable Long id) {
		return aRespuesta(personalService.desactivarParamedico(id));
	}

	@PostMapping("/{id}/activar")
	public ParamedicoResponse activar(@PathVariable Long id) {
		return aRespuesta(personalService.activarParamedico(id));
	}

	/** Lo deja sin ambulancia, sin ponerlo en otra. */
	@PostMapping("/{id}/quitar-asignacion")
	public ParamedicoResponse quitarDeLaUnidad(@PathVariable Long id) {
		return aRespuesta(personalService.quitarDeLaUnidad(id));
	}

	@GetMapping("/{id}/asignaciones")
	public List<AsignacionResponse> historialDeAsignaciones(@PathVariable Long id) {
		return personalService.historialDeAsignaciones(id).stream().map(AsignacionResponse::de).toList();
	}

	private ParamedicoResponse aRespuesta(ParamedicoConAsignacion personal) {
		return ParamedicoResponse.de(personal.paramedico(), personal.asignacionVigente(),
				personalService.estaEnTurno(personal.paramedico().getId()));
	}

}
