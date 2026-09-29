package com.uem.ambulancias.usuarios.controller;

import java.util.List;

import com.uem.ambulancias.comun.web.UsuarioActual;
import com.uem.ambulancias.usuarios.dto.PersonaResponse;
import com.uem.ambulancias.usuarios.dto.RegistrarPersonaRequest;
import com.uem.ambulancias.usuarios.service.CiudadanoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * La agenda del ciudadano: familiares a los que traslada y contactos de confianza. Son personas, no cuentas, y
 * varias pueden compartir el mismo teléfono porque suele ser el de quien las registra.
 */
@RestController
@RequestMapping("/personas")
@RequiredArgsConstructor
public class PersonaController {

	private final CiudadanoService ciudadanoService;

	@GetMapping
	public List<PersonaResponse> mias(@UsuarioActual Long ciudadanoId) {
		return ciudadanoService.personasDe(ciudadanoId).stream().map(PersonaResponse::de).toList();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public PersonaResponse registrar(@UsuarioActual Long ciudadanoId,
			@Valid @RequestBody RegistrarPersonaRequest request) {
		return PersonaResponse
				.de(ciudadanoService.registrarPersona(ciudadanoId, request.nombreCompleto(), request.telefono()));
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void olvidar(@PathVariable("id") Long personaId, @UsuarioActual Long ciudadanoId) {
		ciudadanoService.olvidarPersona(ciudadanoId, personaId);
	}

}
