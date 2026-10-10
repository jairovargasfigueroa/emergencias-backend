package com.uem.ambulancias.flota.domain;

import static com.uem.ambulancias.soporte.Conflictos.rechazaCon;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.uem.ambulancias.comun.error.CodigoError;
import com.uem.ambulancias.usuarios.domain.Usuario;

@Tag("PB-04")
class TurnoTest {

	private static final Instant INICIO = Instant.parse("2026-10-09T08:00:00Z");

	@Test
	@DisplayName("PB-04 · un turno queda abierto hasta que se termina, y guarda la hora del cierre")
	void terminar() {
		Turno turno = nuevo();
		assertThat(turno.isAbierto()).isTrue();

		Instant fin = INICIO.plusSeconds(8 * 3600);
		turno.terminar(fin);

		assertThat(turno.isAbierto()).isFalse();
		assertThat(turno.getFin()).isEqualTo(fin);
	}

	@Test
	@DisplayName("PB-04 · un turno terminado no se vuelve a terminar")
	void terminarDosVeces() {
		Turno turno = nuevo();
		turno.terminar(INICIO.plusSeconds(3600));

		rechazaCon(CodigoError.TRANSICION_INVALIDA, () -> turno.terminar(INICIO.plusSeconds(7200)));
	}

	private static Turno nuevo() {
		return Turno.iniciar(Usuario.registrarParamedico("Ana Rojas", "70000001"),
				Ambulancia.registrar("ABC123", TipoUnidad.II), INICIO);
	}

}
