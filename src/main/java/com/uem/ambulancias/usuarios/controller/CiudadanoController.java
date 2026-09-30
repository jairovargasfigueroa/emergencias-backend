package com.uem.ambulancias.usuarios.controller;

import com.uem.ambulancias.comun.web.UsuarioActual;
import com.uem.ambulancias.usuarios.dto.DispositivoRequest;
import com.uem.ambulancias.usuarios.service.CiudadanoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lo que la app del ciudadano dice de sí misma. Por ahora, a qué teléfono mandarle los avisos de su alerta y de sus
 * traslados.
 */
@RestController
@RequestMapping("/ciudadanos/actual")
@RequiredArgsConstructor
public class CiudadanoController {

	private final CiudadanoService ciudadanoService;

	/** Un teléfono nuevo reemplaza al anterior: los avisos van a donde la persona usa la app ahora. */
	@PostMapping("/dispositivo")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void registrarDispositivo(@UsuarioActual Long ciudadanoId, @Valid @RequestBody DispositivoRequest request) {
		ciudadanoService.registrarDispositivo(ciudadanoId, request.tokenPush());
	}

	/** Al cerrar sesión: el teléfono deja de recibir los avisos de esta persona. */
	@DeleteMapping("/dispositivo")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void quitarDispositivo(@UsuarioActual Long ciudadanoId) {
		ciudadanoService.quitarDispositivo(ciudadanoId);
	}

}
