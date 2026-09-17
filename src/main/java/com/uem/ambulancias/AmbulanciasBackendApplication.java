package com.uem.ambulancias;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AmbulanciasBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(AmbulanciasBackendApplication.class, args);
	}

}
