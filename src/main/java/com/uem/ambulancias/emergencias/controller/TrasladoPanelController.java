package com.uem.ambulancias.emergencias.controller;

import java.time.LocalDate;
import java.util.List;

import com.uem.ambulancias.comun.web.UsuarioActual;
import com.uem.ambulancias.emergencias.domain.Atencion;
import com.uem.ambulancias.emergencias.dto.AsignarTrasladoRequest;
import com.uem.ambulancias.emergencias.dto.TrasladoDelPanelResponse;
import com.uem.ambulancias.emergencias.service.AsignadorDeTraslados;
import com.uem.ambulancias.emergencias.service.TrasladoConAtencion;
import com.uem.ambulancias.emergencias.service.TrasladoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Los traslados como los ve el administrador: la tabla del día, los que necesitan una decisión, y la asignación
 * a mano para cuando el sistema no encontró unidad.
 */
@RestController
@RequestMapping("/traslados")
@RequiredArgsConstructor
public class TrasladoPanelController {

	private final TrasladoService trasladoService;
	private final AsignadorDeTraslados asignador;

	/** Sin fecha devuelve el día de hoy, en la zona de la empresa. */
	@GetMapping
	public List<TrasladoDelPanelResponse> delDia(
			@RequestParam(name = "dia", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dia) {
		return trasladoService.delDia(dia).stream().map(TrasladoPanelController::respuesta).toList();
	}

	/** Los que siguen esperando unidad: es la bandeja donde el barrido deja lo que no pudo resolver solo. */
	@GetMapping("/problemas")
	public List<TrasladoDelPanelResponse> problemas() {
		return trasladoService.problemas().stream().map(TrasladoPanelController::respuesta).toList();
	}

	@PostMapping("/{id}/asignar")
	public TrasladoDelPanelResponse asignar(@PathVariable("id") Long trasladoId, @UsuarioActual Long administradorId,
			@Valid @RequestBody AsignarTrasladoRequest request) {
		Atencion atencion = asignador.asignarA(trasladoId, request.ambulanciaId(), administradorId);
		return TrasladoDelPanelResponse.de(atencion.getTraslado(), atencion);
	}

	private static TrasladoDelPanelResponse respuesta(TrasladoConAtencion fila) {
		return TrasladoDelPanelResponse.de(fila.traslado(), fila.atencion());
	}

}
