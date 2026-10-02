package com.uem.ambulancias.evidencias.controller;

import com.uem.ambulancias.comun.web.UsuarioActual;
import com.uem.ambulancias.evidencias.dto.RegistrarEvidenciaRequest;
import com.uem.ambulancias.evidencias.dto.SubidaEvidenciaResponse;
import com.uem.ambulancias.evidencias.service.EvidenciaService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Las fotos, audios y videos de una alerta. La app pide dónde subir, sube directo al almacén y avisa que terminó.
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

	/** Otra URL para la misma evidencia, si la anterior venció antes de terminar la subida. */
	@PostMapping("/evidencias/{evidenciaId}/url-subida")
	public SubidaEvidenciaResponse firmarDeNuevo(@PathVariable Long evidenciaId, @UsuarioActual Long usuarioId) {
		return SubidaEvidenciaResponse.de(evidenciaService.firmarDeNuevo(evidenciaId, usuarioId));
	}

}
