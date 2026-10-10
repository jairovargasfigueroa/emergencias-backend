package com.uem.ambulancias.soporte;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * La base de las pruebas de integración: un PostGIS en Docker, la misma imagen que producción. Spring lo arranca una
 * sola vez y lo comparten todas las pruebas que usan el mismo contexto; al terminar la corrida se borra solo.
 */
@TestConfiguration(proxyBeanMethods = false)
public class PostgisDePrueba {

	static final DockerImageName IMAGEN = DockerImageName.parse("postgis/postgis:17-3.5")
			.asCompatibleSubstituteFor("postgres");

	@Bean
	@ServiceConnection
	PostgreSQLContainer postgis() {
		return new PostgreSQLContainer(IMAGEN);
	}

}
