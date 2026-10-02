package com.uem.ambulancias.integracion.almacen;

import com.uem.ambulancias.evidencias.service.AlmacenDeEvidencias;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Sin bucket configurado, las evidencias se rechazan con un error claro y el servidor arranca igual. */
@Configuration
@ConditionalOnExpression("'${sga.evidencias.s3.bucket:}'.isBlank()")
public class SinAlmacenConfig {

	@Bean
	AlmacenDeEvidencias almacenNoConfigurado() {
		return new AlmacenNoConfigurado();
	}

}
