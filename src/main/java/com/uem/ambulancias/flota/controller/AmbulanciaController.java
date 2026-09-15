package com.uem.ambulancias.flota.controller;

import java.util.List;

import com.uem.ambulancias.flota.dto.AmbulanciaResponse;
import com.uem.ambulancias.flota.dto.AsignacionResponse;
import com.uem.ambulancias.flota.dto.RegistrarAmbulanciaRequest;
import com.uem.ambulancias.flota.service.AmbulanciaService;

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
 * Flota: no existe operación de borrado, solo baja lógica.
 */
@RestController
@RequestMapping("/ambulancias")
@RequiredArgsConstructor
public class AmbulanciaController {

	private final AmbulanciaService ambulanciaService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public AmbulanciaResponse registrar(@Valid @RequestBody RegistrarAmbulanciaRequest request) {
		return AmbulanciaResponse.de(ambulanciaService.registrar(request.placa(), request.tipoUnidad()));
	}

	@GetMapping
	public List<AmbulanciaResponse> listar() {
		return ambulanciaService.listar().stream().map(AmbulanciaResponse::de).toList();
	}

	@PostMapping("/{id}/fuera-de-servicio")
	public AmbulanciaResponse marcarFueraDeServicio(@PathVariable Long id) {
		return AmbulanciaResponse.de(ambulanciaService.marcarFueraDeServicio(id));
	}

	@PostMapping("/{id}/reactivar")
	public AmbulanciaResponse reactivar(@PathVariable Long id) {
		return AmbulanciaResponse.de(ambulanciaService.reactivar(id));
	}

	@PostMapping("/{id}/desactivar")
	public AmbulanciaResponse desactivar(@PathVariable Long id) {
		return AmbulanciaResponse.de(ambulanciaService.desactivar(id));
	}

	@GetMapping("/{id}/asignaciones")
	public List<AsignacionResponse> historialDeAsignaciones(@PathVariable Long id) {
		return ambulanciaService.historialDeAsignaciones(id).stream().map(AsignacionResponse::de).toList();
	}

}
