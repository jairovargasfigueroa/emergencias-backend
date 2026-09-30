package com.uem.ambulancias.flota.controller;

import com.uem.ambulancias.comun.web.UsuarioActual;
import com.uem.ambulancias.flota.dto.IniciarTurnoRequest;
import com.uem.ambulancias.flota.dto.RegistrarDispositivoRequest;
import com.uem.ambulancias.flota.dto.ServicioActualResponse;
import com.uem.ambulancias.flota.dto.TurnoResponse;
import com.uem.ambulancias.flota.service.ParamedicoConAsignacion;
import com.uem.ambulancias.flota.service.ServicioParamedicoService;
import com.uem.ambulancias.flota.service.TurnoService;
import com.uem.ambulancias.seguridad.service.AccesoParamedicoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lo que la app del paramédico consulta de su propio servicio. Entrar es cosa de /auth.
 */
@RestController
@RequestMapping("/paramedicos")
@RequiredArgsConstructor
public class ServicioParamedicoController {

	private final ServicioParamedicoService servicioParamedicoService;
	private final TurnoService turnoService;
	private final AccesoParamedicoService accesoParamedico;

	@GetMapping("/actual")
	public ServicioActualResponse servicioActual(@UsuarioActual Long paramedicoId) {
		ParamedicoConAsignacion servicio = servicioParamedicoService.servicioActual(paramedicoId);
		long tripulantes = servicio.asignacionVigente() == null ? 0
				: turnoService.tripulantesEnTurno(servicio.asignacionVigente().getAmbulancia().getId());
		return ServicioActualResponse.de(servicio.paramedico(), servicio.asignacionVigente(),
				turnoService.turnoAbierto(paramedicoId).orElse(null), tripulantes);
	}

	/**
	 * Entra a trabajar: su unidad pasa a contar como disponible y empieza a compartir su posición. Antes confirma su
	 * PIN desde el teléfono vinculado. La verificación va en su propia transacción: un PIN equivocado tiene que
	 * quedar contado aunque el turno no se abra.
	 */
	@PostMapping("/actual/turno/inicio")
	@ResponseStatus(HttpStatus.CREATED)
	public TurnoResponse iniciarTurno(@UsuarioActual Long paramedicoId,
			@Valid @RequestBody IniciarTurnoRequest request) {
		accesoParamedico.verificarPin(paramedicoId, request.pin(), request.claveDispositivo());
		return TurnoResponse.de(turnoService.iniciar(paramedicoId));
	}

	/** Sale de trabajar. Se rechaza con una atención en curso: nadie se va dejando un caso abierto. */
	@PostMapping("/actual/turno/cierre")
	public TurnoResponse terminarTurno(@UsuarioActual Long paramedicoId) {
		return TurnoResponse.de(turnoService.terminar(paramedicoId));
	}

	@PostMapping("/actual/dispositivo")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void registrarDispositivo(@UsuarioActual Long paramedicoId,
			@Valid @RequestBody RegistrarDispositivoRequest request) {
		servicioParamedicoService.registrarDispositivo(paramedicoId, request.tokenPush());
	}

}
