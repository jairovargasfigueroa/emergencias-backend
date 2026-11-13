package com.uem.ambulancias.cobros.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.uem.ambulancias.cobros.dto.RegistrarTarifaRequest;
import com.uem.ambulancias.cobros.dto.TarifasResponse;
import com.uem.ambulancias.cobros.service.TarifaService;
import com.uem.ambulancias.comun.web.UsuarioActual;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** PB-29: tarifas de referencia. Solo la central. */
@RestController
@RequestMapping("/tarifas")
@RequiredArgsConstructor
public class TarifaController {

	private final TarifaService tarifaService;

	@GetMapping
	public TarifasResponse consultar() {
		return tarifaService.consultar();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public TarifasResponse registrar(@UsuarioActual Long administradorId,
			@Valid @RequestBody RegistrarTarifaRequest request) {
		return tarifaService.registrar(administradorId, request);
	}

}
