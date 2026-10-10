package com.uem.ambulancias.flujos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.uem.ambulancias.evidencias.service.AnalizadorDeEvidencias;
import com.uem.ambulancias.evidencias.service.FalloDelServicioIa.Desenlace;
import com.uem.ambulancias.evidencias.service.ResumidorDeIncidentes;
import com.uem.ambulancias.soporte.Escenario;
import com.uem.ambulancias.soporte.Escenario.Alerta;
import com.uem.ambulancias.soporte.Escenario.Ciudadano;
import com.uem.ambulancias.soporte.Escenario.Evidencia;
import com.uem.ambulancias.soporte.Escenario.Paramedico;
import com.uem.ambulancias.soporte.Json;
import com.uem.ambulancias.soporte.PruebaIT;

/**
 * El Sprint 3 de punta a punta: lo que manda el ciudadano mientras espera llega analizado y resumido a la unidad que
 * va y a la central, y el caso queda en el historial.
 */
@Tag("flujo")
@Tag("sprint-3")
class EvidenciaYResumenIT extends PruebaIT {

	@Autowired
	private AnalizadorDeEvidencias analizador;

	@Autowired
	private ResumidorDeIncidentes resumidor;

	@Test
	@DisplayName("Flujo Sprint 3 · PB-18 a PB-21 · foto y audio de la escena, resumen para la unidad que va y para la central, y el caso en el historial")
	void evidenciasYResumen() throws Exception {
		Paramedico paramedico = escenario.paramedicoEnTurno();
		escenario.recibirAvisos(paramedico, "push-tripulacion");
		Ciudadano ciudadano = escenario.ciudadano();
		Alerta alerta = escenario.alertar(ciudadano);
		long incidente = alerta.incidenteId();
		long atencion = escenario.tomar(paramedico, incidente);

		// PB-18: mientras espera, manda una foto y un audio de la escena.
		Evidencia foto = escenario.evidenciaSubida(ciudadano, alerta.alertaId(), "image/jpeg");
		Evidencia audio = escenario.evidenciaSubida(ciudadano, alerta.alertaId(), "audio/mp4");

		// PB-19: la IA los analiza y arma el resumen; a la unidad que va le llega el aviso y lo lee.
		analizador.barrer();
		resumidor.barrer();
		assertThat(publicaciones.avisosAlParamedico("push-tripulacion").getLast().datos())
				.containsEntry("tipo", "RESUMEN_IA");
		pedir(get("/incidentes/" + incidente + "/resumen"), paramedico.token())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.version").value(1))
				.andExpect(jsonPath("$.resumen.suggestedSeverity").value("ALTA"))
				.andExpect(jsonPath("$.evidenciasUsadas.length()").value(2));
		pedir(get("/evidencias/" + audio.id() + "/url"), paramedico.token()).andExpect(status().isOk());

		// PB-20: la central ve el mismo resumen, sus fuentes y los dos archivos, y puede abrirlos.
		pedir(get("/incidentes/" + incidente + "/resumen"), escenario.admin())
				.andExpect(jsonPath("$.version").value(1))
				.andExpect(jsonPath("$.evidencias.length()").value(2));
		pedir(get("/evidencias/" + foto.id() + "/url"), escenario.admin()).andExpect(status().isOk());

		// La unidad termina el caso y se libera: deja de ver los archivos; la central los sigue viendo.
		escenario.hastaElHospital(paramedico, atencion);
		pedir(post("/atenciones/" + atencion + "/entrega"), paramedico.token(),
				Json.objeto("latitud", Escenario.LATITUD, "longitud", Escenario.LONGITUD))
				.andExpect(status().isOk());
		escenario.liberar(paramedico, atencion);
		pedir(get("/evidencias/" + foto.id() + "/url"), paramedico.token()).andExpect(status().isForbidden());
		pedir(get("/incidentes/" + incidente + "/resumen"), escenario.admin())
				.andExpect(jsonPath("$.version").value(1));

		// PB-21: el caso queda en el historial, cerrado y con su atención completa.
		pedir(get("/incidentes").param("estado", "CERRADOS"), escenario.admin())
				.andExpect(jsonPath("$.contenido[0].id").value(incidente))
				.andExpect(jsonPath("$.contenido[0].estado").value("ATENDIDO"));
		pedir(get("/incidentes/" + incidente), escenario.admin())
				.andExpect(jsonPath("$.atenciones[0].estado").value("PACIENTE_ENTREGADO"))
				.andExpect(jsonPath("$.atenciones[0].horaLiberacion").isNotEmpty());
	}

	@Test
	@DisplayName("Flujo Sprint 3 · PB-18 y PB-19 · con la IA caída y un audio inutilizable, la ayuda llega igual y el resumen sale con lo que sirve")
	void laIaNoFrenaNada() throws Exception {
		Ciudadano ciudadano = escenario.ciudadano();
		Alerta alerta = escenario.alertar(ciudadano);
		Evidencia foto = escenario.evidenciaSubida(ciudadano, alerta.alertaId(), "image/jpeg");
		Evidencia audio = escenario.evidenciaSubida(ciudadano, alerta.alertaId(), "audio/mp4");
		ia.fallarAnalisisDe(foto.id(), "provider_unavailable", Desenlace.REINTENTAR);
		ia.fallarAnalisisDe(audio.id(), "unreadable_media", Desenlace.DEFINITIVO);

		// La IA no responde: nada queda esperando, una unidad toma el caso y llega.
		analizador.barrer();
		Paramedico paramedico = escenario.paramedicoEnTurno();
		escenario.marcar(paramedico, escenario.tomar(paramedico, alerta.incidenteId()), "llegada");

		// Vuelve la IA: en el reintento la foto se analiza; el audio inutilizable no se vuelve a intentar.
		ia.olvidar();
		jdbc.update("update trabajo_ia set proximo_intento = now() where estado = 'PENDIENTE'");
		analizador.barrer();
		resumidor.barrer();

		assertThat(ia.analisisPedidos()).singleElement()
				.satisfies(pedido -> assertThat(pedido.evidenciaId()).isEqualTo(foto.id()));
		pedir(get("/incidentes/" + alerta.incidenteId() + "/resumen"), escenario.admin())
				.andExpect(jsonPath("$.version").value(1))
				.andExpect(jsonPath("$.evidenciasUsadas.length()").value(1))
				.andExpect(jsonPath("$.evidenciasUsadas[0]").value(foto.id()));
	}

}
