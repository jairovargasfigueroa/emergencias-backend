package com.uem.ambulancias.flota.controller;

import java.util.List;
import java.util.Map;

import com.uem.ambulancias.comun.web.UsuarioActual;
import com.uem.ambulancias.flota.domain.Ambulancia;
import com.uem.ambulancias.flota.dto.AmbulanciaResponse;
import com.uem.ambulancias.flota.dto.EditarAmbulanciaRequest;
import com.uem.ambulancias.flota.dto.AsignacionResponse;
import com.uem.ambulancias.flota.dto.RegistrarAmbulanciaRequest;
import com.uem.ambulancias.flota.service.AmbulanciaService;

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
 * Flota: no existe operación de borrado, solo baja lógica.
 */
@RestController
@RequestMapping("/ambulancias")
@RequiredArgsConstructor
public class AmbulanciaController {

	private final AmbulanciaService ambulanciaService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public AmbulanciaResponse registrar(@Valid @RequestBody RegistrarAmbulanciaRequest request) {
		return respuesta(ambulanciaService.registrar(request.placa(), request.tipoUnidad()));
	}

	@GetMapping
	public List<AmbulanciaResponse> listar() {
		Map<Long, Long> tripulantes = ambulanciaService.tripulantesEnTurno();
		return ambulanciaService.listar().stream()
				.map(ambulancia -> AmbulanciaResponse.de(ambulancia, tripulantes.getOrDefault(ambulancia.getId(), 0L)))
				.toList();
	}

	@PostMapping("/{id}/fuera-de-servicio")
	public AmbulanciaResponse marcarFueraDeServicio(@PathVariable Long id) {
		return respuesta(ambulanciaService.marcarFueraDeServicio(id));
	}

	@PutMapping("/{id}")
	public AmbulanciaResponse editar(@PathVariable Long id, @Valid @RequestBody EditarAmbulanciaRequest request) {
		return respuesta(ambulanciaService.editar(id, request.placa(), request.tipoUnidad()));
	}

	/** La pide el administrador, o el paramédico de esa misma unidad: por eso hace falta saber quién llama. */
	@PostMapping("/{id}/reactivar")
	public AmbulanciaResponse reactivar(@PathVariable Long id, @UsuarioActual Long usuarioId) {
		return respuesta(ambulanciaService.reactivar(id, usuarioId));
	}

	@PostMapping("/{id}/activar")
	public AmbulanciaResponse activar(@PathVariable Long id) {
		return respuesta(ambulanciaService.activar(id));
	}

	@PostMapping("/{id}/desactivar")
	public AmbulanciaResponse desactivar(@PathVariable Long id) {
		return respuesta(ambulanciaService.desactivar(id));
	}

	@GetMapping("/{id}/asignaciones")
	public List<AsignacionResponse> historialDeAsignaciones(@PathVariable Long id) {
		return ambulanciaService.historialDeAsignaciones(id).stream().map(AsignacionResponse::de).toList();
	}

	private AmbulanciaResponse respuesta(Ambulancia ambulancia) {
		return AmbulanciaResponse.de(ambulancia, ambulanciaService.tripulantesEnTurno(ambulancia.getId()));
	}

}
