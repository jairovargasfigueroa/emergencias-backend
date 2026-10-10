package com.uem.ambulancias.evidencias.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.uem.ambulancias.comun.geo.Geo;
import com.uem.ambulancias.emergencias.domain.Incidente;
import com.uem.ambulancias.evidencias.service.Procedencia;
import com.uem.ambulancias.evidencias.service.ResumenRecibido;

@Tag("PB-19")
class ResumenIncidenteTest {

	private static final Instant AHORA = Instant.parse("2026-10-10T15:00:00Z");
	private static final Incidente INCIDENTE = Incidente.crear(Geo.punto(-17.78329, -63.18210), null, AHORA);

	@Test
	@DisplayName("PB-19 · el primer resumen es la versión 1 y cada uno nuevo suma una, con sus fuentes y su procedencia")
	void versiones() {
		ResumenIncidente primero = version(null, List.of(10L), List.of(1L));
		ResumenIncidente segundo = version(primero, List.of(10L, 11L), List.of(1L));

		assertThat(primero.getVersion()).isEqualTo(1);
		assertThat(segundo.getVersion()).isEqualTo(2);
		assertThat(segundo.getEvidenciasUsadas()).containsExactly(10L, 11L);
		assertThat(segundo.getAlertasUsadas()).containsExactly(1L);
		assertThat(segundo.getModelo()).isEqualTo("modelo-de-prueba");
		assertThat(segundo.getGeneradoEn()).isEqualTo(AHORA);
	}

	@Test
	@DisplayName("PB-19 · un resumen nuevo reemplaza al vigente solo si suma una evidencia o una alerta")
	void soloSiSuma() {
		ResumenIncidente vigente = version(null, List.of(10L), List.of(1L));

		assertThat(vigente.seReemplazaCon(List.of(10L, 11L), List.of(1L))).isTrue();
		assertThat(vigente.seReemplazaCon(List.of(10L), List.of(1L, 2L))).isTrue();
		assertThat(vigente.seReemplazaCon(List.of(10L), List.of(1L))).isFalse();
	}

	@Test
	@DisplayName("PB-19 · un resumen que deja afuera una evidencia ya usada no reemplaza al vigente")
	void noPierdeFuentes() {
		ResumenIncidente vigente = version(null, List.of(10L, 11L), List.of(1L));

		assertThat(vigente.seReemplazaCon(List.of(11L, 12L), List.of(1L, 2L))).isFalse();
	}

	private static ResumenIncidente version(ResumenIncidente vigente, List<Long> evidencias, List<Long> alertas) {
		ResumenRecibido recibido = new ResumenRecibido("{\"eventType\": \"CAIDA\"}", evidencias, alertas,
				new Procedencia("prueba", "modelo-de-prueba", "prompt-1", AHORA, "LLM"));
		return ResumenIncidente.nuevaVersion(INCIDENTE, vigente, UUID.randomUUID(), recibido, AHORA);
	}

}
