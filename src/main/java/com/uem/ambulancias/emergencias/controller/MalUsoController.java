package com.uem.ambulancias.emergencias.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.uem.ambulancias.emergencias.dto.CiudadanoConFalsasAlarmasResponse;
import com.uem.ambulancias.emergencias.dto.DecisionSobreCuentaRequest;
import com.uem.ambulancias.emergencias.dto.HistorialDeCiudadanoResponse;
import com.uem.ambulancias.emergencias.service.ControlDeMalUso;
import com.uem.ambulancias.comun.web.UsuarioActual;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** PB-23: falsas alarmas por ciudadano y desactivación de cuentas. Solo para la central. */
@RestController
@RequestMapping("/ciudadanos")
@RequiredArgsConstructor
public class MalUsoController {

	private final ControlDeMalUso control;

	@GetMapping("/falsas-alarmas")
	public List<CiudadanoConFalsasAlarmasResponse> conFalsasAlarmas() {
		return control.ciudadanosConFalsasAlarmas();
	}

	@GetMapping("/{id}/historial")
	public HistorialDeCiudadanoResponse historial(@PathVariable Long id) {
		return control.historial(id);
	}

	@PostMapping("/{id}/desactivacion")
	public HistorialDeCiudadanoResponse desactivar(@PathVariable Long id, @UsuarioActual Long administradorId,
			@Valid @RequestBody DecisionSobreCuentaRequest request) {
		return control.desactivar(id, administradorId, request.motivo());
	}

	@PostMapping("/{id}/reactivacion")
	public HistorialDeCiudadanoResponse reactivar(@PathVariable Long id, @UsuarioActual Long administradorId,
			@Valid @RequestBody DecisionSobreCuentaRequest request) {
		return control.reactivar(id, administradorId, request.motivo());
	}

}
