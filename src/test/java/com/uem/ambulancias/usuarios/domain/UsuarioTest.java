package com.uem.ambulancias.usuarios.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("PB-02")
class UsuarioTest {

	private static final Instant AHORA = Instant.parse("2026-10-09T15:00:00Z");

	@Test
	@DisplayName("PB-02 · el PIN se bloquea al quinto intento fallido, y cada intento dice cuántos quedan")
	void bloqueoDelPin() {
		Usuario paramedico = activado();

		assertThat(paramedico.registrarPinFallido(AHORA)).isEqualTo(4);
		assertThat(paramedico.registrarPinFallido(AHORA)).isEqualTo(3);
		assertThat(paramedico.registrarPinFallido(AHORA)).isEqualTo(2);
		assertThat(paramedico.registrarPinFallido(AHORA)).isEqualTo(1);
		assertThat(paramedico.isPinBloqueado()).isFalse();

		assertThat(paramedico.registrarPinFallido(AHORA)).isZero();
		assertThat(paramedico.isPinBloqueado()).isTrue();
	}

	@Test
	@DisplayName("PB-02 · un PIN correcto vuelve a contar los intentos desde cero")
	void pinCorrectoReiniciaIntentos() {
		Usuario paramedico = activado();
		paramedico.registrarPinFallido(AHORA);
		paramedico.registrarPinFallido(AHORA);

		paramedico.registrarPinCorrecto();

		assertThat(paramedico.registrarPinFallido(AHORA)).isEqualTo(4);
	}

	@Test
	@DisplayName("PB-02 · el código de activación vence a su hora y se anula al quinto intento fallido")
	void codigoDeActivacion() {
		Usuario paramedico = Usuario.registrarParamedico("Ana Rojas", "70000001");
		paramedico.emitirCodigoActivacion("cifrado", AHORA.plus(Duration.ofHours(24)), AHORA);

		assertThat(paramedico.codigoActivacionVencido(AHORA)).isFalse();
		assertThat(paramedico.codigoActivacionVencido(AHORA.plus(Duration.ofHours(24)))).isTrue();

		for (int i = 0; i < 4; i++) {
			paramedico.registrarCodigoFallido();
		}
		assertThat(paramedico.tieneCodigoActivacion()).isTrue();
		assertThat(paramedico.registrarCodigoFallido()).isZero();
		assertThat(paramedico.tieneCodigoActivacion()).isFalse();
	}

	@Test
	@DisplayName("PB-02 · un código nuevo anula el PIN, el teléfono vinculado y los avisos, y cierra las sesiones")
	void codigoNuevoAnulaElAcceso() {
		Usuario paramedico = activado();
		paramedico.registrarDispositivo("token-push");
		paramedico.registrarPinFallido(AHORA);

		Instant despues = AHORA.plusSeconds(60);
		paramedico.emitirCodigoActivacion("otro-cifrado", despues.plus(Duration.ofHours(24)), despues);

		assertThat(paramedico.isAccesoActivado()).isFalse();
		assertThat(paramedico.getPinCifrado()).isNull();
		assertThat(paramedico.getClaveDispositivoCifrada()).isNull();
		assertThat(paramedico.getTokenPush()).isNull();
		assertThat(paramedico.isPinBloqueado()).isFalse();
		assertThat(paramedico.getSesionesCerradasEn()).isEqualTo(despues);
	}

	@Test
	@DisplayName("PB-02 · activar el acceso guarda PIN y teléfono, y deja el código usado")
	void activarAcceso() {
		Usuario paramedico = Usuario.registrarParamedico("Ana Rojas", "70000001");
		paramedico.emitirCodigoActivacion("cifrado", AHORA.plus(Duration.ofHours(24)), AHORA);

		paramedico.activarAcceso("pin-cifrado", "clave-cifrada");

		assertThat(paramedico.isAccesoActivado()).isTrue();
		assertThat(paramedico.tieneCodigoActivacion()).isFalse();
	}

	private static Usuario activado() {
		Usuario paramedico = Usuario.registrarParamedico("Ana Rojas", "70000001");
		paramedico.activarAcceso("pin-cifrado", "clave-cifrada");
		return paramedico;
	}

}
