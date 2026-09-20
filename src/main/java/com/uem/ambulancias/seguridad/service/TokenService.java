package com.uem.ambulancias.seguridad.service;

import java.time.Duration;
import java.time.Instant;

import com.uem.ambulancias.usuarios.domain.Usuario;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/**
 * Firma los tokens con los que los clientes llaman a la API. Dentro viajan quién es ({@code sub}) y su rol, que es
 * todo lo que la API necesita para decidir: nunca hay que volver a preguntarle a la base quién dice ser.
 */
@Service
@RequiredArgsConstructor
public class TokenService {

	/** Nombre del dato del token que lleva el rol. Las reglas de seguridad lo leen de acá. */
	public static final String CLAVE_ROL = "rol";

	private final JwtEncoder codificador;
	private final SeguridadProperties propiedades;

	/** Sesión del panel: corta, porque se abre en computadoras que no siempre son de una sola persona. */
	public String paraPanel(Usuario usuario) {
		return firmar(usuario, Duration.ofHours(propiedades.horasPanel()));
	}

	/** Sesión de una app: larga, para que el teléfono no pida credenciales en medio de una emergencia. */
	public String paraApp(Usuario usuario) {
		return firmar(usuario, Duration.ofDays(propiedades.diasApp()));
	}

	private String firmar(Usuario usuario, Duration duracion) {
		Instant ahora = Instant.now();
		JwtClaimsSet datos = JwtClaimsSet.builder()
				.issuer("sga")
				.issuedAt(ahora)
				.expiresAt(ahora.plus(duracion))
				.subject(String.valueOf(usuario.getId()))
				.claim(CLAVE_ROL, usuario.getRol().name())
				.claim("nombre", usuario.getNombreCompleto())
				.build();
		JwsHeader cabecera = JwsHeader.with(MacAlgorithm.HS256).build();
		return codificador.encode(JwtEncoderParameters.from(cabecera, datos)).getTokenValue();
	}

}
