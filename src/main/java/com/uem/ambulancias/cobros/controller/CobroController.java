package com.uem.ambulancias.cobros.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.uem.ambulancias.cobros.dto.AjustarCobroRequest;
import com.uem.ambulancias.cobros.dto.AnularCobroRequest;
import com.uem.ambulancias.cobros.dto.CobroDeServicioResponse;
import com.uem.ambulancias.cobros.dto.RegistrarCobroRequest;
import com.uem.ambulancias.cobros.service.CobroService;
import com.uem.ambulancias.comun.web.UsuarioActual;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** PB-29: el cobro de cada servicio (una atención). Solo la central. */
@RestController
@RequestMapping("/cobros")
@RequiredArgsConstructor
public class CobroController {

	private final CobroService cobroService;

	@GetMapping("/servicios/{atencionId}")
	public CobroDeServicioResponse delServicio(@PathVariable Long atencionId) {
		return cobroService.delServicio(atencionId);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public CobroDeServicioResponse registrar(@UsuarioActual Long administradorId,
			@Valid @RequestBody RegistrarCobroRequest request) {
		return cobroService.registrar(administradorId, request);
	}

	@PutMapping("/{id}")
	public CobroDeServicioResponse ajustar(@PathVariable Long id, @UsuarioActual Long administradorId,
			@Valid @RequestBody AjustarCobroRequest request) {
		return cobroService.ajustar(id, administradorId, request);
	}

	@PostMapping("/{id}/anulacion")
	public CobroDeServicioResponse anular(@PathVariable Long id, @UsuarioActual Long administradorId,
			@Valid @RequestBody AnularCobroRequest request) {
		return cobroService.anular(id, administradorId, request.motivo());
	}

}
