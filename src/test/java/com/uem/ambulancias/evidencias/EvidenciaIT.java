package com.uem.ambulancias.evidencias;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import com.uem.ambulancias.evidencias.service.LimpiezaDeEvidencias;
import com.uem.ambulancias.soporte.AlmacenEnMemoria;
import com.uem.ambulancias.soporte.Escenario;
import com.uem.ambulancias.soporte.Escenario.Alerta;
import com.uem.ambulancias.soporte.Escenario.Ciudadano;
import com.uem.ambulancias.soporte.Escenario.Evidencia;
import com.uem.ambulancias.soporte.Json;
import com.uem.ambulancias.soporte.PruebaIT;

/**
 * El archivo nunca pasa por el servidor: la app anuncia qué va a subir, sube directo al almacén con la URL firmada y
 * avisa que terminó. En las pruebas el almacén es {@link AlmacenEnMemoria}.
 */
@Tag("PB-18")
class EvidenciaIT extends PruebaIT {

	private static final long UN_MIB = 1024L * 1024;

	@Autowired
	private LimpiezaDeEvidencias limpieza;

	@Test
	@DisplayName("PB-18 · CP-18-01 · la alerta se crea sin esperar archivos, y si un archivo nunca llega el pedido sigue en pie")
	void laAlertaNoEspera() throws Exception {
		Ciudadano ciudadano = escenario.ciudadano();
		Alerta alerta = escenario.alertar(ciudadano);
		Evidencia evidencia = escenario.anunciarEvidencia(ciudadano, alerta.alertaId(), "image/jpeg", 500_000);

		confirmar(ciudadano, evidencia.id())
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("EVIDENCIA_NO_SUBIDA"));

		pedir(get("/incidentes/" + alerta.incidenteId()), escenario.admin())
				.andExpect(jsonPath("$.estado").value("ACTIVO"))
				.andExpect(jsonPath("$.alertas[0].estado").value("VINCULADA"));
	}

	@Test
	@DisplayName("PB-18 · CP-18-02 · anunciar una foto devuelve dónde subirla, con las cabeceras y el vencimiento de la firma")
	void anunciar() throws Exception {
		Ciudadano ciudadano = escenario.ciudadano();
		Alerta alerta = escenario.alertar(ciudadano);

		String respuesta = cuerpo(anunciar(ciudadano, alerta.alertaId(), "image/jpeg", 500_000)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.evidenciaId").isNumber())
				.andExpect(jsonPath("$.urlSubida").isNotEmpty())
				.andExpect(jsonPath("$.cabeceras['Content-Type']").value("image/jpeg"))
				.andExpect(jsonPath("$.venceEn").isNotEmpty()));

		Instant vence = Instant.parse(Json.texto(respuesta, "$.venceEn"));
		assertThat(Duration.between(Instant.now(), vence)).isBetween(Duration.ofMinutes(14), Duration.ofMinutes(15));
		long evidencia = Json.numero(respuesta, "$.evidenciaId");
		assertThat(AlmacenEnMemoria.claveDe(Json.texto(respuesta, "$.urlSubida")))
				.isEqualTo("alertas/" + alerta.alertaId() + "/" + evidencia + ".jpg");
		assertThat(jdbc.queryForObject("select estado from evidencia where id = ?", String.class, evidencia))
				.isEqualTo("PENDIENTE_SUBIDA");
	}

	@Test
	@DisplayName("PB-18 · CP-18-02 · subida y confirmada, la foto o el audio quedan recibidos en la alerta de quien los mandó")
	void subirYConfirmar() throws Exception {
		Ciudadano ciudadano = escenario.ciudadano();
		Alerta alerta = escenario.alertar(ciudadano);
		Evidencia foto = escenario.anunciarEvidencia(ciudadano, alerta.alertaId(), "image/png", 800_000);
		Evidencia audio = escenario.anunciarEvidencia(ciudadano, alerta.alertaId(), "audio/mp4", 300_000);
		escenario.subir(foto);
		escenario.subir(audio);

		confirmar(ciudadano, foto.id())
				.andExpect(status().isAccepted())
				.andExpect(jsonPath("$.alertaId").value(alerta.alertaId()))
				.andExpect(jsonPath("$.modalidad").value("IMAGEN"))
				.andExpect(jsonPath("$.estado").value("SUBIDA"));
		confirmar(ciudadano, audio.id())
				.andExpect(status().isAccepted())
				.andExpect(jsonPath("$.modalidad").value("AUDIO"));
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = { "application/pdf", "image/gif", "video/mp4", "video/quicktime" })
	@DisplayName("PB-18 · CP-18-03 · tipos que no se pueden analizar, y el video, que está apagado, se rechazan")
	void formatoNoAdmitido(String mimeType) throws Exception {
		Ciudadano ciudadano = escenario.ciudadano();
		Alerta alerta = escenario.alertar(ciudadano);

		anunciar(ciudadano, alerta.alertaId(), mimeType, 500_000)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("FORMATO_NO_ADMITIDO"));
	}

	@Test
	@DisplayName("PB-18 · CP-18-03 · cada tipo tiene su tamaño máximo: 10 MiB la foto y 5 MiB el audio")
	void tamanoMaximo() throws Exception {
		Ciudadano ciudadano = escenario.ciudadano();
		Alerta alerta = escenario.alertar(ciudadano);

		anunciar(ciudadano, alerta.alertaId(), "image/jpeg", 10 * UN_MIB).andExpect(status().isCreated());
		anunciar(ciudadano, alerta.alertaId(), "image/jpeg", 10 * UN_MIB + 1)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("EVIDENCIA_DEMASIADO_GRANDE"));
		anunciar(ciudadano, alerta.alertaId(), "audio/mpeg", 5 * UN_MIB + 1)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("EVIDENCIA_DEMASIADO_GRANDE"));
	}

	@Test
	@DisplayName("PB-18 · CP-18-03 · sin tamaño, vacío o con un SHA-256 mal formado no se anuncia")
	void datosInvalidos() throws Exception {
		Ciudadano ciudadano = escenario.ciudadano();
		long alerta = escenario.alertar(ciudadano).alertaId();

		pedir(post("/alertas/" + alerta + "/evidencias"), ciudadano.token(),
				Json.objeto("mimeType", "image/jpeg", "sha256", "a".repeat(64)))
				.andExpect(status().isBadRequest());
		anunciar(ciudadano, alerta, "image/jpeg", 0).andExpect(status().isBadRequest());
		pedir(post("/alertas/" + alerta + "/evidencias"), ciudadano.token(),
				Json.objeto("mimeType", "image/jpeg", "tamanoBytes", 100, "sha256", "no-es-un-hash"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value("VALIDACION"));
	}

	@Test
	@DisplayName("PB-18 · CP-18-03 · solo el emisor adjunta archivos a su alerta")
	void soloElEmisor() throws Exception {
		Alerta alerta = escenario.alertar(escenario.ciudadano());

		anunciar(escenario.ciudadano(), alerta.alertaId(), "image/jpeg", 100)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("ALERTA_AJENA"));
		pedir(post("/alertas/" + alerta.alertaId() + "/evidencias"), escenario.paramedicoActivado().token(),
				Json.objeto("mimeType", "image/jpeg", "tamanoBytes", 100, "sha256", "a".repeat(64)))
				.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("PB-18 · CP-18-03 · hasta 5 archivos por alerta; los descartados no cuentan")
	void maximoPorAlerta() throws Exception {
		Ciudadano ciudadano = escenario.ciudadano();
		long alerta = escenario.alertar(ciudadano).alertaId();
		for (int i = 0; i < 5; i++) {
			escenario.anunciarEvidencia(ciudadano, alerta, "image/jpeg", 100);
		}

		anunciar(ciudadano, alerta, "image/jpeg", 100)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("LIMITE_DE_EVIDENCIAS"));

		jdbc.update("update evidencia set estado = 'DESCARTADA' where id = (select min(id) from evidencia)");
		anunciar(ciudadano, alerta, "image/jpeg", 100).andExpect(status().isCreated());
	}

	@Test
	@DisplayName("PB-18 · CP-18-03 · hasta 15 archivos por incidente, sumando los de todos los que avisaron")
	void maximoPorIncidente() throws Exception {
		for (int persona = 0; persona < 3; persona++) {
			Ciudadano ciudadano = escenario.ciudadano();
			long alerta = escenario.alertar(ciudadano).alertaId();
			for (int i = 0; i < 5; i++) {
				escenario.anunciarEvidencia(ciudadano, alerta, "audio/mp4", 100);
			}
		}
		Ciudadano cuarto = escenario.ciudadano();
		long alerta = escenario.alertar(cuarto).alertaId();

		anunciar(cuarto, alerta, "image/jpeg", 100)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("LIMITE_DE_EVIDENCIAS_INCIDENTE"));
	}

	@Test
	@DisplayName("PB-18 · CP-18-04 · confirmar exige que el archivo subido sea el anunciado: mismo tamaño y mismo contenido")
	void archivoDistinto() throws Exception {
		Ciudadano ciudadano = escenario.ciudadano();
		long alerta = escenario.alertar(ciudadano).alertaId();
		Evidencia otroTamano = escenario.anunciarEvidencia(ciudadano, alerta, "image/jpeg", 500_000);
		almacen.subir(otroTamano.urlSubida(), 499_999, otroTamano.sha256Base64());
		Evidencia otroContenido = escenario.anunciarEvidencia(ciudadano, alerta, "image/jpeg", 500_000);
		almacen.subir(otroContenido.urlSubida(), 500_000, otroTamano.sha256Base64());

		confirmar(ciudadano, otroTamano.id())
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("EVIDENCIA_NO_COINCIDE"));
		confirmar(ciudadano, otroContenido.id())
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("EVIDENCIA_NO_COINCIDE"));
	}

	@Test
	@DisplayName("PB-18 · CP-18-04 · confirmar dos veces no repite nada, y deja un solo análisis pendiente")
	void confirmarDosVeces() throws Exception {
		Ciudadano ciudadano = escenario.ciudadano();
		Evidencia evidencia = escenario.evidenciaSubida(ciudadano, escenario.alertar(ciudadano).alertaId(),
				"image/jpeg");

		confirmar(ciudadano, evidencia.id())
				.andExpect(status().isAccepted())
				.andExpect(jsonPath("$.estado").value("SUBIDA"));

		assertThat(jdbc.queryForObject(
				"select count(*) from trabajo_ia where evidencia_id = ? and tipo = 'ANALISIS' and estado = 'PENDIENTE'",
				Long.class, evidencia.id())).isEqualTo(1);
	}

	@Test
	@DisplayName("PB-18 · CP-18-04 · nadie más confirma ni pide firmas de una evidencia ajena")
	void evidenciaAjena() throws Exception {
		Ciudadano ciudadano = escenario.ciudadano();
		Evidencia evidencia = escenario.anunciarEvidencia(ciudadano, escenario.alertar(ciudadano).alertaId(),
				"image/jpeg", 100);
		escenario.subir(evidencia);
		Ciudadano otro = escenario.ciudadano();

		confirmar(otro, evidencia.id())
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("EVIDENCIA_AJENA"));
		pedir(post("/evidencias/" + evidencia.id() + "/url-subida"), otro.token())
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("EVIDENCIA_AJENA"));
		pedir(post("/evidencias/999999/confirmacion"), ciudadano.token()).andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("PB-18 · CP-18-05 · si la firma venció pide otra; ya subido o con la alerta retirada, no")
	void firmarDeNuevo() throws Exception {
		Ciudadano ciudadano = escenario.ciudadano();
		Alerta alerta = escenario.alertar(ciudadano);
		Evidencia pendiente = escenario.anunciarEvidencia(ciudadano, alerta.alertaId(), "image/jpeg", 100);
		Evidencia subida = escenario.evidenciaSubida(ciudadano, alerta.alertaId(), "image/jpeg");

		pedir(post("/evidencias/" + pendiente.id() + "/url-subida"), ciudadano.token())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.evidenciaId").value(pendiente.id()))
				.andExpect(jsonPath("$.urlSubida").isNotEmpty());
		pedir(post("/evidencias/" + subida.id() + "/url-subida"), ciudadano.token())
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("EVIDENCIA_YA_SUBIDA"));

		pedir(post("/alertas/" + alerta.alertaId() + "/cancelacion"), ciudadano.token(),
				Json.objeto("motivo", "ERROR"))
				.andExpect(status().isOk());
		pedir(post("/evidencias/" + pendiente.id() + "/url-subida"), ciudadano.token())
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("ALERTA_CERRADA"));
		anunciar(ciudadano, alerta.alertaId(), "image/jpeg", 100)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("ALERTA_CERRADA"));
	}

	@Test
	@DisplayName("PB-18 · CP-18-06 · lo anunciado y nunca subido se descarta al día, y se borra lo que quedó a medias")
	void abandonadas() throws Exception {
		Ciudadano ciudadano = escenario.ciudadano();
		long alerta = escenario.alertar(ciudadano).alertaId();
		Evidencia abandonada = escenario.anunciarEvidencia(ciudadano, alerta, "image/jpeg", 100);
		Evidencia reciente = escenario.anunciarEvidencia(ciudadano, alerta, "image/jpeg", 100);
		jdbc.update("update evidencia set registrada_en = now() - interval '25 hours' where id = ?", abandonada.id());

		limpieza.barrer();

		assertThat(estado(abandonada)).isEqualTo("DESCARTADA");
		assertThat(estado(reciente)).isEqualTo("PENDIENTE_SUBIDA");
		assertThat(almacen.fueBorrado(AlmacenEnMemoria.claveDe(abandonada.urlSubida()))).isTrue();
	}

	private ResultActions anunciar(Ciudadano ciudadano, long alerta, String mimeType, long tamano) throws Exception {
		return pedir(post("/alertas/" + alerta + "/evidencias"), ciudadano.token(),
				Json.objeto("mimeType", mimeType, "tamanoBytes", tamano, "sha256", "b".repeat(64)));
	}

	private ResultActions confirmar(Ciudadano ciudadano, long evidencia) throws Exception {
		return pedir(post("/evidencias/" + evidencia + "/confirmacion"), ciudadano.token());
	}

	private String estado(Evidencia evidencia) {
		return jdbc.queryForObject("select estado from evidencia where id = ?", String.class, evidencia.id());
	}

}
