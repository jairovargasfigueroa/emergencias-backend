package com.uem.ambulancias.emergencias.controller;

import com.uem.ambulancias.emergencias.dto.OperacionResponse;
import com.uem.ambulancias.emergencias.service.OperacionService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * El centro de control del administrador: toda la operación en una sola llamada. La pantalla se refresca sola, así
 * que la respuesta tiene que traer todo junto y no obligar a encadenar pedidos.
 */
@RestController
@RequestMapping("/operacion")
@RequiredArgsConstructor
public class OperacionController {

	private final OperacionService operacion;

	@GetMapping
	public OperacionResponse estadoActual() {
		return operacion.estadoActual();
	}

}
