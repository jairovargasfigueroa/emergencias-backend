package com.uem.ambulancias.evidencias;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import com.uem.ambulancias.evidencias.service.AnalizadorDeEvidencias;
import com.uem.ambulancias.evidencias.service.FalloDelServicioIa.Desenlace;
import com.uem.ambulancias.evidencias.service.ResumidorDeIncidentes;
import com.uem.ambulancias.soporte.Escenario.Alerta;
import com.uem.ambulancias.soporte.Escenario.Ciudadano;
import com.uem.ambulancias.soporte.Escenario.Evidencia;
import com.uem.ambulancias.soporte.Escenario.Paramedico;
import com.uem.ambulancias.soporte.Json;
import com.uem.ambulancias.soporte.PruebaIT;

/** Lo que ve la central de un incidente con archivos: el resumen vigente, sus fuentes y cada archivo recibido. */
@Tag("PB-20")
class ResumenYEvidenciasIT extends PruebaIT {

	@Autowired
	private AnalizadorDeEvidencias analizador;

	@Autowired
	private ResumidorDeIncidentes resumidor;

	@Test
	@DisplayName("PB-20 · CP-20-01 · sin resumen todavía, lo dice, y muestra igual los archivos que ya llegaron")
	void sinResumen() throws Exception {
		Ciudadano ciudadano = escenario.ciudadano();
		Alerta alerta = escenario.alertar(ciudadano);
		Evidencia subida = escenario.evidenciaSubida(ciudadano, alerta.alertaId(), "image/jpeg");
		escenario.anunciarEvidencia(ciudadano, alerta.alertaId(), "image/jpeg", 100);

		resumen(alerta.incidenteId())
				.andExpect(jsonPath("$.incidenteId").value(alerta.incidenteId()))
				.andExpect(jsonPath("$.version").doesNotExist())
				.andExpect(jsonPath("$.resumen").doesNotExist())
				.andExpect(jsonPath("$.evidenciasUsadas.length()").value(0))
				.andExpect(jsonPath("$.evidencias.length()").value(1))
				.andExpect(jsonPath("$.evidencias[0].evidenciaId").value(subida.id()))
				.andExpect(jsonPath("$.evidencias[0].estado").value("SUBIDA"));
		pedir(get("/incidentes/999999/resumen"), escenario.admin()).andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("PB-20 · CP-20-01 · con resumen, se ve la versión vigente, su hora y sus puntos relevantes")
	void conResumen() throws Exception {
		Ciudadano ciudadano = escenario.ciudadano();
		Alerta alerta = escenario.alertar(ciudadano);
		escenario.evidenciaSubida(ciudadano, alerta.alertaId(), "audio/mp4");
		correrLaIa();

		resumen(alerta.incidenteId())
				.andExpect(jsonPath("$.version").value(1))
				.andExpect(jsonPath("$.generadoEn").isNotEmpty())
				.andExpect(jsonPath("$.resumen.eventType").value("CAIDA"))
				.andExpect(jsonPath("$.resumen.risks[0]").value("golpe en la cabeza"));
	}

	@Test
	@DisplayName("PB-20 · CP-20-02 · cada archivo con su tipo, su estado y lo que la IA escuchó en él")
	void archivos() throws Exception {
		Ciudadano ciudadano = escenario.ciudadano();
		Alerta alerta = escenario.alertar(ciudadano);
		Evidencia audio = escenario.evidenciaSubida(ciudadano, alerta.alertaId(), "audio/mp4");
		Evidencia foto = escenario.evidenciaSubida(ciudadano, alerta.alertaId(), "image/png");
		analizador.barrer();

		String archivo = "$.evidencias[?(@.evidenciaId == " + audio.id() + ")]";
		resumen(alerta.incidenteId())
				.andExpect(jsonPath("$.evidencias.length()").value(2))
				.andExpect(jsonPath(archivo + ".alertaId", contains((int) alerta.alertaId())))
				.andExpect(jsonPath(archivo + ".modalidad", contains("AUDIO")))
				.andExpect(jsonPath(archivo + ".estado", contains("ANALIZADA")))
				.andExpect(jsonPath(archivo + ".transcripcion", contains(containsString("pidiendo ayuda"))))
				.andExpect(jsonPath(archivo + ".lineaDeTiempo[1].segundo", contains(4.5)))
				.andExpect(jsonPath(archivo + ".lineaDeTiempo[1].texto", contains("Sirena")))
				.andExpect(jsonPath("$.evidencias[?(@.evidenciaId == " + foto.id() + ")].modalidad",
						contains("IMAGEN")));
	}

	@Test
	@DisplayName("PB-20 · CP-20-02 · la central abre cada archivo con una URL temporal, también el que la IA no pudo analizar")
	void urlTemporal() throws Exception {
		Ciudadano ciudadano = escenario.ciudadano();
		Alerta alerta = escenario.alertar(ciudadano);
		Evidencia foto = escenario.evidenciaSubida(ciudadano, alerta.alertaId(), "image/jpeg");
		Evidencia fallida = escenario.evidenciaSubida(ciudadano, alerta.alertaId(), "audio/mp4");
		ia.fallarAnalisisDe(fallida.id(), "unreadable_media",
				Desenlace.DEFINITIVO);
		analizador.barrer();

		String respuesta = cuerpo(url(foto.id(), escenario.admin())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.url").value(containsString("firma=lectura"))));
		Instant vence = Instant.parse(Json.texto(respuesta, "$.venceEn"));
		assertThat(Duration.between(Instant.now(), vence)).isBetween(Duration.ofMinutes(9), Duration.ofMinutes(10));
		url(fallida.id(), escenario.admin()).andExpect(status().isOk());
	}

	@Test
	@DisplayName("PB-20 · CP-20-02 · un archivo que todavía no llegó no se puede abrir")
	void archivoSinSubir() throws Exception {
		Ciudadano ciudadano = escenario.ciudadano();
		Evidencia pendiente = escenario.anunciarEvidencia(ciudadano, escenario.alertar(ciudadano).alertaId(),
				"image/jpeg", 100);

		url(pendiente.id(), escenario.admin()).andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("PB-20 · CP-20-02 · el paramédico ve el resumen y los archivos solo mientras su unidad tiene el caso")
	void accesoDelParamedico() throws Exception {
		Ciudadano ciudadano = escenario.ciudadano();
		Alerta alerta = escenario.alertar(ciudadano);
		Evidencia foto = escenario.evidenciaSubida(ciudadano, alerta.alertaId(), "image/jpeg");
		Paramedico atiende = escenario.paramedicoEnTurno();
		Paramedico otro = escenario.paramedicoEnTurno();
		long atencion = escenario.tomar(atiende, alerta.incidenteId());

		url(foto.id(), atiende.token()).andExpect(status().isOk());
		pedir(get("/incidentes/" + alerta.incidenteId() + "/resumen"), atiende.token()).andExpect(status().isOk());
		url(foto.id(), otro.token())
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.codigo").value("SIN_ATENCION_EN_INCIDENTE"));
		pedir(get("/incidentes/" + alerta.incidenteId() + "/resumen"), otro.token())
				.andExpect(status().isForbidden());

		escenario.marcar(atiende, atencion, "llegada");
		pedir(post("/atenciones/" + atencion + "/sin-traslado"), atiende.token(), Json.objeto("latitud", -17.78,
				"longitud", -63.18, "motivo", "ATENDIDO_EN_EL_LUGAR"))
				.andExpect(status().isOk());
		escenario.liberar(atiende, atencion);
		url(foto.id(), atiende.token()).andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("PB-20 · CP-20-02 · el ciudadano no abre el resumen ni los archivos desde estas rutas")
	void ciudadanoNo() throws Exception {
		Ciudadano ciudadano = escenario.ciudadano();
		Alerta alerta = escenario.alertar(ciudadano);
		Evidencia foto = escenario.evidenciaSubida(ciudadano, alerta.alertaId(), "image/jpeg");

		url(foto.id(), ciudadano.token()).andExpect(status().isForbidden());
		pedir(get("/incidentes/" + alerta.incidenteId() + "/resumen"), ciudadano.token())
				.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("PB-20 · CP-20-03 · las fuentes separan lo generado por la IA del material que mandó la gente")
	void fuentes() throws Exception {
		Ciudadano ciudadano = escenario.ciudadano();
		Alerta alerta = escenario.alertar(ciudadano);
		Evidencia foto = escenario.evidenciaSubida(ciudadano, alerta.alertaId(), "image/jpeg");
		escenario.anunciarEvidencia(ciudadano, alerta.alertaId(), "audio/mp4", 100);
		correrLaIa();

		String respuesta = cuerpo(resumen(alerta.incidenteId())
				.andExpect(jsonPath("$.metodo").value("LLM"))
				.andExpect(jsonPath("$.versionPrompt").value("prompt-1"))
				.andExpect(jsonPath("$.alertasUsadas[0]").value(alerta.alertaId())));
		List<Integer> usadas = Json.leer(respuesta, "$.evidenciasUsadas");
		List<Integer> recibidas = Json.leer(respuesta, "$.evidencias[*].evidenciaId");
		assertThat(usadas).containsExactly((int) foto.id());
		assertThat(recibidas).containsAll(usadas);
	}

	@Test
	@DisplayName("PB-20 · CP-20-04 · una versión nueva no borra la anterior ni cambia el historial del incidente")
	void versionNuevaSinTocarElHistorial() throws Exception {
		Ciudadano ciudadano = escenario.ciudadano();
		Alerta alerta = escenario.alertar(ciudadano);
		Paramedico paramedico = escenario.paramedicoEnTurno();
		escenario.tomar(paramedico, alerta.incidenteId());
		escenario.evidenciaSubida(ciudadano, alerta.alertaId(), "image/jpeg");
		correrLaIa();
		String antes = cuerpo(pedir(get("/incidentes/" + alerta.incidenteId()), escenario.admin()));

		escenario.evidenciaSubida(ciudadano, alerta.alertaId(), "audio/mp4");
		correrLaIa();

		resumen(alerta.incidenteId()).andExpect(jsonPath("$.version").value(2));
		List<Map<String, Object>> versiones = jdbc.queryForList(
				"select version, cardinality(evidencias_usadas) as usadas from resumen_incidente "
						+ "where incidente_id = ? order by version",
				alerta.incidenteId());
		assertThat(versiones).extracting(fila -> fila.get("version")).containsExactly(1, 2);
		assertThat(versiones).extracting(fila -> fila.get("usadas")).containsExactly(1, 2);
		String despues = cuerpo(pedir(get("/incidentes/" + alerta.incidenteId()), escenario.admin()));
		assertThat(despues).isEqualTo(antes);
	}

	private void correrLaIa() {
		analizador.barrer();
		resumidor.barrer();
	}

	private ResultActions resumen(long incidente) throws Exception {
		return pedir(get("/incidentes/" + incidente + "/resumen"), escenario.admin()).andExpect(status().isOk());
	}

	private ResultActions url(long evidencia, String token) throws Exception {
		return pedir(get("/evidencias/" + evidencia + "/url"), token);
	}

}
