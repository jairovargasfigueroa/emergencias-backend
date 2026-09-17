package com.uem.ambulancias.flota.controller;

import com.uem.ambulancias.comun.web.Cabeceras;
import com.uem.ambulancias.flota.dto.RegistrarPosicionRequest;
import com.uem.ambulancias.flota.service.PosicionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/posiciones")
@RequiredArgsConstructor
public class PosicionController {

	private final PosicionService posicionService;

	/** La app del paramédico la envía periódicamente mientras está en servicio. */
	@PostMapping
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void registrar(@RequestHeader(Cabeceras.USUARIO_ID) Long paramedicoId,
			@Valid @RequestBody RegistrarPosicionRequest request) {
		posicionService.registrar(paramedicoId, request.latitud(), request.longitud());
	}

}
