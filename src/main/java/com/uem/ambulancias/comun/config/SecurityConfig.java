package com.uem.ambulancias.comun.config;

import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.uem.ambulancias.seguridad.service.SeguridadProperties;
import com.uem.ambulancias.seguridad.service.TokenService;
import com.uem.ambulancias.usuarios.domain.RolUsuario;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Quién puede llamar a qué. La identidad sale del token, nunca de un dato que el cliente escriba: lo único abierto
 * son las tres puertas de entrada de {@code /auth}.
 */
@Configuration
public class SecurityConfig {

	private static final String ADMIN = RolUsuario.ADMIN.name();
	private static final String PARAMEDICO = RolUsuario.PARAMEDICO.name();
	private static final String CIUDADANO = RolUsuario.CIUDADANO.name();

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationConverter convertidor) throws Exception {
		return http
				.csrf(AbstractHttpConfigurer::disable)
				.cors(Customizer.withDefaults())
				// Sin sesión en el servidor: cada petición se sostiene sola con su token.
				.sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(peticiones -> peticiones
						.requestMatchers("/auth/**").permitAll()
						// Lo que usa la app del paramédico. Va primero porque /paramedicos/** es del administrador.
						.requestMatchers("/paramedicos/actual/**").hasRole(PARAMEDICO)
						.requestMatchers("/atenciones/**").hasRole(PARAMEDICO)
						.requestMatchers(HttpMethod.POST, "/posiciones").hasRole(PARAMEDICO)
						.requestMatchers(HttpMethod.POST, "/incidentes/*/tomar", "/incidentes/*/sumarse").hasRole(PARAMEDICO)
						.requestMatchers(HttpMethod.GET, "/centros-salud").hasRole(PARAMEDICO)
						// ME-1 M5: la ambulancia vuelve de una avería desde la app (PB-05 R11) o desde el panel.
						.requestMatchers(HttpMethod.POST, "/ambulancias/*/reactivar").hasAnyRole(PARAMEDICO, ADMIN)
						// Lo que usa la app del ciudadano.
						.requestMatchers("/alertas/**").hasRole(CIUDADANO)
						// Lo que usa el panel.
						.requestMatchers(HttpMethod.GET, "/incidentes", "/incidentes/*").hasRole(ADMIN)
						.requestMatchers("/ambulancias/**", "/asignaciones/**", "/paramedicos/**").hasRole(ADMIN)
						.anyRequest().authenticated())
				.oauth2ResourceServer(recurso -> recurso.jwt(jwt -> jwt.jwtAuthenticationConverter(convertidor)))
				.build();
	}

	/** El rol viaja como un dato del token; acá se traduce a lo que entienden las reglas de arriba. */
	@Bean
	JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtGrantedAuthoritiesConverter permisos = new JwtGrantedAuthoritiesConverter();
		permisos.setAuthorityPrefix("ROLE_");
		permisos.setAuthoritiesClaimName(TokenService.CLAVE_ROL);
		JwtAuthenticationConverter convertidor = new JwtAuthenticationConverter();
		convertidor.setJwtGrantedAuthoritiesConverter(permisos);
		return convertidor;
	}

	/** La misma clave firma y verifica. Corta a menos de 32 caracteres, el algoritmo la rechaza. */
	@Bean
	SecretKey claveDeFirma(SeguridadProperties seguridad) {
		String secreto = seguridad.secreto();
		if (secreto == null || secreto.getBytes(StandardCharsets.UTF_8).length < 32) {
			throw new IllegalStateException("sga.seguridad.secreto es obligatorio y debe tener al menos 32 caracteres.");
		}
		return new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
	}

	@Bean
	JwtEncoder jwtEncoder(SecretKey claveDeFirma) {
		return new NimbusJwtEncoder(new ImmutableSecret<>(claveDeFirma));
	}

	@Bean
	JwtDecoder jwtDecoder(SecretKey claveDeFirma) {
		return NimbusJwtDecoder.withSecretKey(claveDeFirma).macAlgorithm(MacAlgorithm.HS256).build();
	}

	/** Las claves se guardan cifradas y nunca se pueden volver a leer, solo comparar. */
	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	/** Solo los orígenes configurados pueden llamar a la API desde un navegador. */
	@Bean
	CorsConfigurationSource corsConfigurationSource(CorsProperties cors) {
		CorsConfiguration configuracion = new CorsConfiguration();
		configuracion.setAllowedOrigins(cors.origenesPermitidos());
		configuracion.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE"));
		configuracion.setAllowedHeaders(List.of("Content-Type", "Authorization"));
		UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
		fuente.registerCorsConfiguration("/**", configuracion);
		return fuente;
	}

}
