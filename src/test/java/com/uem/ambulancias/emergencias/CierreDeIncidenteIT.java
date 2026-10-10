package com.uem.ambulancias.emergencias;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.web.servlet.ResultActions;

import com.uem.ambulancias.emergencias.service.AvisoParaCiudadano;
import com.uem.ambulancias.soporte.Escenario.Ciudadano;
import com.uem.ambulancias.soporte.Escenario.Paramedico;
import com.uem.ambulancias.soporte.Json;
import com.uem.ambulancias.soporte.PruebaIT;

@Tag("PB-17")
class CierreDeIncidenteIT extends PruebaIT {

	@ParameterizedTest(name = "{0} → {1}")
	@CsvSource({
			"FALSA_ALARMA_VERIFICADA, FALSA_ALARMA",
			"ATENDIDO_EXTERNAMENTE, ATENDIDO_EXTERNAMENTE",
			"SIN_COBERTURA, CANCELADO",
			"OTRO, CANCELADO" })
	@DisplayName("PB-17 · CP-17-01 · la central cierra con un motivo, y queda el motivo, la hora y quién lo cerró")
	void cerrar(String motivo, String estado) throws Exception {
		long incidente = escenario.alertar(escenario.ciudadano()).incidenteId();

		cerrar(incidente, motivo).andExpect(status().isNoContent());

		pedir(get("/incidentes/" + incidente), escenario.admin())
				.andExpect(jsonPath("$.estado").value(estado))
				.andExpect(jsonPath("$.motivoCierre").value(motivo))
				.andExpect(jsonPath("$.fechaHoraCierre").isNotEmpty())
				.andExpect(jsonPath("$.cerradoPor").value(startsWith("Administrador")));
	}

	@Test
	@DisplayName("PB-17 · CP-17-02 · sin motivo o con uno que no existe no se cierra")
	void sinMotivo() throws Exception {
		long incidente = escenario.alertar(escenario.ciudadano()).incidenteId();

		pedir(post("/incidentes/" + incidente + "/cierre"), escenario.admin(), Json.objeto("motivo", null))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value("VALIDACION"));
		cerrar(incidente, "PORQUE_SI").andExpect(status().isBadRequest());

		pedir(get("/incidentes/" + incidente), escenario.admin()).andExpect(jsonPath("$.estado").value("ACTIVO"));
	}

	@Test
	@DisplayName("PB-17 · CP-17-03 · con una unidad trabajando no se cierra; cuando ya no va nadie, sí")
	void conUnidadTrabajando() throws Exception {
		long incidente = escenario.alertar(escenario.ciudadano()).incidenteId();
		Paramedico paramedico = escenario.paramedicoEnTurno();
		long atencion = escenario.tomar(paramedico, incidente);

		cerrar(incidente, "FALSA_ALARMA_VERIFICADA")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("TRANSICION_INVALIDA"));
		pedir(get("/incidentes/" + incidente), escenario.admin()).andExpect(jsonPath("$.estado").value("EN_ATENCION"));

		pedir(post("/atenciones/" + atencion + "/cancelar"), paramedico.token(), Json.objeto("motivo", "DESVIADA"))
				.andExpect(status().isOk());
		cerrar(incidente, "SIN_COBERTURA").andExpect(status().isNoContent());
	}

	@Test
	@DisplayName("PB-17 · CP-17-03 · un incidente ya cerrado no se vuelve a cerrar")
	void yaCerrado() throws Exception {
		long incidente = escenario.alertar(escenario.ciudadano()).incidenteId();
		cerrar(incidente, "OTRO").andExpect(status().isNoContent());

		cerrar(incidente, "FALSA_ALARMA_VERIFICADA")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("INCIDENTE_CERRADO"));
		pedir(get("/incidentes/" + incidente), escenario.admin()).andExpect(jsonPath("$.motivoCierre").value("OTRO"));
	}

	@Test
	@DisplayName("PB-17 · CP-17-04 · cerrado, deja de ofrecerse: sale de la lista de las unidades y del centro de control, y nadie lo toma")
	void dejaDeOfrecerse() throws Exception {
		long incidente = escenario.alertar(escenario.ciudadano()).incidenteId();

		cerrar(incidente, "ATENDIDO_EXTERNAMENTE").andExpect(status().isNoContent());

		assertThat(publicaciones.fueRetirado(incidente)).isTrue();
		pedir(get("/operacion"), escenario.admin()).andExpect(jsonPath("$.incidentesSinCubrir.length()").value(0));
		pedir(post("/incidentes/" + incidente + "/tomar"), escenario.paramedicoEnTurno().token())
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("INCIDENTE_CERRADO"));
	}

	@Test
	@DisplayName("PB-17 · CP-17-04 · el incidente cerrado conserva su detalle y figura entre los cerrados")
	void conservaElHistorial() throws Exception {
		long incidente = escenario.alertar(escenario.ciudadano()).incidenteId();
		escenario.alertar(escenario.ciudadano());

		cerrar(incidente, "FALSA_ALARMA_VERIFICADA").andExpect(status().isNoContent());

		pedir(get("/incidentes/" + incidente), escenario.admin())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.cantidadAlertas").value(2))
				.andExpect(jsonPath("$.alertas.length()").value(2));
		pedir(get("/incidentes").param("estado", "CERRADOS"), escenario.admin())
				.andExpect(jsonPath("$.contenido[0].id").value(incidente))
				.andExpect(jsonPath("$.contenido[0].motivoCierre").value("FALSA_ALARMA_VERIFICADA"));
	}

	@Test
	@DisplayName("PB-17 · CP-17-05 · a quien pidió ayuda le llega el aviso del cierre; sin cobertura, se lo dice así")
	void avisoAlCiudadano() throws Exception {
		Ciudadano ciudadano = escenario.ciudadano();
		escenario.recibirAvisos(ciudadano, "push-ciudadano");
		long incidente = escenario.alertar(ciudadano).incidenteId();

		cerrar(incidente, "SIN_COBERTURA").andExpect(status().isNoContent());

		AvisoParaCiudadano aviso = publicaciones.avisosAlCiudadano("push-ciudadano").getLast();
		assertThat(aviso.datos()).containsEntry("tipo", "CERRADO_POR_LA_CENTRAL");
		assertThat(aviso.cuerpo()).startsWith("No conseguimos una ambulancia");
	}

	@Test
	@DisplayName("PB-17 · CP-17-05 · solo la central cierra incidentes")
	void soloLaCentral() throws Exception {
		long incidente = escenario.alertar(escenario.ciudadano()).incidenteId();

		pedir(post("/incidentes/" + incidente + "/cierre"), escenario.paramedicoActivado().token(),
				Json.objeto("motivo", "OTRO"))
				.andExpect(status().isForbidden());
		pedir(post("/incidentes/" + incidente + "/cierre"), escenario.ciudadano().token(),
				Json.objeto("motivo", "OTRO"))
				.andExpect(status().isForbidden());
	}

	private ResultActions cerrar(long incidente, String motivo) throws Exception {
		return pedir(post("/incidentes/" + incidente + "/cierre"), escenario.admin(), Json.objeto("motivo", motivo));
	}

}
