package com.uem.ambulancias.emergencias.domain;

import static com.uem.ambulancias.soporte.Conflictos.rechazaCon;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.uem.ambulancias.comun.error.CodigoError;
import com.uem.ambulancias.comun.geo.Geo;
import com.uem.ambulancias.usuarios.domain.Usuario;

class AlertaTest {

	private static final Instant AHORA = Instant.parse("2026-10-09T15:00:00Z");

	@Test
	@Tag("PB-05")
	@DisplayName("PB-05 · la alerta nace recibida y queda vinculada al sumarse a un incidente")
	void emitirYVincular() {
		Alerta alerta = nueva(null, null);
		assertThat(alerta.getEstado()).isEqualTo(EstadoAlerta.RECIBIDA);
		assertThat(alerta.getUbicacionEfectiva()).isEqualTo(alerta.getUbicacionOriginal());

		Incidente incidente = Incidente.crear(alerta.getUbicacionEfectiva(), null, AHORA);
		alerta.vincular(incidente);

		assertThat(alerta.getEstado()).isEqualTo(EstadoAlerta.VINCULADA);
		assertThat(alerta.getIncidente()).isSameAs(incidente);
	}

	@Test
	@Tag("PB-10")
	@DisplayName("PB-10 · completar los datos después solo cambia los que llegan")
	void completarDetalles() {
		Alerta alerta = nueva(2, "Choque de moto");

		alerta.completarDetalles(null, "Choque de moto, el conductor no responde");
		assertThat(alerta.getCantidadAfectados()).isEqualTo(2);
		assertThat(alerta.getDescripcion()).isEqualTo("Choque de moto, el conductor no responde");

		alerta.completarDetalles(3, null);
		assertThat(alerta.getCantidadAfectados()).isEqualTo(3);
		assertThat(alerta.getDescripcion()).isEqualTo("Choque de moto, el conductor no responde");
	}

	@Test
	@Tag("PB-11")
	@DisplayName("PB-11 · retirar el pedido guarda el motivo y no se puede retirar dos veces")
	void cancelar() {
		Alerta alerta = nueva(null, null);

		alerta.cancelar(MotivoCancelacionAlerta.FALSA_ALARMA, true);

		assertThat(alerta.getEstado()).isEqualTo(EstadoAlerta.CANCELADA);
		assertThat(alerta.getMotivoCancelacion()).isEqualTo(MotivoCancelacionAlerta.FALSA_ALARMA);
		assertThat(alerta.getEmisorEsPaciente()).isTrue();
		assertThat(alerta.getHoraCancelacion()).isNotNull();
		rechazaCon(CodigoError.TRANSICION_INVALIDA, () -> alerta.cancelar(MotivoCancelacionAlerta.ERROR, null));
	}

	private static Alerta nueva(Integer afectados, String descripcion) {
		return Alerta.emitir(Usuario.registrarCiudadano("Ana Rojas", "60000123", AHORA), Geo.punto(-17.78329, -63.18210),
				OrigenUbicacion.GPS, afectados, descripcion, AHORA);
	}

}
