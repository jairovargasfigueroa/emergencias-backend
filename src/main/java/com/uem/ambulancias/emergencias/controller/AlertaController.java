package com.uem.ambulancias.emergencias.controller;

import java.time.Instant;

import com.uem.ambulancias.comun.geo.Geo;
import com.uem.ambulancias.comun.web.UsuarioActual;
import com.uem.ambulancias.comun.web.Textos;
import com.uem.ambulancias.emergencias.domain.Alerta;
import com.uem.ambulancias.emergencias.domain.Incidente;
import com.uem.ambulancias.emergencias.dto.AlertaResponse;
import com.uem.ambulancias.emergencias.dto.CancelarAlertaRequest;
import com.uem.ambulancias.emergencias.dto.CrearAlertaRequest;
import com.uem.ambulancias.emergencias.dto.DetallesAlertaRequest;
import com.uem.ambulancias.emergencias.service.IncidenteService;
import com.uem.ambulancias.usuarios.domain.Usuario;
import com.uem.ambulancias.usuarios.service.CiudadanoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
	public AlertaResponse crearAlerta(@UsuarioActual Long usuarioId,
			@Valid @RequestBody CrearAlertaRequest request) {
		Usuario emisor = ciudadanoService.buscarCiudadanoActivo(usuarioId);
		Alerta alerta = Alerta.emitir(emisor, Geo.punto(request.latitud(), request.longitud()),
				request.origenUbicacion(), request.cantidadAfectados(), Textos.opcional(request.descripcion()),
				Instant.now());
		Incidente incidente = incidenteService.agruparAlerta(alerta);
		return AlertaResponse.de(alerta, incidente);
	}

	/**
	 * Los datos opcionales (PB-02 R3) también se pueden completar después de emitir, mientras el ciudadano espera:
	 * solo se aplican los que llegan y la descripción se normaliza igual que al crear.
	 */
	@PostMapping("/{alertaId}/detalles")
	public AlertaResponse completarDetalles(@PathVariable Long alertaId,
			@UsuarioActual Long usuarioId,
			@Valid @RequestBody DetallesAlertaRequest request) {
		Alerta alerta = incidenteService.completarDetalles(alertaId, usuarioId, request.cantidadAfectados(),
				Textos.opcional(request.descripcion()));
		return AlertaResponse.de(alerta, alerta.getIncidente());
	}

	/**
	 * El ciudadano retira su pedido, hasta que una unidad llegue al lugar. No siempre cierra el incidente: si otro
	 * también pidió, o si ya hay una unidad en camino, el incidente sigue y lo resuelve quien corresponde.
	 */
	@PostMapping("/{alertaId}/cancelacion")
	public AlertaResponse cancelar(@PathVariable Long alertaId, @UsuarioActual Long usuarioId,
			@Valid @RequestBody CancelarAlertaRequest request) {
		Alerta alerta = incidenteService.cancelarAlerta(alertaId, usuarioId, request.motivo(),
				request.emisorEsPaciente());
		return AlertaResponse.de(alerta, alerta.getIncidente());
	}

}
