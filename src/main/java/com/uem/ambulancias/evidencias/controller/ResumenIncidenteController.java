package com.uem.ambulancias.evidencias.controller;

import com.uem.ambulancias.evidencias.dto.ResumenIncidenteResponse;
import com.uem.ambulancias.evidencias.service.ResumenConEvidencias;
import com.uem.ambulancias.evidencias.service.ResumenIncidenteService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * El resumen preliminar que arma la IA con las alertas y las evidencias de un incidente. Lo consultan el panel y la
 * app del paramédico cuando Firebase les avisa que hay una versión nueva.
 */
@RestController
@RequiredArgsConstructor
public class ResumenIncidenteController {

	private final ResumenIncidenteService resumenService;

	@GetMapping("/incidentes/{id}/resumen")
	public ResumenIncidenteResponse resumen(@PathVariable("id") Long incidenteId) {
		ResumenConEvidencias consulta = resumenService.consultar(incidenteId);
		return ResumenIncidenteResponse.de(incidenteId, consulta.resumen(), consulta.evidencias());
	}

}
