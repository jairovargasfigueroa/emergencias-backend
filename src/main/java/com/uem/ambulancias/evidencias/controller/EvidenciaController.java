package com.uem.ambulancias.evidencias.controller;

import com.uem.ambulancias.comun.web.RolActual;
import com.uem.ambulancias.comun.web.UsuarioActual;
import com.uem.ambulancias.evidencias.dto.EvidenciaResponse;
import com.uem.ambulancias.evidencias.dto.LecturaEvidenciaResponse;
import com.uem.ambulancias.evidencias.dto.RegistrarEvidenciaRequest;
import com.uem.ambulancias.evidencias.dto.SubidaEvidenciaResponse;
import com.uem.ambulancias.evidencias.service.EvidenciaService;
import com.uem.ambulancias.usuarios.domain.RolUsuario;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Las fotos, audios y videos de una alerta. La app pide dónde subir, sube directo al almacén y avisa que terminó. El
 * personal los ve con una URL temporal, también directo del almacén.
 */
@RestController
@RequiredArgsConstructor
public class EvidenciaController {

	private final EvidenciaService evidenciaService;

	/** El ciudadano adjunta un archivo a su alerta: se registra y se devuelve la URL firmada para subirlo. */
	@PostMapping("/alertas/{alertaId}/evidencias")
	@ResponseStatus(HttpStatus.CREATED)
	public SubidaEvidenciaResponse registrar(@PathVariable Long alertaId, @UsuarioActual Long usuarioId,
			@Valid @RequestBody RegistrarEvidenciaRequest request) {
		return SubidaEvidenciaResponse.de(evidenciaService.registrar(alertaId, usuarioId, request.mimeType(),
				request.tamanoBytes(), request.sha256()));
	}

	/**
	 * La app avisa que terminó de subir. 202 porque el análisis viene después y por su lado: la respuesta solo dice
	 * que el archivo llegó bien.
	 */
	@PostMapping("/evidencias/{evidenciaId}/confirmacion")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public EvidenciaResponse confirmar(@PathVariable Long evidenciaId, @UsuarioActual Long usuarioId) {
		return EvidenciaResponse.de(evidenciaService.confirmar(evidenciaId, usuarioId));
	}

	/** Otra URL para la misma evidencia, si la anterior venció antes de terminar la subida. */
	@PostMapping("/evidencias/{evidenciaId}/url-subida")
	public SubidaEvidenciaResponse firmarDeNuevo(@PathVariable Long evidenciaId, @UsuarioActual Long usuarioId) {
		return SubidaEvidenciaResponse.de(evidenciaService.firmarDeNuevo(evidenciaId, usuarioId));
	}

	/**
	 * URL temporal para ver o escuchar el archivo desde el panel o desde la app del paramédico que atiende el
	 * incidente.
	 */
	@GetMapping("/evidencias/{evidenciaId}/url")
	public LecturaEvidenciaResponse url(@PathVariable Long evidenciaId, @UsuarioActual Long usuarioId,
			@RolActual RolUsuario rol) {
		return LecturaEvidenciaResponse.de(evidenciaService.firmarLectura(evidenciaId, usuarioId, rol));
	}

}
