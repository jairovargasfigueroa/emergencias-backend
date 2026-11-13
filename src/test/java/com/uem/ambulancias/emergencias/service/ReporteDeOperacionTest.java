package com.uem.ambulancias.emergencias.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.uem.ambulancias.emergencias.dto.ReporteDeOperacionResponse.Medida;

@Tag("PB-22")
class ReporteDeOperacionTest {

	@Test
	@DisplayName("PB-22 · sin casos no hay promedio ni mediana: no se inventa un tiempo")
	void sinCasos() {
		assertThat(ReporteDeOperacion.medida(List.of())).isEqualTo(new Medida(0, null, null));
	}

	@Test
	@DisplayName("PB-22 · la mediana de una cantidad impar es el del medio, sin importar el orden")
	void medianaImpar() {
		assertThat(ReporteDeOperacion.medida(List.of(600L, 120L, 300L))).isEqualTo(new Medida(3, 340L, 300L));
	}

	@Test
	@DisplayName("PB-22 · la mediana de una cantidad par es el promedio de los dos del medio")
	void medianaPar() {
		assertThat(ReporteDeOperacion.medida(List.of(100L, 200L, 300L, 1000L))).isEqualTo(new Medida(4, 400L, 250L));
	}

	@Test
	@DisplayName("PB-22 · un caso que se aleja mucho mueve el promedio pero no la mediana")
	void valorExtremo() {
		Medida medida = ReporteDeOperacion.medida(List.of(300L, 320L, 340L, 7200L));
		assertThat(medida.medianaSegundos()).isEqualTo(330L);
		assertThat(medida.promedioSegundos()).isEqualTo(2040L);
	}
}
