package com.uem.ambulancias;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/** {@code @EnableScheduling} es por el barrido de traslados: es lo único del sistema que corre sin que nadie pida nada. */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class AmbulanciasBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(AmbulanciasBackendApplication.class, args);
	}

}
