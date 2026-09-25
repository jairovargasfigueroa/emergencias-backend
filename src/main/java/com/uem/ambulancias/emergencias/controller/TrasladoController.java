package com.uem.ambulancias.emergencias.controller;

import java.util.List;

import com.uem.ambulancias.comun.web.UsuarioActual;
import com.uem.ambulancias.emergencias.dto.DetallesTrasladoRequest;
import com.uem.ambulancias.emergencias.dto.RegistrarTrasladoRequest;
import com.uem.ambulancias.emergencias.dto.TrasladoResponse;
import com.uem.ambulancias.emergencias.service.TrasladoService;

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
 * Traslados del ciudadano. Pedir no reserva unidad: el traslado queda agendado y la ambulancia se busca recién
 * cuando llega la hora de salir.
 */
@RestController
@RequestMapping("/traslados")
@RequiredArgsConstructor
public class TrasladoController {

	private final TrasladoService trasladoService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public TrasladoResponse registrar(@UsuarioActual Long ciudadanoId,
			@Valid @RequestBody RegistrarTrasladoRequest request) {
		return TrasladoResponse.de(trasladoService.registrar(ciudadanoId, request));
	}

	@GetMapping("/mios")
	public List<TrasladoResponse> mios(@UsuarioActual Long ciudadanoId) {
		return trasladoService.mios(ciudadanoId).stream().map(TrasladoResponse::de).toList();
	}

	/** Cambiar el pedido entero. Solo se acepta mientras no haya una unidad en camino. */
	@PutMapping("/{id}")
	public TrasladoResponse reprogramar(@PathVariable("id") Long trasladoId, @UsuarioActual Long ciudadanoId,
			@Valid @RequestBody RegistrarTrasladoRequest request) {
		return TrasladoResponse.de(trasladoService.reprogramar(ciudadanoId, trasladoId, request));
	}

	/** Corregir la referencia, el contacto y las observaciones, incluso con la unidad ya en camino. */
	@PutMapping("/{id}/detalles")
	public TrasladoResponse actualizarDetalles(@PathVariable("id") Long trasladoId, @UsuarioActual Long ciudadanoId,
			@Valid @RequestBody DetallesTrasladoRequest request) {
		return TrasladoResponse.de(trasladoService.actualizarDetalles(ciudadanoId, trasladoId,
				request.origenReferencia(), request.contactoNombre(), request.contactoTelefono(),
				request.observaciones()));
	}

	@PostMapping("/{id}/cancelar")
	public TrasladoResponse cancelar(@PathVariable("id") Long trasladoId, @UsuarioActual Long ciudadanoId) {
		return TrasladoResponse.de(trasladoService.cancelar(ciudadanoId, trasladoId));
	}

}
