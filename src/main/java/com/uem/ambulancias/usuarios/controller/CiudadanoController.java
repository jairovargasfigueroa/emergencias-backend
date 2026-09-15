package com.uem.ambulancias.usuarios.controller;

import com.uem.ambulancias.usuarios.dto.CiudadanoResponse;
import com.uem.ambulancias.usuarios.dto.RegistrarCiudadanoRequest;
import com.uem.ambulancias.usuarios.service.CiudadanoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ciudadanos")
@RequiredArgsConstructor
public class CiudadanoController {

	private final CiudadanoService ciudadanoService;

	/** Registro ligero: la app guarda el id devuelto y lo envía como emisor de sus alertas. */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public CiudadanoResponse registrar(@Valid @RequestBody RegistrarCiudadanoRequest request) {
		return CiudadanoResponse.de(ciudadanoService.registrar(request.nombreCompleto(), request.telefono()));
	}

}
