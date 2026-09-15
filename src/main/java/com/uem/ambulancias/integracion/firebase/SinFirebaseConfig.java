package com.uem.ambulancias.integracion.firebase;

import com.uem.ambulancias.emergencias.service.PublicadorTiempoReal;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "sga.firebase", name = "habilitado", havingValue = "false", matchIfMissing = true)
public class SinFirebaseConfig {

	@Bean
	PublicadorTiempoReal publicadorEnRegistro() {
		return new PublicadorEnRegistro();
	}

}
