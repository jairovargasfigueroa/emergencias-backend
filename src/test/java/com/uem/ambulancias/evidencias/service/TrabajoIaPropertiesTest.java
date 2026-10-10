package com.uem.ambulancias.evidencias.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("PB-19")
class TrabajoIaPropertiesTest {

	@Test
	@DisplayName("PB-19 · cada reintento espera el doble que el anterior, desde 30 s y sin pasar de 15 minutos")
	void esperaCreciente() {
		TrabajoIaProperties config = new TrabajoIaProperties(5, 30, 900, 6);

		assertThat(config.esperaTras(1)).isEqualTo(Duration.ofSeconds(30));
		assertThat(config.esperaTras(2)).isEqualTo(Duration.ofSeconds(60));
		assertThat(config.esperaTras(3)).isEqualTo(Duration.ofSeconds(120));
		assertThat(config.esperaTras(5)).isEqualTo(Duration.ofSeconds(480));
		assertThat(config.esperaTras(6)).isEqualTo(Duration.ofSeconds(900));
		assertThat(config.esperaTras(40)).isEqualTo(Duration.ofSeconds(900));
	}

}
