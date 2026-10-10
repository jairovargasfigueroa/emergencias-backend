package com.uem.ambulancias.flota.domain;

import static com.uem.ambulancias.soporte.Conflictos.rechazaCon;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.uem.ambulancias.comun.error.CodigoError;

@Tag("PB-01")
class AmbulanciaTest {

	@Test
	@DisplayName("PB-01 · una ambulancia nueva queda activa y sin turno, y todavía no puede atender")
	void nueva() {
		Ambulancia ambulancia = Ambulancia.registrar("ABC123", TipoUnidad.II);

		assertThat(ambulancia.getEstado()).isEqualTo(EstadoAmbulancia.SIN_TURNO);
		assertThat(ambulancia.isActiva()).isTrue();
		assertThat(ambulancia.puedeAtender()).isFalse();
	}

	@Test
	@DisplayName("PB-01 · solo una ambulancia activa y disponible puede atender")
	void puedeAtender() {
		Ambulancia ambulancia = disponible();
		assertThat(ambulancia.puedeAtender()).isTrue();

		ambulancia.desactivar();
		assertThat(ambulancia.puedeAtender()).isFalse();
	}

	@Test
	@DisplayName("PB-01 · una ambulancia en atención no se pone fuera de servicio")
	void fueraDeServicioEnAtencion() {
		Ambulancia ambulancia = enAtencion();

		rechazaCon(CodigoError.AMBULANCIA_EN_ATENCION, ambulancia::marcarFueraDeServicio);
	}

	@Test
	@DisplayName("PB-01 · una ambulancia en atención no se desactiva")
	void desactivarEnAtencion() {
		Ambulancia ambulancia = enAtencion();

		rechazaCon(CodigoError.AMBULANCIA_EN_ATENCION, ambulancia::desactivar);
		assertThat(ambulancia.isActiva()).isTrue();
	}

	@Test
	@DisplayName("PB-01 · en atención se corrige la placa, pero no el tipo de unidad")
	void corregirDatosEnAtencion() {
		Ambulancia ambulancia = enAtencion();

		ambulancia.corregirDatos("XYZ789", TipoUnidad.II);
		assertThat(ambulancia.getPlaca()).isEqualTo("XYZ789");

		rechazaCon(CodigoError.AMBULANCIA_EN_ATENCION, () -> ambulancia.corregirDatos("XYZ789", TipoUnidad.III));
	}

	@Test
	@DisplayName("PB-01 · al reactivarse vuelve a Disponible si hay turno, y a Sin turno si no")
	void reactivar() {
		Ambulancia conTurno = disponible();
		conTurno.marcarFueraDeServicio();
		conTurno.reactivar(true);
		assertThat(conTurno.getEstado()).isEqualTo(EstadoAmbulancia.DISPONIBLE);

		Ambulancia sinTurno = Ambulancia.registrar("DEF456", TipoUnidad.II);
		sinTurno.marcarFueraDeServicio();
		sinTurno.reactivar(false);
		assertThat(sinTurno.getEstado()).isEqualTo(EstadoAmbulancia.SIN_TURNO);
	}

	@Test
	@DisplayName("PB-01 · solo se reactiva una ambulancia que está fuera de servicio")
	void reactivarSinEstarFueraDeServicio() {
		Ambulancia ambulancia = disponible();

		rechazaCon(CodigoError.TRANSICION_INVALIDA, () -> ambulancia.reactivar(true));
	}

	@Test
	@DisplayName("PB-01 · una ambulancia sin turno no pasa directo a En atención")
	void sinTurnoNoAtiende() {
		Ambulancia ambulancia = Ambulancia.registrar("ABC123", TipoUnidad.II);

		rechazaCon(CodigoError.TRANSICION_INVALIDA, () -> ambulancia.cambiarEstado(EstadoAmbulancia.EN_ATENCION));
	}

	@Test
	@DisplayName("PB-01 · una ambulancia desactivada no puede pasar a En atención")
	void desactivadaNoAtiende() {
		Ambulancia ambulancia = disponible();
		ambulancia.desactivar();

		rechazaCon(CodigoError.AMBULANCIA_NO_DISPONIBLE, () -> ambulancia.cambiarEstado(EstadoAmbulancia.EN_ATENCION));
	}

	private static Ambulancia disponible() {
		Ambulancia ambulancia = Ambulancia.registrar("ABC123", TipoUnidad.II);
		ambulancia.cambiarEstado(EstadoAmbulancia.DISPONIBLE);
		return ambulancia;
	}

	private static Ambulancia enAtencion() {
		Ambulancia ambulancia = disponible();
		ambulancia.cambiarEstado(EstadoAmbulancia.EN_ATENCION);
		return ambulancia;
	}

}
