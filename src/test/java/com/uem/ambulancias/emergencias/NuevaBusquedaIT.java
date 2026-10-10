package com.uem.ambulancias.emergencias;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.uem.ambulancias.emergencias.domain.EstadoIncidente;
import com.uem.ambulancias.emergencias.service.IncidentePublicado;
import com.uem.ambulancias.soporte.Escenario;
import com.uem.ambulancias.soporte.Escenario.Alerta;
import com.uem.ambulancias.soporte.Escenario.Ciudadano;
import com.uem.ambulancias.soporte.Escenario.Paramedico;
import com.uem.ambulancias.soporte.Json;
import com.uem.ambulancias.soporte.PruebaIT;

@Tag("PB-14")
class NuevaBusquedaIT extends PruebaIT {

	@Test
	@DisplayName("PB-14 · CP-14-01 · si la única unidad lo deja y alguien sigue esperando, el incidente vuelve a activo")
	void vuelveAActivo() throws Exception {
		long incidente = escenario.alertar(escenario.ciudadano()).incidenteId();
		Paramedico paramedico = escenario.paramedicoEnTurno();
		long atencion = escenario.tomar(paramedico, incidente);

		cancelar(paramedico, atencion, "DESVIADA");

		pedir(get("/incidentes/" + incidente), escenario.admin())
				.andExpect(jsonPath("$.estado").value("ACTIVO"))
				.andExpect(jsonPath("$.unidadesAcudiendo").value(0))
				.andExpect(jsonPath("$.fechaHoraCierre").doesNotExist());
	}

	@Test
	@DisplayName("PB-14 · CP-14-02 · se vuelve a ofrecer a las unidades libres, con push, y el ciudadano ve que se busca otra")
	void seVuelveAOfrecer() throws Exception {
		Ciudadano ciudadano = escenario.ciudadano();
		escenario.recibirAvisos(ciudadano, "push-ciudadano");
		long incidente = escenario.alertar(ciudadano).incidenteId();
		Paramedico seVa = escenario.paramedicoEnTurno();
		Paramedico libre = escenario.paramedicoEnTurno();
		escenario.reportarPosicion(libre, Escenario.LATITUD + 0.01, Escenario.LONGITUD);
		escenario.recibirAvisos(libre, "push-libre");
		long atencion = escenario.tomar(seVa, incidente);
		int avisos = publicaciones.cantidadDeAvisosDeIncidenteNuevo();

		cancelar(seVa, atencion, "AVERIA");

		assertThat(publicaciones.cantidadDeAvisosDeIncidenteNuevo()).isEqualTo(avisos + 1);
		assertThat(publicaciones.ultimoAvisoDeIncidenteNuevo()).containsExactly("push-libre");
		IncidentePublicado publicado = publicaciones.incidentesAbiertos(incidente).getLast();
		assertThat(publicado.estado()).isEqualTo(EstadoIncidente.ACTIVO);
		assertThat(publicado.unidadesAcudiendo()).isZero();
		assertThat(publicaciones.ultimoSeguimiento(incidente).estado()).isEqualTo(EstadoIncidente.ACTIVO);
		assertThat(publicaciones.avisosAlCiudadano("push-ciudadano").getLast().datos())
				.containsEntry("tipo", "BUSCANDO_OTRA_UNIDAD");
	}

	@Test
	@DisplayName("PB-14 · CP-14-03 · si otra unidad sigue yendo, no se reinicia la búsqueda ni se toca su atención")
	void otraUnidadSigue() throws Exception {
		Ciudadano ciudadano = escenario.ciudadano();
		escenario.recibirAvisos(ciudadano, "push-ciudadano");
		long incidente = escenario.alertar(ciudadano).incidenteId();
		Paramedico seVa = escenario.paramedicoEnTurno();
		Paramedico sigue = escenario.paramedicoEnTurno();
		long atencionSeVa = escenario.tomar(seVa, incidente);
		pedir(post("/incidentes/" + incidente + "/sumarse"), sigue.token()).andExpect(status().isCreated());
		int avisos = publicaciones.cantidadDeAvisosDeIncidenteNuevo();

		cancelar(seVa, atencionSeVa, "DESVIADA");

		pedir(get("/incidentes/" + incidente), escenario.admin())
				.andExpect(jsonPath("$.estado").value("EN_ATENCION"))
				.andExpect(jsonPath("$.unidadesAcudiendo").value(1))
				.andExpect(jsonPath("$.atenciones[1].estado").value("EN_CAMINO"));
		pedir(get("/paramedicos/actual/atencion"), sigue.token()).andExpect(jsonPath("$.estado").value("EN_CAMINO"));
		assertThat(publicaciones.cantidadDeAvisosDeIncidenteNuevo()).isEqualTo(avisos);
		assertThat(publicaciones.avisosAlCiudadano("push-ciudadano"))
				.extracting(aviso -> aviso.datos().get("tipo"))
				.doesNotContain("BUSCANDO_OTRA_UNIDAD");
	}

	@Test
	@DisplayName("PB-14 · CP-14-04 · la atención que se cayó queda en el historial junto a la de la unidad que tomó la posta")
	void historialConservado() throws Exception {
		long incidente = escenario.alertar(escenario.ciudadano()).incidenteId();
		Paramedico primera = escenario.paramedicoEnTurno();
		Paramedico segunda = escenario.paramedicoEnTurno();
		cancelar(primera, escenario.tomar(primera, incidente), "AVERIA");

		escenario.tomar(segunda, incidente);

		pedir(get("/incidentes/" + incidente), escenario.admin())
				.andExpect(jsonPath("$.estado").value("EN_ATENCION"))
				.andExpect(jsonPath("$.atenciones.length()").value(2))
				.andExpect(jsonPath("$.atenciones[0].ambulanciaId").value(primera.ambulanciaId()))
				.andExpect(jsonPath("$.atenciones[0].estado").value("CANCELADA"))
				.andExpect(jsonPath("$.atenciones[0].motivoCancelacion").value("AVERIA"))
				.andExpect(jsonPath("$.atenciones[1].ambulanciaId").value(segunda.ambulanciaId()))
				.andExpect(jsonPath("$.atenciones[1].estado").value("EN_CAMINO"));
	}

	@Test
	@DisplayName("PB-14 · CP-14-05 · si todos retiraron su pedido y la unidad se vuelve, no se busca otra: se cancela")
	void nadieEspera() throws Exception {
		Ciudadano ciudadano = escenario.ciudadano();
		Alerta alerta = escenario.alertar(ciudadano);
		Paramedico paramedico = escenario.paramedicoEnTurno();
		escenario.recibirAvisos(escenario.paramedicoEnTurno(), "push-otra-unidad");
		long atencion = escenario.tomar(paramedico, alerta.incidenteId());
		pedir(post("/alertas/" + alerta.alertaId() + "/cancelacion"), ciudadano.token(),
				Json.objeto("motivo", "YA_FUE_ATENDIDO"))
				.andExpect(status().isOk());
		int avisos = publicaciones.cantidadDeAvisosDeIncidenteNuevo();

		cancelar(paramedico, atencion, "DESVIADA");

		pedir(get("/incidentes/" + alerta.incidenteId()), escenario.admin())
				.andExpect(jsonPath("$.estado").value("CANCELADO"));
		assertThat(publicaciones.cantidadDeAvisosDeIncidenteNuevo()).isEqualTo(avisos);
		assertThat(publicaciones.fueRetirado(alerta.incidenteId())).isTrue();
	}

	private void cancelar(Paramedico paramedico, long atencion, String motivo) throws Exception {
		pedir(post("/atenciones/" + atencion + "/cancelar"), paramedico.token(), Json.objeto("motivo", motivo))
				.andExpect(status().isOk());
	}

}
