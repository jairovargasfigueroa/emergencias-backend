package com.uem.ambulancias.emergencias;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import com.uem.ambulancias.emergencias.service.IncidentePublicado;
import com.uem.ambulancias.soporte.Escenario.Alerta;
import com.uem.ambulancias.soporte.Escenario.Ciudadano;
import com.uem.ambulancias.soporte.Escenario.Paramedico;
import com.uem.ambulancias.soporte.Json;
import com.uem.ambulancias.soporte.PruebaIT;

@Tag("PB-10")
class DetallesDeAlertaIT extends PruebaIT {

	@Test
	@DisplayName("PB-10 · CP-10-01 · la alerta sale y se avisa a las unidades antes de cualquier pregunta; los datos llegan después")
	void primeroLaAlerta() throws Exception {
		Paramedico paramedico = escenario.paramedicoEnTurno();
		escenario.recibirAvisos(paramedico, "push-unidad");
		Ciudadano ciudadano = escenario.ciudadano();

		Alerta alerta = escenario.alertar(ciudadano);
		assertThat(publicaciones.ultimoAvisoDeIncidenteNuevo()).containsExactly("push-unidad");

		detalles(ciudadano, alerta, Json.objeto("cantidadAfectados", 2, "descripcion", "Choque entre dos autos"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.alertaId").value(alerta.alertaId()))
				.andExpect(jsonPath("$.incidenteId").value(alerta.incidenteId()));
	}

	@Test
	@DisplayName("PB-10 · CP-10-02 · completa o corrige los datos; lo que no manda queda como estaba")
	void completarYCorregir() throws Exception {
		Ciudadano ciudadano = escenario.ciudadano();
		Alerta alerta = escenario.alertar(ciudadano);

		detalles(ciudadano, alerta, Json.objeto("cantidadAfectados", 3, "descripcion", "Hay un herido"))
				.andExpect(status().isOk());
		detalles(ciudadano, alerta, Json.objeto("descripcion", "Hay un herido grave, no respira bien"))
				.andExpect(status().isOk());
		detalles(ciudadano, alerta, Json.objeto("cantidadAfectados", 2, "descripcion", "   "))
				.andExpect(status().isOk());

		incidente(alerta.incidenteId())
				.andExpect(jsonPath("$.alertas[0].cantidadAfectados").value(2))
				.andExpect(jsonPath("$.alertas[0].descripcion").value("Hay un herido grave, no respira bien"));
	}

	@Test
	@DisplayName("PB-10 · CP-10-03 · el total de afectados del incidente es el mayor reportado: bajar uno no lo baja")
	void totalDelIncidente() throws Exception {
		Ciudadano primero = escenario.ciudadano();
		Ciudadano segundo = escenario.ciudadano();
		Alerta alerta = escenario.alertar(primero);
		Alerta otra = escenario.alertar(segundo);

		detalles(primero, alerta, Json.objeto("cantidadAfectados", 4)).andExpect(status().isOk());
		detalles(segundo, otra, Json.objeto("cantidadAfectados", 2)).andExpect(status().isOk());
		detalles(primero, alerta, Json.objeto("cantidadAfectados", 1)).andExpect(status().isOk());

		incidente(alerta.incidenteId()).andExpect(jsonPath("$.cantidadAfectados").value(4));
	}

	@Test
	@DisplayName("PB-10 · CP-10-03 · los datos le llegan a la tripulación: en lo publicado y en su atención")
	void llegaALaTripulacion() throws Exception {
		Ciudadano ciudadano = escenario.ciudadano();
		Alerta alerta = escenario.alertar(ciudadano);
		Paramedico paramedico = escenario.paramedicoEnTurno();
		escenario.tomar(paramedico, alerta.incidenteId());

		detalles(ciudadano, alerta, Json.objeto("cantidadAfectados", 2, "descripcion", "Son dos, una embarazada"))
				.andExpect(status().isOk());

		IncidentePublicado publicado = publicaciones.incidentesAbiertos(alerta.incidenteId()).getLast();
		assertThat(publicado.cantidadAfectados()).isEqualTo(2);
		assertThat(publicado.descripciones()).containsExactly("Son dos, una embarazada");
		pedir(get("/paramedicos/actual/atencion"), paramedico.token())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.incidente.cantidadAfectados").value(2))
				.andExpect(jsonPath("$.incidente.descripciones[0]").value("Son dos, una embarazada"));
	}

	@Test
	@DisplayName("PB-10 · CP-10-04 · otro ciudadano no toca los datos de una alerta ajena, ni la tripulación ni la central")
	void soloElEmisor() throws Exception {
		Alerta alerta = escenario.alertar(escenario.ciudadano());
		String cuerpo = Json.objeto("cantidadAfectados", 9);

		detalles(escenario.ciudadano(), alerta, cuerpo)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("ALERTA_AJENA"));
		pedir(post("/alertas/" + alerta.alertaId() + "/detalles"), escenario.paramedicoActivado().token(), cuerpo)
				.andExpect(status().isForbidden());
		pedir(post("/alertas/" + alerta.alertaId() + "/detalles"), escenario.admin(), cuerpo)
				.andExpect(status().isForbidden());
		incidente(alerta.incidenteId()).andExpect(jsonPath("$.alertas[0].cantidadAfectados").doesNotExist());
	}

	@Test
	@DisplayName("PB-10 · CP-10-05 · cuando la unidad llegó o el incidente se cerró, la alerta ya no admite cambios")
	void yaNoAdmiteCambios() throws Exception {
		Ciudadano ciudadano = escenario.ciudadano();
		Alerta alerta = escenario.alertar(ciudadano);
		Paramedico paramedico = escenario.paramedicoEnTurno();
		escenario.marcar(paramedico, escenario.tomar(paramedico, alerta.incidenteId()), "llegada");

		detalles(ciudadano, alerta, Json.objeto("cantidadAfectados", 2))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("DETALLES_NO_EDITABLES"));

		Ciudadano otro = escenario.ciudadano();
		Alerta retirada = escenario.alertar(otro, -17.80, -63.20);
		pedir(post("/alertas/" + retirada.alertaId() + "/cancelacion"), otro.token(), Json.objeto("motivo", "ERROR"))
				.andExpect(status().isOk());
		detalles(otro, retirada, Json.objeto("cantidadAfectados", 2))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("DETALLES_NO_EDITABLES"));
	}

	@Test
	@DisplayName("PB-10 · CP-10-06 · afectados negativos, una descripción enorme o una alerta que no existe se rechazan")
	void datosInvalidos() throws Exception {
		Ciudadano ciudadano = escenario.ciudadano();
		Alerta alerta = escenario.alertar(ciudadano);

		detalles(ciudadano, alerta, Json.objeto("cantidadAfectados", -1))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value("VALIDACION"));
		detalles(ciudadano, alerta, Json.objeto("descripcion", "x".repeat(2001))).andExpect(status().isBadRequest());
		pedir(post("/alertas/999999/detalles"), ciudadano.token(), Json.objeto("cantidadAfectados", 1))
				.andExpect(status().isNotFound());
	}

	private ResultActions detalles(Ciudadano ciudadano, Alerta alerta, String cuerpo) throws Exception {
		return pedir(post("/alertas/" + alerta.alertaId() + "/detalles"), ciudadano.token(), cuerpo);
	}

	private ResultActions incidente(long id) throws Exception {
		return pedir(get("/incidentes/" + id), escenario.admin()).andExpect(status().isOk());
	}

}
