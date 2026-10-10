package com.uem.ambulancias.integracion.ia;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpStatusCode;

import com.uem.ambulancias.evidencias.service.FalloDelServicioIa.Desenlace;

/** La tabla con la que el backend decide qué hacer con cada error del servicio de IA. */
@Tag("PB-19")
class ClienteIaHttpTest {

	@ParameterizedTest(name = "{0} → {1}")
	@CsvSource({
			"download_forbidden, FIRMAR_DE_NUEVO",
			"unauthorized, REVISION",
			"provider_misconfigured, REVISION",
			"unreadable_media, DEFINITIVO",
			"unsupported_media_type, DEFINITIVO",
			"checksum_mismatch, DEFINITIVO",
			"nothing_to_summarize, DEFINITIVO",
			"provider_unavailable, REINTENTAR",
			"invalid_model_output, REINTENTAR" })
	@DisplayName("PB-19 · cada error conocido del servicio tiene su desenlace: reintentar, firmar otra vez, dejarlo o revisarlo")
	void erroresConocidos(String codigo, Desenlace desenlace) {
		assertThat(ClienteIaHttp.desenlace(HttpStatusCode.valueOf(422), codigo, null)).isEqualTo(desenlace);
	}

	@Test
	@DisplayName("PB-19 · un error desconocido se resuelve con lo que diga el servicio sobre reintentarlo")
	void desconocidoConReintentable() {
		assertThat(ClienteIaHttp.desenlace(HttpStatusCode.valueOf(400), "algo_nuevo", true))
				.isEqualTo(Desenlace.REINTENTAR);
		assertThat(ClienteIaHttp.desenlace(HttpStatusCode.valueOf(503), "algo_nuevo", false))
				.isEqualTo(Desenlace.DEFINITIVO);
	}

	@ParameterizedTest(name = "HTTP {0} → {1}")
	@CsvSource({ "401, REVISION", "403, REVISION", "404, REVISION", "500, REINTENTAR", "503, REINTENTAR",
			"429, REINTENTAR", "400, DEFINITIVO", "422, DEFINITIVO" })
	@DisplayName("PB-19 · sin código ni pista, decide el estado HTTP: configuración, pasajero o definitivo")
	void soloElEstado(int estado, Desenlace desenlace) {
		assertThat(ClienteIaHttp.desenlace(HttpStatusCode.valueOf(estado), "http_" + estado, null))
				.isEqualTo(desenlace);
	}

}
