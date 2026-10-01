package com.uem.ambulancias.seguridad.service;

import java.time.Instant;

import com.uem.ambulancias.usuarios.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Un token bien firmado y sin vencer igual deja de valer si las sesiones de su dueño se cerraron después de emitirlo:
 * así un teléfono perdido queda afuera en el acto, y no cuando se le termine la sesión, que dura meses. El cliente
 * recibe un 401, igual que con un token vencido, y la app vuelve a la pantalla de entrada.
 */
@Component
@RequiredArgsConstructor
public class ValidadorDeSesionVigente implements OAuth2TokenValidator<Jwt> {

	private static final OAuth2Error SESION_CERRADA = new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN,
			"Esta sesión se cerró. Hay que volver a entrar.", null);

	private final UsuarioRepository usuarios;

	@Override
	public OAuth2TokenValidatorResult validate(Jwt token) {
		Instant emitido = token.getIssuedAt();
		// El token guarda el instante recortado a segundos: uno del mismo segundo del cierre cuenta como anterior. Uno
		// nuevo nunca cae ahí, porque para tenerlo hay que activar la app con el código.
		boolean cerrada = usuarios.buscarSesionesCerradasEn(Long.valueOf(token.getSubject()))
				.map(cerradasEn -> emitido == null || emitido.isBefore(cerradasEn))
				.orElse(false);
		return cerrada ? OAuth2TokenValidatorResult.failure(SESION_CERRADA) : OAuth2TokenValidatorResult.success();
	}

}
