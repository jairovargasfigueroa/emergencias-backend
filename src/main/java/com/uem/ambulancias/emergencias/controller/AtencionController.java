package com.uem.ambulancias.emergencias.controller;

import com.uem.ambulancias.comun.geo.Geo;
import com.uem.ambulancias.comun.web.Cabeceras;
import com.uem.ambulancias.comun.web.Textos;
import com.uem.ambulancias.emergencias.dto.AtencionResponse;
import com.uem.ambulancias.emergencias.dto.CancelarAtencionRequest;
import com.uem.ambulancias.emergencias.dto.DatosPacienteRequest;
import com.uem.ambulancias.emergencias.dto.EntregaRequest;
import com.uem.ambulancias.emergencias.dto.RecogidaRequest;
import com.uem.ambulancias.emergencias.dto.UbicacionRequest;
import com.uem.ambulancias.emergencias.service.AtencionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * PB-05: la app del paramédico marca los hitos de su atención, la cancela o edita los datos del paciente.
 */
@RestController
@RequiredArgsConstructor
public class AtencionController {

	private final AtencionService atencionService;

	/** 204 si la ambulancia del paramédico no tiene una atención activa. */
	@GetMapping("/paramedicos/actual/atencion")
	public ResponseEntity<AtencionResponse> atencionActiva(@RequestHeader(Cabeceras.USUARIO_ID) Long paramedicoId) {
		return atencionService.atencionActiva(paramedicoId)
				.map(atencion -> ResponseEntity.ok(AtencionResponse.de(atencion)))
				.orElseGet(() -> ResponseEntity.noContent().build());
	}

	@PostMapping("/atenciones/{id}/llegada")
	public AtencionResponse marcarLlegada(@PathVariable Long id, @RequestHeader(Cabeceras.USUARIO_ID) Long paramedicoId,
			@Valid @RequestBody UbicacionRequest request) {
		return AtencionResponse.de(atencionService.marcarLlegada(id, paramedicoId,
				Geo.punto(request.latitud(), request.longitud())));
	}

	@PostMapping("/atenciones/{id}/recogida")
	public AtencionResponse marcarRecogida(@PathVariable Long id,
			@RequestHeader(Cabeceras.USUARIO_ID) Long paramedicoId, @Valid @RequestBody RecogidaRequest request) {
		return AtencionResponse.de(atencionService.marcarRecogida(id, paramedicoId,
				Geo.punto(request.latitud(), request.longitud()), Textos.opcional(request.nombrePaciente()),
				Textos.opcional(request.documentoPaciente())));
	}

	@PostMapping("/atenciones/{id}/entrega")
	public AtencionResponse entregar(@PathVariable Long id, @RequestHeader(Cabeceras.USUARIO_ID) Long paramedicoId,
			@Valid @RequestBody EntregaRequest request) {
		return AtencionResponse.de(atencionService.entregar(id, paramedicoId,
				Geo.punto(request.latitud(), request.longitud()), request.centroSaludId(),
				Textos.opcional(request.destinoDescripcion())));
	}

	@PostMapping("/atenciones/{id}/cancelar")
	public AtencionResponse cancelar(@PathVariable Long id, @RequestHeader(Cabeceras.USUARIO_ID) Long paramedicoId,
			@Valid @RequestBody CancelarAtencionRequest request) {
		return AtencionResponse.de(atencionService.cancelar(id, paramedicoId, request.motivo()));
	}

	@PostMapping("/atenciones/{id}/paciente")
	public AtencionResponse actualizarPaciente(@PathVariable Long id,
			@RequestHeader(Cabeceras.USUARIO_ID) Long paramedicoId, @Valid @RequestBody DatosPacienteRequest request) {
		return AtencionResponse.de(atencionService.actualizarPaciente(id, paramedicoId,
				Textos.opcional(request.nombrePaciente()), Textos.opcional(request.documentoPaciente())));
	}

}
