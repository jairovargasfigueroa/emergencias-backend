package com.uem.ambulancias.evidencias.controller;

import com.uem.ambulancias.comun.web.RolActual;
import com.uem.ambulancias.comun.web.UsuarioActual;
import com.uem.ambulancias.evidencias.dto.ResumenIncidenteResponse;
import com.uem.ambulancias.evidencias.service.ResumenIncidenteService;
import com.uem.ambulancias.usuarios.domain.RolUsuario;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * El resumen preliminar que arma la IA con las alertas y las evidencias de un incidente. Lo consultan el panel y la
 * app del paramédico cuando Firebase les avisa que hay una versión nueva. El paramédico, solo el del incidente que
 * está atendiendo.
 */
@RestController
@RequiredArgsConstructor
public class ResumenIncidenteController {

	private final ResumenIncidenteService resumenService;

	@GetMapping("/incidentes/{id}/resumen")
	public ResumenIncidenteResponse resumen(@PathVariable("id") Long incidenteId, @UsuarioActual Long usuarioId,
			@RolActual RolUsuario rol) {
		return ResumenIncidenteResponse.de(incidenteId, resumenService.consultar(incidenteId, usuarioId, rol));
	}

}
