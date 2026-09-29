package com.uem.ambulancias.emergencias.controller;

import com.uem.ambulancias.comun.web.Textos;
import com.uem.ambulancias.comun.web.UsuarioActual;
import com.uem.ambulancias.emergencias.dto.CierreDesdeLaCentralRequest;
import com.uem.ambulancias.emergencias.dto.OperacionResponse;
import com.uem.ambulancias.emergencias.service.AtencionService;
import com.uem.ambulancias.emergencias.service.OperacionService;

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
 * El centro de control del administrador: toda la operación en una sola llamada. La pantalla se refresca sola, así
 * que la respuesta tiene que traer todo junto y no obligar a encadenar pedidos.
 */
@RestController
@RequestMapping("/operacion")
@RequiredArgsConstructor
public class OperacionController {

	private final OperacionService operacion;
	private final AtencionService atencionService;

	@GetMapping
	public OperacionResponse estadoActual() {
		return operacion.estadoActual();
	}

	/**
	 * Cerrar una atención que la tripulación no puede cerrar. No devuelve nada: la pantalla vuelve a pedir la
	 * operación entera, que es donde se ve el cambio.
	 */
	@PostMapping("/atenciones/{id}/cierre")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void cerrarAtencion(@PathVariable Long id, @UsuarioActual Long administradorId,
			@Valid @RequestBody CierreDesdeLaCentralRequest request) {
		atencionService.cerrarDesdeLaCentral(id, administradorId, request.cierre(), request.dejarDisponible(),
				request.centroSaludId(), Textos.opcional(request.destinoDescripcion()));
	}

}
