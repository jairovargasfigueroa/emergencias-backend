package com.uem.ambulancias.evidencias;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import com.uem.ambulancias.emergencias.service.AvisoParaParamedico;
import com.uem.ambulancias.evidencias.service.AnalizadorDeEvidencias;
import com.uem.ambulancias.evidencias.service.ColaDeTrabajosIa;
import com.uem.ambulancias.evidencias.service.FalloDelServicioIa.Desenlace;
import com.uem.ambulancias.evidencias.service.PedidoDeAnalisis;
import com.uem.ambulancias.evidencias.service.PedidoDeResumen;
import com.uem.ambulancias.evidencias.service.ResumidorDeIncidentes;
import com.uem.ambulancias.soporte.Escenario;
import com.uem.ambulancias.soporte.Escenario.Alerta;
import com.uem.ambulancias.soporte.Escenario.Ciudadano;
import com.uem.ambulancias.soporte.Escenario.Evidencia;
import com.uem.ambulancias.soporte.Escenario.Paramedico;
import com.uem.ambulancias.soporte.Json;
import com.uem.ambulancias.soporte.PruebaIT;

/**
 * La cola de la IA de punta a punta: cada archivo confirmado se analiza, y con los análisis se rehace el resumen del
 * incidente. Los workers son los reales y cada prueba los dispara cuando quiere; quien responde es el servicio falso.
 */
@Tag("PB-19")
class ResumenIaIT extends PruebaIT {

	@Autowired
	private AnalizadorDeEvidencias analizador;

	@Autowired
	private ResumidorDeIncidentes resumidor;

	@Autowired
	private ColaDeTrabajosIa cola;

	@Test
	@DisplayName("PB-19 · CP-19-01 · el archivo confirmado se analiza y con eso sale la versión 1 del resumen, con sus fuentes")
	void analizarYResumir() throws Exception {
		Ciudadano ciudadano = escenario.ciudadano();
		Alerta alerta = alertaConDescripcion(ciudadano, "Mi papá se cayó y no se levanta");
		Evidencia foto = escenario.evidenciaSubida(ciudadano, alerta.alertaId(), "image/jpeg");

		analizador.barrer();

		assertThat(estado(foto)).isEqualTo("ANALIZADA");
		PedidoDeAnalisis pedido = ia.analisisPedidos().getFirst();
		assertThat(pedido.evidenciaId()).isEqualTo(foto.id());
		assertThat(pedido.incidenteId()).isEqualTo(alerta.incidenteId());
		assertThat(pedido.mimeType()).isEqualTo("image/jpeg");
		assertThat(pedido.urlLectura()).contains("firma=lectura");
		assertThat(trabajos("RESUMEN", "PENDIENTE")).isEqualTo(1);

		resumidor.barrer();

		PedidoDeResumen aResumir = ia.resumenesPedidos().getFirst();
		assertThat(aResumir.alertas()).singleElement()
				.satisfies(uno -> assertThat(uno.descripcion()).isEqualTo("Mi papá se cayó y no se levanta"));
		assertThat(aResumir.evidencias()).singleElement()
				.satisfies(una -> assertThat(una.analisisJson()).contains("transcript"));
		resumen(alerta.incidenteId())
				.andExpect(jsonPath("$.version").value(1))
				.andExpect(jsonPath("$.resumen.eventType").value("CAIDA"))
				.andExpect(jsonPath("$.resumen.suggestedSeverity").value("ALTA"))
				.andExpect(jsonPath("$.evidenciasUsadas[0]").value(foto.id()))
				.andExpect(jsonPath("$.alertasUsadas[0]").value(alerta.alertaId()))
				.andExpect(jsonPath("$.modelo").value("modelo-de-prueba"))
				.andExpect(jsonPath("$.generadoEn").isNotEmpty());
	}

	@Test
	@DisplayName("PB-19 · CP-19-01 · un archivo nuevo da la versión 2, y la anterior se conserva")
	void nuevaEvidenciaNuevaVersion() throws Exception {
		Ciudadano ciudadano = escenario.ciudadano();
		Alerta alerta = escenario.alertar(ciudadano);
		Evidencia foto = escenario.evidenciaSubida(ciudadano, alerta.alertaId(), "image/jpeg");
		correrLaIa();

		Evidencia audio = escenario.evidenciaSubida(ciudadano, alerta.alertaId(), "audio/mp4");
		correrLaIa();

		resumen(alerta.incidenteId())
				.andExpect(jsonPath("$.version").value(2))
				.andExpect(jsonPath("$.evidenciasUsadas.length()").value(2))
				.andExpect(jsonPath("$.evidenciasUsadas[0]").value(foto.id()))
				.andExpect(jsonPath("$.evidenciasUsadas[1]").value(audio.id()));
		assertThat(jdbc.queryForObject("select count(*) from resumen_incidente where incidente_id = ?", Long.class,
				alerta.incidenteId())).isEqualTo(2);
	}

	@Test
	@DisplayName("PB-19 · CP-19-01 · otra persona que avisa del mismo incidente hace rehacer el resumen")
	void nuevaAlertaRehaceElResumen() throws Exception {
		Ciudadano ciudadano = escenario.ciudadano();
		Alerta alerta = escenario.alertar(ciudadano);
		escenario.evidenciaSubida(ciudadano, alerta.alertaId(), "image/jpeg");
		correrLaIa();

		Alerta otra = escenario.alertar(escenario.ciudadano(), Escenario.LATITUD + Escenario.CIEN_METROS,
				Escenario.LONGITUD);
		assertThat(otra.incidenteId()).isEqualTo(alerta.incidenteId());
		assertThat(trabajos("RESUMEN", "PENDIENTE")).isEqualTo(1);
		resumidor.barrer();

		resumen(alerta.incidenteId())
				.andExpect(jsonPath("$.version").value(2))
				.andExpect(jsonPath("$.alertasUsadas.length()").value(2));
	}

	@Test
	@DisplayName("PB-19 · CP-19-01 · un resumen que no suma nada nuevo no reemplaza al vigente")
	void sinNovedadNoHayVersionNueva() throws Exception {
		Ciudadano ciudadano = escenario.ciudadano();
		Alerta alerta = escenario.alertar(ciudadano);
		escenario.evidenciaSubida(ciudadano, alerta.alertaId(), "image/jpeg");
		correrLaIa();

		cola.pedirResumen(alerta.incidenteId());
		resumidor.barrer();

		assertThat(ia.resumenesPedidos()).hasSize(2);
		resumen(alerta.incidenteId()).andExpect(jsonPath("$.version").value(1));
	}

	@Test
	@DisplayName("PB-19 · CP-19-02 · sin ningún análisis todavía no se pide resumen")
	void sinAnalisisNoHayResumen() throws Exception {
		Alerta alerta = escenario.alertar(escenario.ciudadano());

		cola.pedirResumen(alerta.incidenteId());
		resumidor.barrer();

		assertThat(ia.resumenesPedidos()).isEmpty();
		assertThat(trabajos("RESUMEN", "HECHO")).isEqualTo(1);
		resumen(alerta.incidenteId()).andExpect(jsonPath("$.version").doesNotExist());
	}

	@Test
	@DisplayName("PB-19 · CP-19-02 · si la IA no responde, el análisis espera su reintento y la atención sigue sin trabas")
	void laFallaNoBloquea() throws Exception {
		Ciudadano ciudadano = escenario.ciudadano();
		Alerta alerta = escenario.alertar(ciudadano);
		Evidencia foto = escenario.evidenciaSubida(ciudadano, alerta.alertaId(), "image/jpeg");
		ia.fallarAnalisisDe(foto.id(), "provider_unavailable", Desenlace.REINTENTAR);

		analizador.barrer();

		assertThat(estado(foto)).isEqualTo("SUBIDA");
		Map<String, Object> trabajo = jdbc.queryForMap(
				"select estado, intentos, ultimo_error, proximo_intento from trabajo_ia where evidencia_id = ?",
				foto.id());
		assertThat(trabajo.get("estado")).isEqualTo("PENDIENTE");
		assertThat(trabajo.get("intentos")).isEqualTo(1);
		assertThat(trabajo.get("ultimo_error")).isEqualTo("provider_unavailable");
		assertThat(((Timestamp) trabajo.get("proximo_intento")).toInstant()).isAfter(Instant.now());

		Paramedico paramedico = escenario.paramedicoEnTurno();
		escenario.marcar(paramedico, escenario.tomar(paramedico, alerta.incidenteId()), "llegada");
		resumen(alerta.incidenteId())
				.andExpect(jsonPath("$.version").doesNotExist())
				.andExpect(jsonPath("$.evidencias[0].estado").value("SUBIDA"));
	}

	@Test
	@DisplayName("PB-19 · CP-19-02 · agotados los cinco intentos, el análisis se da por fallido y el archivo queda sin analizar")
	void agotaIntentos() throws Exception {
		Ciudadano ciudadano = escenario.ciudadano();
		Evidencia foto = escenario.evidenciaSubida(ciudadano, escenario.alertar(ciudadano).alertaId(), "image/jpeg");
		ia.fallarAnalisisDe(foto.id(), "provider_unavailable", Desenlace.REINTENTAR);
		jdbc.update("update trabajo_ia set intentos = 4 where evidencia_id = ?", foto.id());

		analizador.barrer();

		assertThat(estadoDelTrabajo(foto)).isEqualTo("FALLIDO");
		assertThat(estado(foto)).isEqualTo("FALLIDA");
	}

	@Test
	@DisplayName("PB-19 · CP-19-02 · si venció la URL de lectura, se firma otra y se vuelve a intentar en el momento")
	void urlVencida() throws Exception {
		Ciudadano ciudadano = escenario.ciudadano();
		Evidencia foto = escenario.evidenciaSubida(ciudadano, escenario.alertar(ciudadano).alertaId(), "image/jpeg");
		ia.fallarAnalisisDe(foto.id(), "download_forbidden", Desenlace.FIRMAR_DE_NUEVO);

		analizador.barrer();

		assertThat(ia.analisisPedidos()).hasSize(2);
		assertThat(estadoDelTrabajo(foto)).isEqualTo("PENDIENTE");
	}

	@Test
	@DisplayName("PB-19 · CP-19-02 · un error de configuración del servicio no se reintenta solo: queda para que lo revise una persona")
	void errorDeConfiguracion() throws Exception {
		Ciudadano ciudadano = escenario.ciudadano();
		Evidencia foto = escenario.evidenciaSubida(ciudadano, escenario.alertar(ciudadano).alertaId(), "image/jpeg");
		ia.fallarAnalisisDe(foto.id(), "unauthorized", Desenlace.REVISION);

		analizador.barrer();

		assertThat(estadoDelTrabajo(foto)).isEqualTo("EN_REVISION");
		assertThat(estado(foto)).isEqualTo("SUBIDA");
	}

	@Test
	@DisplayName("PB-19 · CP-19-03 · cada versión nueva se avisa en vivo y por push a la tripulación que va, sin contar lo que dice")
	void avisoALaTripulacion() throws Exception {
		Ciudadano ciudadano = escenario.ciudadano();
		Alerta alerta = escenario.alertar(ciudadano);
		Paramedico paramedico = escenario.paramedicoEnTurno();
		escenario.recibirAvisos(paramedico, "push-tripulacion");
		escenario.tomar(paramedico, alerta.incidenteId());
		escenario.evidenciaSubida(ciudadano, alerta.alertaId(), "image/jpeg");

		correrLaIa();

		assertThat(publicaciones.versionesDeResumenPublicadas(alerta.incidenteId())).containsExactly(1);
		AvisoParaParamedico aviso = publicaciones.avisosAlParamedico("push-tripulacion").getLast();
		assertThat(aviso.datos()).containsEntry("tipo", "RESUMEN_IA")
				.containsEntry("incidenteId", String.valueOf(alerta.incidenteId()));
		assertThat(aviso.cuerpo()).doesNotContain("CAIDA");
		pedir(get("/incidentes/" + alerta.incidenteId() + "/resumen"), paramedico.token())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.version").value(1));
	}

	@Test
	@DisplayName("PB-19 · CP-19-04 · un archivo inutilizable queda fallido, sin análisis, y no aporta nada al resumen")
	void evidenciaInutilizable() throws Exception {
		Ciudadano ciudadano = escenario.ciudadano();
		Alerta alerta = escenario.alertar(ciudadano);
		Evidencia buena = escenario.evidenciaSubida(ciudadano, alerta.alertaId(), "image/jpeg");
		Evidencia mala = escenario.evidenciaSubida(ciudadano, alerta.alertaId(), "audio/mp4");
		ia.fallarAnalisisDe(mala.id(), "unreadable_media", Desenlace.DEFINITIVO);

		correrLaIa();

		assertThat(estado(buena)).isEqualTo("ANALIZADA");
		assertThat(estado(mala)).isEqualTo("FALLIDA");
		assertThat(jdbc.queryForObject("select count(*) from analisis_evidencia where evidencia_id = ?", Long.class,
				mala.id())).isZero();
		assertThat(ia.resumenesPedidos().getLast().evidencias())
				.extracting(PedidoDeResumen.EvidenciaAnalizada::evidenciaId)
				.containsExactly(buena.id());
		resumen(alerta.incidenteId())
				.andExpect(jsonPath("$.evidenciasUsadas.length()").value(1))
				.andExpect(jsonPath("$.evidencias[?(@.evidenciaId == " + mala.id() + ")].estado")
						.value(contains("FALLIDA")))
				.andExpect(jsonPath("$.evidencias[?(@.evidenciaId == " + mala.id() + ")].transcripcion")
						.value(contains((Object) null)));
	}

	private void correrLaIa() {
		analizador.barrer();
		resumidor.barrer();
	}

	private Alerta alertaConDescripcion(Ciudadano ciudadano, String descripcion) throws Exception {
		String respuesta = cuerpo(pedir(post("/alertas"), ciudadano.token(), Json.objeto("latitud", Escenario.LATITUD,
				"longitud", Escenario.LONGITUD, "origenUbicacion", "GPS", "descripcion", descripcion))
				.andExpect(status().isCreated()));
		return new Alerta(Json.numero(respuesta, "$.alertaId"), Json.numero(respuesta, "$.incidenteId"));
	}

	private ResultActions resumen(long incidente) throws Exception {
		return pedir(get("/incidentes/" + incidente + "/resumen"), escenario.admin()).andExpect(status().isOk());
	}

	private String estado(Evidencia evidencia) {
		return jdbc.queryForObject("select estado from evidencia where id = ?", String.class, evidencia.id());
	}

	private String estadoDelTrabajo(Evidencia evidencia) {
		return jdbc.queryForObject("select estado from trabajo_ia where evidencia_id = ?", String.class,
				evidencia.id());
	}

	private long trabajos(String tipo, String estado) {
		return jdbc.queryForObject("select count(*) from trabajo_ia where tipo = ? and estado = ?", Long.class, tipo,
				estado);
	}

}
