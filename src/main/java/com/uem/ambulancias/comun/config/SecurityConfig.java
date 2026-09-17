package com.uem.ambulancias.comun.config;

import java.util.List;

import com.uem.ambulancias.comun.web.Cabeceras;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Seguridad abierta: todavía no hay autenticación, así que todos los endpoints quedan libres. CORS se configura en
 * esta cadena para que la consulta previa del navegador no la rechace la seguridad antes de llegar a la API.
 */
@Configuration
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		return http
				.csrf(AbstractHttpConfigurer::disable)
				.cors(Customizer.withDefaults())
				.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
				.build();
	}

	/** Solo los orígenes configurados pueden llamar a la API desde un navegador. */
	@Bean
	CorsConfigurationSource corsConfigurationSource(CorsProperties cors) {
		CorsConfiguration configuracion = new CorsConfiguration();
		configuracion.setAllowedOrigins(cors.origenesPermitidos());
		configuracion.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE"));
		configuracion.setAllowedHeaders(List.of("Content-Type", Cabeceras.USUARIO_ID));
		UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
		fuente.registerCorsConfiguration("/**", configuracion);
		return fuente;
	}

}
