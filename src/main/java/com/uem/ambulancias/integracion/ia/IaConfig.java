package com.uem.ambulancias.integracion.ia;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "sga.ia", name = "habilitado", havingValue = "true")
public class IaConfig {

	/** Encendido sin URL o sin token, el servidor no arranca: mejor eso que trabajos que fallan en silencio. */
	@Bean
	ClienteIaHttp clienteIa(IaProperties propiedades) {
		if (propiedades.url() == null || propiedades.url().isBlank()) {
			throw new IllegalStateException("sga.ia.url es obligatoria con sga.ia.habilitado=true.");
		}
		if (propiedades.token() == null || propiedades.token().isBlank()) {
			throw new IllegalStateException("sga.ia.token es obligatorio con sga.ia.habilitado=true.");
		}
		return new ClienteIaHttp(propiedades);
	}

}
