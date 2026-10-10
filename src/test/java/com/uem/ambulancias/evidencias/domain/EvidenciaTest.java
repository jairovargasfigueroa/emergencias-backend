package com.uem.ambulancias.evidencias.domain;

import static com.uem.ambulancias.soporte.Conflictos.rechazaCon;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.uem.ambulancias.comun.error.CodigoError;
import com.uem.ambulancias.comun.geo.Geo;
import com.uem.ambulancias.emergencias.domain.Alerta;
import com.uem.ambulancias.emergencias.domain.EstadoIncidente;
import com.uem.ambulancias.emergencias.domain.Incidente;
import com.uem.ambulancias.emergencias.domain.MotivoCancelacionAlerta;
import com.uem.ambulancias.emergencias.domain.OrigenUbicacion;
import com.uem.ambulancias.usuarios.domain.Usuario;

@Tag("PB-18")
class EvidenciaTest {

	private static final Instant AHORA = Instant.parse("2026-10-10T15:00:00Z");
	private static final long LIMITE = 10L * 1024 * 1024;
	private static final String SHA = "A".repeat(64);

	@Test
	@DisplayName("PB-18 · la evidencia nace pendiente de subida, con su SHA-256 normalizado")
	void registrar() {
		Evidencia evidencia = Evidencia.registrar(alerta(), FormatoEvidencia.JPEG, LIMITE, SHA, LIMITE, AHORA);

		assertThat(evidencia.getEstado()).isEqualTo(EstadoEvidencia.PENDIENTE_SUBIDA);
		assertThat(evidencia.isPendienteDeSubida()).isTrue();
		assertThat(evidencia.tieneArchivo()).isFalse();
		assertThat(evidencia.getSha256()).isEqualTo("a".repeat(64));
		assertThat(evidencia.getSha256Base64())
				.isEqualTo(Base64.getEncoder().encodeToString(HexFormat.of().parseHex("a".repeat(64))));
	}

	@Test
	@DisplayName("PB-18 · un archivo que pasa el límite de su tipo se rechaza")
	void demasiadoGrande() {
		rechazaCon(CodigoError.EVIDENCIA_DEMASIADO_GRANDE,
				() -> Evidencia.registrar(alerta(), FormatoEvidencia.JPEG, LIMITE + 1, SHA, LIMITE, AHORA));
	}

	@Test
	@DisplayName("PB-18 · una alerta retirada o un incidente cerrado no reciben archivos")
	void alertaCerrada() {
		Alerta retirada = alerta();
		retirada.cancelar(MotivoCancelacionAlerta.ERROR, null);
		rechazaCon(CodigoError.ALERTA_CERRADA,
				() -> Evidencia.registrar(retirada, FormatoEvidencia.JPEG, 100, SHA, LIMITE, AHORA));

		Alerta deIncidenteCerrado = alerta();
		deIncidenteCerrado.getIncidente().cambiarEstado(EstadoIncidente.CANCELADO);
		rechazaCon(CodigoError.ALERTA_CERRADA,
				() -> Evidencia.registrar(deIncidenteCerrado, FormatoEvidencia.JPEG, 100, SHA, LIMITE, AHORA));
	}

	@Test
	@DisplayName("PB-18 · el archivo pasa de pendiente a subido y de ahí a analizado o fallido, sin saltos")
	void estados() {
		Evidencia analizada = Evidencia.registrar(alerta(), FormatoEvidencia.PNG, 100, SHA, LIMITE, AHORA);
		rechazaCon(CodigoError.TRANSICION_INVALIDA, () -> analizada.marcarAnalizada(AHORA));

		analizada.confirmarSubida(AHORA);
		assertThat(analizada.tieneArchivo()).isTrue();
		rechazaCon(CodigoError.TRANSICION_INVALIDA, () -> analizada.confirmarSubida(AHORA));
		analizada.marcarAnalizada(AHORA);
		assertThat(analizada.getEstado()).isEqualTo(EstadoEvidencia.ANALIZADA);

		Evidencia fallida = Evidencia.registrar(alerta(), FormatoEvidencia.MPEG, 100, SHA, LIMITE, AHORA);
		fallida.confirmarSubida(AHORA);
		fallida.marcarFallida(AHORA);
		assertThat(fallida.getEstado()).isEqualTo(EstadoEvidencia.FALLIDA);
		assertThat(fallida.tieneArchivo()).isTrue();
	}

	@Test
	@DisplayName("PB-18 · descartar saca la evidencia de circulación desde cualquier estado")
	void descartar() {
		Evidencia evidencia = Evidencia.registrar(alerta(), FormatoEvidencia.JPEG, 100, SHA, LIMITE, AHORA);
		evidencia.confirmarSubida(AHORA);

		evidencia.descartar();
		evidencia.descartar();

		assertThat(evidencia.getEstado()).isEqualTo(EstadoEvidencia.DESCARTADA);
		assertThat(evidencia.tieneArchivo()).isFalse();
	}

	private static Alerta alerta() {
		Alerta alerta = Alerta.emitir(Usuario.registrarCiudadano("Ana Rojas", "60000123", AHORA),
				Geo.punto(-17.78329, -63.18210), OrigenUbicacion.GPS, null, null, AHORA);
		alerta.vincular(Incidente.crear(alerta.getUbicacionEfectiva(), null, AHORA));
		return alerta;
	}

}
