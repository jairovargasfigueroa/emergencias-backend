package com.uem.ambulancias.integracion.almacen;

import com.uem.ambulancias.evidencias.service.AlmacenDeEvidencias;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Mientras no haya adaptador del almacén, las evidencias se rechazan con un error claro. */
@Configuration
public class SinAlmacenConfig {

	@Bean
	AlmacenDeEvidencias almacenNoConfigurado() {
		return new AlmacenNoConfigurado();
	}

}
