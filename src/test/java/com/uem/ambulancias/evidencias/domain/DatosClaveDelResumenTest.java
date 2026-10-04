package com.uem.ambulancias.evidencias.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;

class DatosClaveDelResumenTest {

	private static final DatosClaveDelResumen BASE = new DatosClaveDelResumen("moderate", "traffic_accident", 1, 2,
			Set.of("traffic"));

	@Test
	void unCambioDeRedaccionNoEsImportante() {
		DatosClaveDelResumen igual = new DatosClaveDelResumen("moderate", "traffic_accident", 1, 2, Set.of("traffic"));
		assertThat(igual.cambioImportanteRespectoDe(BASE)).isFalse();
	}

	@Test
	void unPeligroNuevoEsImportante() {
		DatosClaveDelResumen conHumo = new DatosClaveDelResumen("moderate", "traffic_accident", 1, 2,
				Set.of("traffic", "smoke"));
		assertThat(conHumo.cambioImportanteRespectoDe(BASE)).isTrue();
	}

	@Test
	void masPersonasOMasGravedadEsImportante() {
		assertThat(new DatosClaveDelResumen("moderate", "traffic_accident", 1, 3, Set.of("traffic"))
				.cambioImportanteRespectoDe(BASE)).isTrue();
		assertThat(new DatosClaveDelResumen("high", "traffic_accident", 1, 2, Set.of("traffic"))
				.cambioImportanteRespectoDe(BASE)).isTrue();
	}

	@Test
	void otroTipoDeSucesoEsImportante() {
		assertThat(new DatosClaveDelResumen("moderate", "fire", 1, 2, Set.of("traffic")).cambioImportanteRespectoDe(BASE))
				.isTrue();
	}

}
