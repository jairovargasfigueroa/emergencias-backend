package com.uem.ambulancias.emergencias.domain;

import static com.uem.ambulancias.soporte.Conflictos.rechazaCon;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.uem.ambulancias.comun.error.CodigoError;
import com.uem.ambulancias.comun.geo.Geo;

@Tag("PB-05")
class IncidenteTest {

	private static final Instant AHORA = Instant.parse("2026-10-09T15:00:00Z");

	@Test
	@DisplayName("PB-05 · un incidente nace activo, con los afectados de su primera alerta")
	void nuevo() {
		Incidente incidente = Incidente.crear(Geo.punto(-17.78329, -63.18210), 2, AHORA);

		assertThat(incidente.getEstado()).isEqualTo(EstadoIncidente.ACTIVO);
		assertThat(incidente.getCantidadAfectados()).isEqualTo(2);
		assertThat(incidente.getFechaHoraCierre()).isNull();
	}

	@Test
	@DisplayName("PB-05 · los afectados se consolidan con el máximo reportado, nunca con la suma")
	void consolidarAfectados() {
		Incidente incidente = Incidente.crear(Geo.punto(-17.78329, -63.18210), 3, AHORA);

		incidente.consolidarAfectados(2);
		assertThat(incidente.getCantidadAfectados()).isEqualTo(3);

		incidente.consolidarAfectados(null);
		assertThat(incidente.getCantidadAfectados()).isEqualTo(3);

		incidente.consolidarAfectados(5);
		assertThat(incidente.getCantidadAfectados()).isEqualTo(5);
	}

	@Test
	@DisplayName("PB-05 · sin afectados en la primera alerta, el primero que llega es el que vale")
	void afectadosDesdeCero() {
		Incidente incidente = Incidente.crear(Geo.punto(-17.78329, -63.18210), null, AHORA);

		incidente.consolidarAfectados(1);

		assertThat(incidente.getCantidadAfectados()).isEqualTo(1);
	}

	@Test
	@DisplayName("PB-05 · al pasar a un estado final queda la hora del cierre, y de ahí no sale")
	void estadoFinal() {
		Incidente incidente = Incidente.crear(Geo.punto(-17.78329, -63.18210), null, AHORA);

		incidente.cambiarEstado(EstadoIncidente.CANCELADO);

		assertThat(incidente.getEstado().isAbierto()).isFalse();
		assertThat(incidente.getFechaHoraCierre()).isNotNull();
		rechazaCon(CodigoError.TRANSICION_INVALIDA, () -> incidente.cambiarEstado(EstadoIncidente.ACTIVO));
	}

	@Test
	@DisplayName("PB-05 · un incidente activo no pasa a atendido sin que una unidad lo haya tomado")
	void atendidoSinUnidad() {
		Incidente incidente = Incidente.crear(Geo.punto(-17.78329, -63.18210), null, AHORA);

		rechazaCon(CodigoError.TRANSICION_INVALIDA, () -> incidente.cambiarEstado(EstadoIncidente.ATENDIDO));
		assertThat(incidente.getEstado()).isEqualTo(EstadoIncidente.ACTIVO);
	}

}
