package com.uem.ambulancias.emergencias.controller;

import java.util.List;

import com.uem.ambulancias.emergencias.dto.CentroSaludResponse;
import com.uem.ambulancias.emergencias.service.CentroSaludService;

import com.uem.ambulancias.emergencias.dto.GuardarCentroSaludRequest;

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

@RestController
@RequestMapping("/centros-salud")
@RequiredArgsConstructor
public class CentroSaludController {

	private final CentroSaludService centroSaludService;

	@GetMapping
	public List<CentroSaludResponse> listarActivos() {
		return centroSaludService.listarActivos().stream().map(CentroSaludResponse::de).toList();
	}


	/** Para la central: todos los centros, también los dados de baja. */
	@GetMapping("/todos")
	public List<CentroSaludResponse> listarTodos() {
		return centroSaludService.listarTodos().stream().map(CentroSaludResponse::de).toList();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public CentroSaludResponse registrar(@Valid @RequestBody GuardarCentroSaludRequest request) {
		return CentroSaludResponse.de(centroSaludService.registrar(request));
	}

	@PutMapping("/{id}")
	public CentroSaludResponse corregir(@PathVariable Long id, @Valid @RequestBody GuardarCentroSaludRequest request) {
		return CentroSaludResponse.de(centroSaludService.corregir(id, request));
	}

	@PostMapping("/{id}/desactivar")
	public CentroSaludResponse desactivar(@PathVariable Long id) {
		return CentroSaludResponse.de(centroSaludService.desactivar(id));
	}

	@PostMapping("/{id}/activar")
	public CentroSaludResponse activar(@PathVariable Long id) {
		return CentroSaludResponse.de(centroSaludService.activar(id));
	}

}
