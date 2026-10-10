package com.uem.ambulancias.soporte;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import com.uem.ambulancias.seguridad.service.VerificadorDeTelefono;

/**
 * Las piezas falsas que reemplazan a lo externo durante las pruebas: Firebase, el almacén S3 y el servicio de IA.
 * Ganan sobre las reales por ser primarias.
 */
@TestConfiguration(proxyBeanMethods = false)
public class DoblesDePrueba {

	@Bean
	@Primary
	PublicacionesAnotadas publicacionesAnotadas() {
		return new PublicacionesAnotadas();
	}

	@Bean
	@Primary
	VerificadorDeTelefono verificadorDeTelefonoDePrueba() {
		return new VerificadorDeTelefonoFalso();
	}

	@Bean
	@Primary
	AlmacenEnMemoria almacenEnMemoria() {
		return new AlmacenEnMemoria();
	}

	@Bean
	@Primary
	ServicioIaFalso servicioIaFalso() {
		return new ServicioIaFalso();
	}

}
