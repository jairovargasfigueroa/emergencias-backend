package com.uem.ambulancias.emergencias.controller;

import java.util.List;

import com.uem.ambulancias.comun.geo.Geo;
import com.uem.ambulancias.comun.web.UsuarioActual;
import com.uem.ambulancias.comun.web.Textos;
import com.uem.ambulancias.emergencias.domain.Atencion;
import com.uem.ambulancias.emergencias.dto.AtencionResponse;
import com.uem.ambulancias.emergencias.dto.CancelarAtencionRequest;
import com.uem.ambulancias.emergencias.dto.DatosPacienteRequest;
import com.uem.ambulancias.emergencias.dto.EntregaRequest;
import com.uem.ambulancias.emergencias.dto.RecogidaRequest;
import com.uem.ambulancias.emergencias.dto.SinTrasladoRequest;
import com.uem.ambulancias.emergencias.dto.UbicacionRequest;
import com.uem.ambulancias.emergencias.dto.UnidadNoCorrespondeRequest;
import com.uem.ambulancias.emergencias.service.AtencionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * PB-05: la app del paramédico marca los hitos de su atención, la cancela o edita los datos del paciente.
 */
@RestController
@RequiredArgsConstructor
public class AtencionController {

	private final AtencionService atencionService;

	/** 204 si la ambulancia del paramédico no tiene ninguna atención que la ocupe. */
	@GetMapping("/paramedicos/actual/atencion")
	public ResponseEntity<AtencionResponse> atencionActiva(@UsuarioActual Long paramedicoId) {
		return atencionService.atencionActiva(paramedicoId)
				.map(atencion -> ResponseEntity.ok(respuesta(atencion)))
				.orElseGet(() -> ResponseEntity.noContent().build());
	}

	/**
	 * Toda respuesta lleva si los emisores retiraron su pedido: es lo que el paramédico necesita para decidir. En
	 * un traslado no aplica, porque no hay alertas que retirar: lo pidió una persona y ella misma lo cancela.
	 */
	private AtencionResponse respuesta(Atencion atencion) {
		boolean emisoresCancelaron = atencion.getIncidente() != null
				&& atencionService.emisoresCancelaron(atencion.getIncidente().getId());
		return AtencionResponse.de(atencion, emisoresCancelaron);
	}

	/** El historial de traslados del paramédico, del más reciente al más viejo. */
	@GetMapping("/paramedicos/actual/traslados")
	public List<AtencionResponse> misTraslados(@UsuarioActual Long paramedicoId) {
		return atencionService.trasladosDe(paramedicoId).stream().map(AtencionController::sinAvisoDeEmisores).toList();
	}

	/** En un traslado no hay alertas que retirar: lo pidió una persona y ella misma lo cancela. */
	private static AtencionResponse sinAvisoDeEmisores(Atencion atencion) {
		return AtencionResponse.de(atencion, false);
	}

	/** Solo en traslados: llegó y el paciente no estaba listo. Queda la hora, que es tiempo de unidad perdido. */
	@PostMapping("/atenciones/{id}/no-listo")
	public AtencionResponse marcarPacienteNoListo(@PathVariable Long id, @UsuarioActual Long paramedicoId) {
		return respuesta(atencionService.marcarPacienteNoListo(id, paramedicoId));
	}

	/** Solo en traslados: el paciente necesita más de lo que esta unidad puede dar. */
	@PostMapping("/atenciones/{id}/unidad-no-corresponde")
	public AtencionResponse unidadNoCorresponde(@PathVariable Long id, @UsuarioActual Long paramedicoId,
			@Valid @RequestBody UnidadNoCorrespondeRequest request) {
		return respuesta(atencionService.cerrarPorUnidadQueNoCorresponde(id, paramedicoId,
				Geo.punto(request.latitud(), request.longitud()), request.movilidad(), request.oxigeno(),
				request.equipo()));
	}

	@PostMapping("/atenciones/{id}/llegada")
	public AtencionResponse marcarLlegada(@PathVariable Long id, @UsuarioActual Long paramedicoId,
			@Valid @RequestBody UbicacionRequest request) {
		return respuesta(atencionService.marcarLlegada(id, paramedicoId,
				Geo.punto(request.latitud(), request.longitud())));
	}

	@PostMapping("/atenciones/{id}/recogida")
	public AtencionResponse marcarRecogida(@PathVariable Long id,
			@UsuarioActual Long paramedicoId, @Valid @RequestBody RecogidaRequest request) {
		return respuesta(atencionService.marcarRecogida(id, paramedicoId,
				Geo.punto(request.latitud(), request.longitud()), Textos.opcional(request.nombrePaciente()),
				Textos.opcional(request.documentoPaciente())));
	}

	@PostMapping("/atenciones/{id}/hospital")
	public AtencionResponse marcarLlegadaAlHospital(@PathVariable Long id, @UsuarioActual Long paramedicoId,
			@Valid @RequestBody UbicacionRequest request) {
		return respuesta(atencionService.marcarLlegadaAlHospital(id, paramedicoId,
				Geo.punto(request.latitud(), request.longitud())));
	}

	@PostMapping("/atenciones/{id}/entrega")
	public AtencionResponse entregar(@PathVariable Long id, @UsuarioActual Long paramedicoId,
			@Valid @RequestBody EntregaRequest request) {
		return respuesta(atencionService.entregar(id, paramedicoId,
				Geo.punto(request.latitud(), request.longitud()), request.centroSaludId(),
				Textos.opcional(request.destinoDescripcion())));
	}

	/** La salida que no trasladó a nadie. No es una cancelación: la unidad fue, resolvió y lo reporta. */
	@PostMapping("/atenciones/{id}/sin-traslado")
	public AtencionResponse cerrarSinTraslado(@PathVariable Long id, @UsuarioActual Long paramedicoId,
			@Valid @RequestBody SinTrasladoRequest request) {
		return respuesta(atencionService.cerrarSinTraslado(id, paramedicoId,
				Geo.punto(request.latitud(), request.longitud()), request.motivo()));
	}

	/** La unidad queda libre. Hasta acá sigue ocupada, aunque el paciente ya esté entregado. */
	@PostMapping("/atenciones/{id}/liberacion")
	public AtencionResponse liberar(@PathVariable Long id, @UsuarioActual Long paramedicoId) {
		return respuesta(atencionService.liberar(id, paramedicoId));
	}

	@PostMapping("/atenciones/{id}/cancelar")
	public AtencionResponse cancelar(@PathVariable Long id, @UsuarioActual Long paramedicoId,
			@Valid @RequestBody CancelarAtencionRequest request) {
		return respuesta(atencionService.cancelar(id, paramedicoId, request.motivo()));
	}

	@PostMapping("/atenciones/{id}/paciente")
	public AtencionResponse actualizarPaciente(@PathVariable Long id,
			@UsuarioActual Long paramedicoId, @Valid @RequestBody DatosPacienteRequest request) {
		return respuesta(atencionService.actualizarPaciente(id, paramedicoId,
				Textos.opcional(request.nombrePaciente()), Textos.opcional(request.documentoPaciente())));
	}

}
