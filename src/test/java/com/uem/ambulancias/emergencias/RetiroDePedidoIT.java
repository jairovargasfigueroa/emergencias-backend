package com.uem.ambulancias.emergencias;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.ResultActions;

import com.uem.ambulancias.emergencias.domain.EstadoIncidente;
import com.uem.ambulancias.soporte.Escenario.Alerta;
import com.uem.ambulancias.soporte.Escenario.Ciudadano;
import com.uem.ambulancias.soporte.Escenario.Paramedico;
import com.uem.ambulancias.soporte.Json;
import com.uem.ambulancias.soporte.PruebaIT;

@Tag("PB-11")
class RetiroDePedidoIT extends PruebaIT {

	@ParameterizedTest(name = "motivo {0}")
	@ValueSource(strings = { "YA_FUE_ATENDIDO", "FALSA_ALARMA", "ERROR", "OTRO" })
	@DisplayName("PB-11 · CP-11-01 · el emisor retira su pedido con cualquiera de los motivos admitidos")
	void motivosAdmitidos(String motivo) throws Exception {
		Ciudadano ciudadano = escenario.ciudadano();
		Alerta alerta = escenario.alertar(ciudadano);

		retirar(ciudadano, alerta, Json.objeto("motivo", motivo))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.alertaId").value(alerta.alertaId()));

		incidente(alerta.incidenteId()).andExpect(jsonPath("$.alertas[0].motivoCancelacion").value(motivo));
	}

	@Test
	@DisplayName("PB-11 · CP-11-01 · sin motivo, con un motivo inventado o sobre una alerta ajena no se retira")
	void retiroInvalido() throws Exception {
		Ciudadano ciudadano = escenario.ciudadano();
		Alerta alerta = escenario.alertar(ciudadano);

		retirar(ciudadano, alerta, Json.objeto("emisorEsPaciente", true))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value("VALIDACION"));
		retirar(ciudadano, alerta, Json.objeto("motivo", "ME_ARREPENTI")).andExpect(status().isBadRequest());
		retirar(escenario.ciudadano(), alerta, Json.objeto("motivo", "ERROR"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("ALERTA_AJENA"));
		pedir(post("/alertas/" + alerta.alertaId() + "/cancelacion"), escenario.paramedicoActivado().token(),
				Json.objeto("motivo", "ERROR"))
				.andExpect(status().isForbidden());

		incidente(alerta.incidenteId())
				.andExpect(jsonPath("$.estado").value("ACTIVO"))
				.andExpect(jsonPath("$.alertas[0].estado").value("VINCULADA"));
	}

	@Test
	@DisplayName("PB-11 · CP-11-02 · con una unidad en camino todavía se puede retirar; cuando llegó, ya no")
	void hastaLaLlegada() throws Exception {
		Ciudadano ciudadano = escenario.ciudadano();
		Alerta alerta = escenario.alertar(ciudadano);
		Ciudadano vecino = escenario.ciudadano();
		Alerta otra = escenario.alertar(vecino);
		Paramedico paramedico = escenario.paramedicoEnTurno();
		long atencion = escenario.tomar(paramedico, alerta.incidenteId());

		retirar(ciudadano, alerta, Json.objeto("motivo", "YA_FUE_ATENDIDO")).andExpect(status().isOk());

		escenario.marcar(paramedico, atencion, "llegada");
		retirar(vecino, otra, Json.objeto("motivo", "YA_FUE_ATENDIDO"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("TRANSICION_INVALIDA"));
	}

	@Test
	@DisplayName("PB-11 · CP-11-03 · si otra persona también pidió, retirar una alerta no cierra el incidente")
	void otroPedidoSigue() throws Exception {
		Ciudadano primero = escenario.ciudadano();
		Alerta alerta = escenario.alertar(primero);
		escenario.alertar(escenario.ciudadano());

		retirar(primero, alerta, Json.objeto("motivo", "ERROR")).andExpect(status().isOk());

		incidente(alerta.incidenteId())
				.andExpect(jsonPath("$.estado").value("ACTIVO"))
				.andExpect(jsonPath("$.alertas[0].estado").value("CANCELADA"))
				.andExpect(jsonPath("$.alertas[1].estado").value("VINCULADA"));
		assertThat(publicaciones.fueRetirado(alerta.incidenteId())).isFalse();
	}

	@Test
	@DisplayName("PB-11 · CP-11-03 · si nadie más pidió y nadie salió, el incidente se cancela y deja de ofrecerse")
	void nadieMasCancela() throws Exception {
		Ciudadano ciudadano = escenario.ciudadano();
		Alerta alerta = escenario.alertar(ciudadano);

		retirar(ciudadano, alerta, Json.objeto("motivo", "FALSA_ALARMA")).andExpect(status().isOk());

		incidente(alerta.incidenteId())
				.andExpect(jsonPath("$.estado").value("CANCELADO"))
				.andExpect(jsonPath("$.fechaHoraCierre").isNotEmpty());
		assertThat(publicaciones.fueRetirado(alerta.incidenteId())).isTrue();
		assertThat(publicaciones.ultimoSeguimiento(alerta.incidenteId()).estado())
				.isEqualTo(EstadoIncidente.CANCELADO);
		retirar(ciudadano, alerta, Json.objeto("motivo", "ERROR"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("TRANSICION_INVALIDA"));
	}

	@Test
	@DisplayName("PB-11 · CP-11-04 · con la unidad en camino el incidente sigue, y la tripulación se entera de que ya nadie espera")
	void laUnidadSeEntera() throws Exception {
		Ciudadano ciudadano = escenario.ciudadano();
		Alerta alerta = escenario.alertar(ciudadano);
		Paramedico paramedico = escenario.paramedicoEnTurno();
		escenario.tomar(paramedico, alerta.incidenteId());
		pedir(get("/paramedicos/actual/atencion"), paramedico.token())
				.andExpect(jsonPath("$.emisoresCancelaron").value(false));

		retirar(ciudadano, alerta, Json.objeto("motivo", "YA_FUE_ATENDIDO")).andExpect(status().isOk());

		incidente(alerta.incidenteId()).andExpect(jsonPath("$.estado").value("EN_ATENCION"));
		pedir(get("/paramedicos/actual/atencion"), paramedico.token())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("EN_CAMINO"))
				.andExpect(jsonPath("$.emisoresCancelaron").value(true));
	}

	@Test
	@DisplayName("PB-11 · CP-11-04 · la central ve el retiro con su motivo, su hora y si quien avisó era el paciente")
	void registroParaLaCentral() throws Exception {
		Ciudadano ciudadano = escenario.ciudadano();
		Alerta alerta = escenario.alertar(ciudadano);
		escenario.alertar(escenario.ciudadano());
		int publicadas = publicaciones.incidentesAbiertos(alerta.incidenteId()).size();

		retirar(ciudadano, alerta, Json.objeto("motivo", "YA_FUE_ATENDIDO", "emisorEsPaciente", false))
				.andExpect(status().isOk());

		incidente(alerta.incidenteId())
				.andExpect(jsonPath("$.alertas[0].estado").value("CANCELADA"))
				.andExpect(jsonPath("$.alertas[0].motivoCancelacion").value("YA_FUE_ATENDIDO"))
				.andExpect(jsonPath("$.alertas[0].horaCancelacion").isNotEmpty())
				.andExpect(jsonPath("$.alertas[0].emisorEsPaciente").value(false));
		assertThat(publicaciones.incidentesAbiertos(alerta.incidenteId())).hasSize(publicadas + 1);
	}

	private ResultActions retirar(Ciudadano ciudadano, Alerta alerta, String cuerpo) throws Exception {
		return pedir(post("/alertas/" + alerta.alertaId() + "/cancelacion"), ciudadano.token(), cuerpo);
	}

	private ResultActions incidente(long id) throws Exception {
		return pedir(get("/incidentes/" + id), escenario.admin()).andExpect(status().isOk());
	}

}
