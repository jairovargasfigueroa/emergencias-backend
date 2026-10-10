package com.uem.ambulancias.emergencias.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Point;

import com.uem.ambulancias.comun.geo.Geo;
import com.uem.ambulancias.emergencias.domain.Horario;
import com.uem.ambulancias.emergencias.domain.ModoHorario;
import com.uem.ambulancias.flota.domain.TipoUnidad;

/** Con los valores de producción: 15 min de acercamiento, 15 de recogida, 20 de margen y una hora para lo inmediato. */
@Tag("PB-25")
class EstimadorDeTiemposTest {

	static final TrasladoProperties CONFIG = new TrasladoProperties(1.4, 25, 15, 15, 20, 60, 120, 5, 15, 19,
			TipoUnidad.II, TipoUnidad.II, TipoUnidad.III, "America/La_Paz");

	private static final Point CASA = Geo.punto(-17.78329, -63.18210);
	private static final Point CLINICA = Geo.punto(-17.76529, -63.18210);
	private static final Instant CITA = Instant.parse("2026-10-11T14:00:00Z");

	private final EstimadorDeTiempos estimador = new EstimadorDeTiempos(CONFIG);

	@Test
	@DisplayName("PB-25 · el viaje se estima por calle: la distancia en línea recta por 1,4, a 25 km/h")
	void minutosDeViaje() {
		double kilometrosPorCalle = Geo.metrosEntre(CASA, CLINICA) / 1000 * 1.4;

		assertThat(estimador.minutosDeViaje(CASA, CLINICA)).isEqualTo(Math.round(kilometrosPorCalle / 25 * 60));
		assertThat(estimador.minutosDeViaje(CASA, CASA)).isZero();
	}

	@Test
	@DisplayName("PB-25 · con cita, la salida se calcula hacia atrás: viaje, recogida, acercamiento y un margen")
	void conCita() {
		long viaje = estimador.minutosDeViaje(CASA, CLINICA);

		Horario horario = estimador.paraCita(CASA, CLINICA, CITA);

		Instant limite = CITA.minus(Duration.ofMinutes(15 + 15 + viaje));
		assertThat(horario.modo()).isEqualTo(ModoHorario.PROGRAMADO);
		assertThat(horario.horaCita()).isEqualTo(CITA);
		assertThat(horario.limiteSalida()).isEqualTo(limite);
		assertThat(horario.salidaEstimada()).isEqualTo(limite.minus(Duration.ofMinutes(20)));
		assertThat(horario.recogidaDesde()).isEqualTo(horario.salidaEstimada().plus(Duration.ofMinutes(15)));
		assertThat(horario.recogidaHasta()).isEqualTo(limite.plus(Duration.ofMinutes(15)));
	}

	@Test
	@DisplayName("PB-25 · sin cita es para ahora: sale ya y tiene una hora para conseguir unidad")
	void paraAhora() {
		Instant ahora = Instant.parse("2026-10-10T15:00:00Z");

		Horario horario = estimador.paraAhora(ahora);

		assertThat(horario.modo()).isEqualTo(ModoHorario.INMEDIATO);
		assertThat(horario.horaCita()).isNull();
		assertThat(horario.salidaEstimada()).isEqualTo(ahora);
		assertThat(horario.limiteSalida()).isEqualTo(ahora.plus(Duration.ofMinutes(60)));
		assertThat(horario.recogidaDesde()).isEqualTo(ahora.plus(Duration.ofMinutes(15)));
		assertThat(horario.recogidaHasta()).isEqualTo(ahora.plus(Duration.ofMinutes(75)));
	}

}
