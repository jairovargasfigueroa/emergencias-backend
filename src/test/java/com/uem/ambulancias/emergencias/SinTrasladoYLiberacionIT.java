package com.uem.ambulancias.emergencias;

import static org.hamcrest.Matchers.hasItem;
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

import com.uem.ambulancias.soporte.Escenario;
import com.uem.ambulancias.soporte.Escenario.Paramedico;
import com.uem.ambulancias.soporte.Json;
import com.uem.ambulancias.soporte.PruebaIT;

@Tag("PB-13")
class SinTrasladoYLiberacionIT extends PruebaIT {

	@Test
	@DisplayName("PB-13 · CP-13-01 · desde el lugar se termina sin traslado con su motivo, y no cuenta como cancelación")
	void terminarSinTraslado() throws Exception {
		Paramedico paramedico = escenario.paramedicoEnTurno();
		long incidente = escenario.alertar(escenario.ciudadano()).incidenteId();
		long atencion = escenario.tomar(paramedico, incidente);
		escenario.marcar(paramedico, atencion, "llegada");

		sinTraslado(paramedico, atencion, "ATENDIDO_EN_EL_LUGAR")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("SIN_TRASLADO"))
				.andExpect(jsonPath("$.motivoSinTraslado").value("ATENDIDO_EN_EL_LUGAR"))
				.andExpect(jsonPath("$.horaSinTraslado").isNotEmpty())
				.andExpect(jsonPath("$.motivoCancelacion").doesNotExist());

		pedir(get("/incidentes/" + incidente), escenario.admin())
				.andExpect(jsonPath("$.atenciones[0].motivoSinTraslado").value("ATENDIDO_EN_EL_LUGAR"))
				.andExpect(jsonPath("$.atenciones[0].ubicacionSinTraslado.latitud").value(Escenario.LATITUD));
	}

	@Test
	@DisplayName("PB-13 · CP-13-01 · solo desde el lugar: ni en camino ni con el paciente a bordo")
	void soloDesdeElLugar() throws Exception {
		Paramedico paramedico = escenario.paramedicoEnTurno();
		long atencion = escenario.tomar(paramedico, escenario.alertar(escenario.ciudadano()).incidenteId());

		sinTraslado(paramedico, atencion, "PACIENTE_RECHAZO")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("TRANSICION_INVALIDA"));

		escenario.marcar(paramedico, atencion, "llegada");
		escenario.marcar(paramedico, atencion, "recogida");
		sinTraslado(paramedico, atencion, "PACIENTE_RECHAZO")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("TRANSICION_INVALIDA"));
	}

	@Test
	@DisplayName("PB-13 · CP-13-01 · sin motivo o con un motivo que es solo de traslados no se termina")
	void motivoInvalido() throws Exception {
		Paramedico paramedico = escenario.paramedicoEnTurno();
		long atencion = escenario.tomar(paramedico, escenario.alertar(escenario.ciudadano()).incidenteId());
		escenario.marcar(paramedico, atencion, "llegada");

		pedir(post("/atenciones/" + atencion + "/sin-traslado"), paramedico.token(),
				Json.objeto("latitud", Escenario.LATITUD, "longitud", Escenario.LONGITUD))
				.andExpect(status().isBadRequest());
		sinTraslado(paramedico, atencion, "PACIENTE_NO_LISTO")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("VALIDACION"));
		sinTraslado(paramedico, atencion, "UNIDAD_NO_CORRESPONDE")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("VALIDACION"));
		pedir(get("/paramedicos/actual/atencion"), paramedico.token())
				.andExpect(jsonPath("$.estado").value("EN_EL_LUGAR"));
	}

	@ParameterizedTest(name = "{0} → {1}")
	@CsvSource({
			"ATENDIDO_EN_EL_LUGAR, ATENDIDO, ",
			"PACIENTE_RECHAZO, ATENDIDO, ",
			"FALLECIDO, ATENDIDO, ",
			"NO_HABIA_PACIENTE, FALSA_ALARMA, FALSA_ALARMA_VERIFICADA",
			"TRASLADO_POR_OTRO_MEDIO, ATENDIDO_EXTERNAMENTE, ATENDIDO_EXTERNAMENTE" })
	@DisplayName("PB-13 · CP-13-02 · el motivo decide cómo se cierra el incidente")
	void desenlace(String motivo, String estadoIncidente, String motivoCierre) throws Exception {
		Paramedico paramedico = escenario.paramedicoEnTurno();
		long incidente = escenario.alertar(escenario.ciudadano()).incidenteId();
		long atencion = escenario.tomar(paramedico, incidente);
		escenario.marcar(paramedico, atencion, "llegada");

		sinTraslado(paramedico, atencion, motivo).andExpect(status().isOk());

		pedir(get("/incidentes/" + incidente), escenario.admin())
				.andExpect(jsonPath("$.estado").value(estadoIncidente))
				.andExpect(jsonPath("$.fechaHoraCierre").isNotEmpty())
				.andExpect(motivoCierre == null ? jsonPath("$.motivoCierre").doesNotExist()
						: jsonPath("$.motivoCierre").value(motivoCierre));
	}

	@Test
	@DisplayName("PB-13 · CP-13-02 · con otra unidad trabajando el incidente sigue, y una entrega manda sobre el resto")
	void conOtraUnidad() throws Exception {
		long incidente = escenario.alertar(escenario.ciudadano()).incidenteId();
		Paramedico primero = escenario.paramedicoEnTurno();
		Paramedico segundo = escenario.paramedicoEnTurno();
		long atencionPrimero = escenario.tomar(primero, incidente);
		long atencionSegundo = Json.numero(cuerpo(pedir(post("/incidentes/" + incidente + "/sumarse"),
				segundo.token()).andExpect(status().isCreated())), "$.id");
		escenario.marcar(primero, atencionPrimero, "llegada");
		escenario.marcar(segundo, atencionSegundo, "llegada");

		sinTraslado(segundo, atencionSegundo, "NO_HABIA_PACIENTE").andExpect(status().isOk());
		pedir(get("/incidentes/" + incidente), escenario.admin()).andExpect(jsonPath("$.estado").value("EN_ATENCION"));

		escenario.marcar(primero, atencionPrimero, "recogida");
		escenario.marcar(primero, atencionPrimero, "hospital");
		pedir(post("/atenciones/" + atencionPrimero + "/entrega"), primero.token(),
				Json.objeto("latitud", Escenario.LATITUD, "longitud", Escenario.LONGITUD))
				.andExpect(status().isOk());

		pedir(get("/incidentes/" + incidente), escenario.admin()).andExpect(jsonPath("$.estado").value("ATENDIDO"));
	}

	@Test
	@DisplayName("PB-13 · CP-13-03 · entregar no libera la unidad: sigue ocupada y no toma otro incidente")
	void entregarNoLibera() throws Exception {
		Paramedico paramedico = escenario.paramedicoEnTurno();
		long atencion = escenario.tomar(paramedico, escenario.alertar(escenario.ciudadano()).incidenteId());
		escenario.hastaElHospital(paramedico, atencion);
		pedir(post("/atenciones/" + atencion + "/entrega"), paramedico.token(),
				Json.objeto("latitud", Escenario.LATITUD, "longitud", Escenario.LONGITUD))
				.andExpect(status().isOk());

		estadoDeLaAmbulancia(paramedico.ambulanciaId(), "EN_ATENCION");
		pedir(get("/paramedicos/actual/atencion"), paramedico.token())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(atencion))
				.andExpect(jsonPath("$.horaLiberacion").doesNotExist());
		long otro = escenario.alertar(escenario.ciudadano(), Escenario.LATITUD + 0.1, Escenario.LONGITUD).incidenteId();
		pedir(post("/incidentes/" + otro + "/tomar"), paramedico.token())
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("AMBULANCIA_NO_DISPONIBLE"));
	}

	@Test
	@DisplayName("PB-13 · CP-13-03 · terminar sin traslado tampoco libera la unidad")
	void sinTrasladoNoLibera() throws Exception {
		Paramedico paramedico = escenario.paramedicoEnTurno();
		long atencion = escenario.tomar(paramedico, escenario.alertar(escenario.ciudadano()).incidenteId());
		escenario.marcar(paramedico, atencion, "llegada");

		sinTraslado(paramedico, atencion, "ATENDIDO_EN_EL_LUGAR").andExpect(status().isOk());

		estadoDeLaAmbulancia(paramedico.ambulanciaId(), "EN_ATENCION");
	}

	@Test
	@DisplayName("PB-13 · CP-13-04 · al liberarse la unidad vuelve a estar disponible, y queda la hora")
	void liberar() throws Exception {
		Paramedico paramedico = escenario.paramedicoEnTurno();
		long incidente = escenario.alertar(escenario.ciudadano()).incidenteId();
		long atencion = escenario.tomar(paramedico, incidente);
		escenario.marcar(paramedico, atencion, "llegada");
		sinTraslado(paramedico, atencion, "PACIENTE_RECHAZO").andExpect(status().isOk());

		pedir(post("/atenciones/" + atencion + "/liberacion"), paramedico.token())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.horaLiberacion").isNotEmpty());

		estadoDeLaAmbulancia(paramedico.ambulanciaId(), "DISPONIBLE");
		pedir(get("/paramedicos/actual/atencion"), paramedico.token()).andExpect(status().isNoContent());
		pedir(get("/incidentes/" + incidente), escenario.admin())
				.andExpect(jsonPath("$.atenciones[0].horaLiberacion").isNotEmpty());
		pedir(post("/atenciones/" + atencion + "/liberacion"), paramedico.token())
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("TRANSICION_INVALIDA"));
	}

	@Test
	@DisplayName("PB-13 · CP-13-04 · no se libera una atención que sigue en curso")
	void liberarEnCurso() throws Exception {
		Paramedico paramedico = escenario.paramedicoEnTurno();
		long atencion = escenario.tomar(paramedico, escenario.alertar(escenario.ciudadano()).incidenteId());
		escenario.marcar(paramedico, atencion, "llegada");

		pedir(post("/atenciones/" + atencion + "/liberacion"), paramedico.token())
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("TRANSICION_INVALIDA"));
		estadoDeLaAmbulancia(paramedico.ambulanciaId(), "EN_ATENCION");
	}

	@Test
	@DisplayName("PB-13 · CP-13-04 · una unidad que quedó fuera de servicio por avería no vuelve a disponible liberándose")
	void averiaNoSeLibera() throws Exception {
		Paramedico paramedico = escenario.paramedicoEnTurno();
		long atencion = escenario.tomar(paramedico, escenario.alertar(escenario.ciudadano()).incidenteId());
		pedir(post("/atenciones/" + atencion + "/cancelar"), paramedico.token(), Json.objeto("motivo", "AVERIA"))
				.andExpect(status().isOk());

		pedir(post("/atenciones/" + atencion + "/liberacion"), paramedico.token())
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("TRANSICION_INVALIDA"));
		estadoDeLaAmbulancia(paramedico.ambulanciaId(), "FUERA_DE_SERVICIO");
	}

	private ResultActions sinTraslado(Paramedico paramedico, long atencion, String motivo) throws Exception {
		return pedir(post("/atenciones/" + atencion + "/sin-traslado"), paramedico.token(),
				Json.objeto("latitud", Escenario.LATITUD, "longitud", Escenario.LONGITUD, "motivo", motivo));
	}

	private void estadoDeLaAmbulancia(long ambulancia, String estado) throws Exception {
		pedir(get("/ambulancias"), escenario.admin())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.id == " + ambulancia + ")].estado", hasItem(estado)));
	}

}
