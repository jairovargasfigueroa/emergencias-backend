package com.uem.ambulancias.emergencias.domain;

import static com.uem.ambulancias.soporte.Conflictos.rechazaCon;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.uem.ambulancias.comun.error.CodigoError;
import com.uem.ambulancias.comun.geo.Geo;
import com.uem.ambulancias.flota.domain.TipoUnidad;
import com.uem.ambulancias.usuarios.domain.Usuario;

class TrasladoTest {

	private static final Instant AHORA = Instant.parse("2026-10-10T15:00:00Z");
	private static final Instant CITA = AHORA.plus(Duration.ofDays(1));

	@Test
	@Tag("PB-25")
	@DisplayName("PB-25 · con cita nace programado; para ahora, buscando unidad")
	void estadoInicial() {
		assertThat(programado().getEstado()).isEqualTo(EstadoTraslado.PROGRAMADO);
		assertThat(inmediato().getEstado()).isEqualTo(EstadoTraslado.BUSCANDO_UNIDAD);
	}

	@Test
	@Tag("PB-27")
	@DisplayName("PB-27 · programado, buscando unidad, asignado y completado, sin saltarse pasos")
	void caminoNormal() {
		Traslado traslado = programado();
		rechazaCon(CodigoError.TRANSICION_INVALIDA, traslado::asignar);

		traslado.empezarBusqueda();
		traslado.asignar();
		traslado.completar();

		assertThat(traslado.getEstado()).isEqualTo(EstadoTraslado.COMPLETADO);
		assertThat(traslado.getEstado().isVigente()).isFalse();
		rechazaCon(CodigoError.TRANSICION_INVALIDA, traslado::empezarBusqueda);
	}

	@Test
	@Tag("PB-25")
	@DisplayName("PB-25 · cancelado queda quién lo canceló y cuándo, y de ahí no sale")
	void cancelar() {
		Traslado traslado = programado();
		Usuario quien = traslado.getSolicitante();

		traslado.cancelar(quien, AHORA);

		assertThat(traslado.getEstado()).isEqualTo(EstadoTraslado.CANCELADO);
		assertThat(traslado.getCanceladoPor()).isSameAs(quien);
		assertThat(traslado.getFechaHoraCancelacion()).isEqualTo(AHORA);
		rechazaCon(CodigoError.TRANSICION_INVALIDA, () -> traslado.cancelar(quien, AHORA));
	}

	@Test
	@Tag("PB-25")
	@DisplayName("PB-25 · con unidad asignada ya no se reprograma: solo se corrigen referencia, contacto y observaciones")
	void reprogramarAsignado() {
		Traslado traslado = inmediato();
		traslado.asignar();

		rechazaCon(CodigoError.TRASLADO_FINALIZADO, () -> traslado.reprogramar(necesidades(), traslado.getOrigen(),
				null, null, null, null, traslado.getDestino(), null, horarioInmediato(AHORA), TipoUnidad.IA));
		traslado.actualizarDetalles("Portón verde", "Rosa", "70011122", "Usa bastón");
		assertThat(traslado.getOrigenReferencia()).isEqualTo("Portón verde");
	}

	@Test
	@Tag("PB-27")
	@DisplayName("PB-27 · al devolverse a búsqueda tarde, la ventana se corre para que haya tiempo de conseguir otra unidad")
	void devolverCorreLaVentana() {
		Traslado traslado = inmediato();
		traslado.asignar();
		Instant tarde = AHORA.plus(Duration.ofMinutes(50));

		traslado.devolverABusqueda(tarde, Duration.ofMinutes(60), Duration.ofMinutes(15));

		assertThat(traslado.getEstado()).isEqualTo(EstadoTraslado.BUSCANDO_UNIDAD);
		assertThat(traslado.getHoraDevolucion()).isEqualTo(tarde);
		assertThat(traslado.getHoraLimiteSalida()).isEqualTo(tarde.plus(Duration.ofMinutes(60)));
		assertThat(traslado.getHoraRecogidaHasta()).isEqualTo(tarde.plus(Duration.ofMinutes(75)));
		assertThat(traslado.getHoraRecogidaDesde()).isEqualTo(tarde.plus(Duration.ofMinutes(15)));
	}

	@Test
	@Tag("PB-27")
	@DisplayName("PB-27 · el aviso a la familia se marca solo en un traslado no cubierto, y queda la primera hora")
	void familiaAvisada() {
		Traslado traslado = inmediato();
		rechazaCon(CodigoError.TRANSICION_INVALIDA, () -> traslado.marcarFamiliaAvisada(AHORA));

		traslado.marcarNoCubierto();
		traslado.marcarFamiliaAvisada(AHORA);
		traslado.marcarFamiliaAvisada(AHORA.plusSeconds(600));

		assertThat(traslado.getHoraFamiliaAvisada()).isEqualTo(AHORA);
	}

	@Test
	@Tag("PB-28")
	@DisplayName("PB-28 · si la ficha decía menos de lo que el paciente necesita, se corrige hacia una unidad mayor")
	void corregirNecesidades() {
		Traslado traslado = inmediato();

		traslado.corregirNecesidades(Movilidad.CAMILLA, true, false, TipoUnidad.II);

		assertThat(traslado.tipoUnidadEfectivo()).isEqualTo(TipoUnidad.II);
		assertThat(traslado.getTipoUnidadPedido()).isEqualTo(TipoUnidad.IA);
		assertThat(traslado.getMovilidad()).isEqualTo(Movilidad.CAMILLA);
	}

	private static Traslado programado() {
		Instant limite = CITA.minus(Duration.ofMinutes(40));
		return nuevo(new Horario(ModoHorario.PROGRAMADO, CITA, limite.minus(Duration.ofMinutes(20)), limite,
				limite.minus(Duration.ofMinutes(5)), limite.plus(Duration.ofMinutes(15))));
	}

	private static Traslado inmediato() {
		return nuevo(horarioInmediato(AHORA));
	}

	private static Horario horarioInmediato(Instant ahora) {
		return new Horario(ModoHorario.INMEDIATO, null, ahora, ahora.plus(Duration.ofMinutes(60)),
				ahora.plus(Duration.ofMinutes(15)), ahora.plus(Duration.ofMinutes(75)));
	}

	private static Traslado nuevo(Horario horario) {
		Usuario solicitante = Usuario.registrarCiudadano("Ana Rojas", "60000123", AHORA);
		return Traslado.registrar(solicitante, solicitante, necesidades(), Geo.punto(-17.78329, -63.18210), null,
				null, null, null, Geo.punto(-17.76529, -63.18210), "Clínica del barrio", horario, TipoUnidad.IA,
				AHORA);
	}

	private static Necesidades necesidades() {
		return new Necesidades(Movilidad.CAMINA_CON_AYUDA, false, false, false, null, 0, null);
	}

}
