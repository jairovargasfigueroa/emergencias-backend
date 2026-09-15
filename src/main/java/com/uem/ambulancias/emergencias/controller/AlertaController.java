package com.uem.ambulancias.emergencias.controller;

import java.time.Instant;

import com.uem.ambulancias.comun.geo.Geo;
import com.uem.ambulancias.comun.web.Cabeceras;
import com.uem.ambulancias.emergencias.domain.Alerta;
import com.uem.ambulancias.emergencias.domain.Incidente;
import com.uem.ambulancias.emergencias.dto.AlertaResponse;
import com.uem.ambulancias.emergencias.dto.CrearAlertaRequest;
import com.uem.ambulancias.emergencias.service.IncidenteService;
import com.uem.ambulancias.usuarios.domain.Usuario;
import com.uem.ambulancias.usuarios.service.CiudadanoService;

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
@RequestMapping("/alertas")
@RequiredArgsConstructor
public class AlertaController {

	private final CiudadanoService ciudadanoService;
	private final IncidenteService incidenteService;

	/**
	 * SEC-A: construye la alerta del ciudadano emisor y delega la agrupación en {@link IncidenteService}. Se acepta
	 * igual con origen GPS o MANUAL.
	 */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public AlertaResponse crearAlerta(@RequestHeader(Cabeceras.USUARIO_ID) Long usuarioId,
			@Valid @RequestBody CrearAlertaRequest request) {
		Usuario emisor = ciudadanoService.buscarCiudadanoActivo(usuarioId);
		Alerta alerta = Alerta.emitir(emisor, Geo.punto(request.latitud(), request.longitud()),
				request.origenUbicacion(), request.cantidadAfectados(), textoOpcional(request.descripcion()),
				Instant.now());
		Incidente incidente = incidenteService.agruparAlerta(alerta);
		return AlertaResponse.de(alerta, incidente);
	}

	private static String textoOpcional(String texto) {
		return texto == null || texto.isBlank() ? null : texto.trim();
	}

}
