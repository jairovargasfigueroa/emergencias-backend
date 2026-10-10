package com.uem.ambulancias.emergencias;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import com.uem.ambulancias.emergencias.domain.EstadoAtencion;
import com.uem.ambulancias.soporte.Escenario;
import com.uem.ambulancias.soporte.Escenario.Paramedico;
import com.uem.ambulancias.soporte.Json;
import com.uem.ambulancias.soporte.PruebaIT;

@Tag("PB-12")
class EstadosDeAtencionIT extends PruebaIT {

	/** El hospital queda a unos 1,5 km del lugar del incidente. */
	private static final double LATITUD_HOSPITAL = Escenario.LATITUD + 0.0135;

	@Test
	@DisplayName("PB-12 · CP-12-01 · la atención avanza en orden y cada hito guarda su hora y su lugar")
	void secuenciaCompleta() throws Exception {
		Paramedico paramedico = escenario.paramedicoEnTurno();
		long incidente = escenario.alertar(escenario.ciudadano()).incidenteId();
		long atencion = escenario.tomar(paramedico, incidente);

		hito(paramedico, atencion, "llegada", Escenario.LATITUD).andExpect(jsonPath("$.estado").value("EN_EL_LUGAR"));
		hito(paramedico, atencion, "recogida", Escenario.LATITUD + 0.0001)
				.andExpect(jsonPath("$.estado").value("PACIENTE_RECOGIDO"));
		hito(paramedico, atencion, "hospital", LATITUD_HOSPITAL).andExpect(jsonPath("$.estado").value("EN_HOSPITAL"));
		hito(paramedico, atencion, "entrega", LATITUD_HOSPITAL)
				.andExpect(jsonPath("$.estado").value("PACIENTE_ENTREGADO"));

		pedir(get("/incidentes/" + incidente), escenario.admin())
				.andExpect(jsonPath("$.atenciones[0].horaToma").isNotEmpty())
				.andExpect(jsonPath("$.atenciones[0].horaLlegada").isNotEmpty())
				.andExpect(jsonPath("$.atenciones[0].horaRecogida").isNotEmpty())
				.andExpect(jsonPath("$.atenciones[0].horaLlegadaHospital").isNotEmpty())
				.andExpect(jsonPath("$.atenciones[0].horaEntrega").isNotEmpty())
				.andExpect(jsonPath("$.atenciones[0].ubicacionLlegada.latitud").value(Escenario.LATITUD))
				.andExpect(jsonPath("$.atenciones[0].ubicacionRecogida.latitud").value(Escenario.LATITUD + 0.0001))
				.andExpect(jsonPath("$.atenciones[0].ubicacionLlegadaHospital.latitud").value(LATITUD_HOSPITAL))
				.andExpect(jsonPath("$.atenciones[0].ubicacionEntrega.latitud").value(LATITUD_HOSPITAL));
	}

	@Test
	@DisplayName("PB-12 · CP-12-01 · no se saltan hitos ni se vuelve atrás")
	void sinSaltosNiRetrocesos() throws Exception {
		Paramedico paramedico = escenario.paramedicoEnTurno();
		long atencion = escenario.tomar(paramedico, escenario.alertar(escenario.ciudadano()).incidenteId());

		hitoRechazado(paramedico, atencion, "recogida");
		hitoRechazado(paramedico, atencion, "hospital");
		hitoRechazado(paramedico, atencion, "entrega");

		escenario.marcar(paramedico, atencion, "llegada");
		escenario.marcar(paramedico, atencion, "recogida");
		hitoRechazado(paramedico, atencion, "llegada");
		hitoRechazado(paramedico, atencion, "recogida");

		pedir(get("/paramedicos/actual/atencion"), paramedico.token())
				.andExpect(jsonPath("$.estado").value("PACIENTE_RECOGIDO"));
	}

	@Test
	@DisplayName("PB-12 · CP-12-02 · quien sigue el incidente ve cada hito: la central y el ciudadano")
	void cambiosVisibles() throws Exception {
		Paramedico paramedico = escenario.paramedicoEnTurno();
		long incidente = escenario.alertar(escenario.ciudadano()).incidenteId();
		long atencion = escenario.tomar(paramedico, incidente);

		escenario.marcar(paramedico, atencion, "llegada");
		escenario.marcar(paramedico, atencion, "recogida");

		pedir(get("/incidentes/" + incidente), escenario.admin())
				.andExpect(jsonPath("$.atenciones[0].estado").value("PACIENTE_RECOGIDO"));
		assertThat(publicaciones.ultimoSeguimiento(incidente).unidades()).singleElement()
				.satisfies(unidad -> assertThat(unidad.estado()).isEqualTo(EstadoAtencion.PACIENTE_RECOGIDO));
	}

	@Test
	@DisplayName("PB-12 · CP-12-03 · los datos del paciente son opcionales, se corrigen mientras está activa y después no")
	void datosDelPaciente() throws Exception {
		Paramedico paramedico = escenario.paramedicoEnTurno();
		long incidente = escenario.alertar(escenario.ciudadano()).incidenteId();
		long atencion = escenario.tomar(paramedico, incidente);
		escenario.marcar(paramedico, atencion, "llegada");

		pedir(post("/atenciones/" + atencion + "/recogida"), paramedico.token(), Json.objeto("latitud",
				Escenario.LATITUD, "longitud", Escenario.LONGITUD, "nombrePaciente", "Juan Peres"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nombrePaciente").value("Juan Peres"));
		paciente(paramedico, atencion, Json.objeto("nombrePaciente", "Juan Pérez", "documentoPaciente", "4567890 SC"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nombrePaciente").value("Juan Pérez"))
				.andExpect(jsonPath("$.documentoPaciente").value("4567890 SC"));

		escenario.marcar(paramedico, atencion, "hospital");
		hito(paramedico, atencion, "entrega", LATITUD_HOSPITAL).andExpect(status().isOk());

		paciente(paramedico, atencion, Json.objeto("nombrePaciente", "Otro nombre"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("ATENCION_FINALIZADA"));
		pedir(get("/incidentes/" + incidente), escenario.admin())
				.andExpect(jsonPath("$.atenciones[0].nombrePaciente").value("Juan Pérez"))
				.andExpect(jsonPath("$.atenciones[0].documentoPaciente").value("4567890 SC"));
	}

	@Test
	@DisplayName("PB-12 · CP-12-04 · cancelar por avería deja la unidad fuera de servicio")
	void cancelarPorAveria() throws Exception {
		Paramedico paramedico = escenario.paramedicoEnTurno();
		long atencion = escenario.tomar(paramedico, escenario.alertar(escenario.ciudadano()).incidenteId());

		cancelar(paramedico, atencion, "AVERIA")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("CANCELADA"))
				.andExpect(jsonPath("$.motivoCancelacion").value("AVERIA"))
				.andExpect(jsonPath("$.horaCancelacion").isNotEmpty());

		estadoDeLaAmbulancia(paramedico.ambulanciaId(), "FUERA_DE_SERVICIO");
	}

	@Test
	@DisplayName("PB-12 · CP-12-04 · cancelar por otro motivo admitido deja la unidad disponible")
	void cancelarPorOtroMotivo() throws Exception {
		Paramedico paramedico = escenario.paramedicoEnTurno();
		long atencion = escenario.tomar(paramedico, escenario.alertar(escenario.ciudadano()).incidenteId());
		escenario.marcar(paramedico, atencion, "llegada");

		cancelar(paramedico, atencion, "NO_SE_ENCONTRO_PACIENTE").andExpect(status().isOk());

		estadoDeLaAmbulancia(paramedico.ambulanciaId(), "DISPONIBLE");
	}

	@Test
	@DisplayName("PB-12 · CP-12-04 · sin motivo, con un motivo de la central o ya entregada, no se cancela")
	void cancelarNoPermitido() throws Exception {
		Paramedico paramedico = escenario.paramedicoEnTurno();
		long atencion = escenario.tomar(paramedico, escenario.alertar(escenario.ciudadano()).incidenteId());

		pedir(post("/atenciones/" + atencion + "/cancelar"), paramedico.token(), Json.objeto("motivo", null))
				.andExpect(status().isBadRequest());
		cancelar(paramedico, atencion, "CERRADA_POR_CENTRAL")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("VALIDACION"));

		escenario.hastaElHospital(paramedico, atencion);
		hito(paramedico, atencion, "entrega", LATITUD_HOSPITAL).andExpect(status().isOk());
		cancelar(paramedico, atencion, "OTRO")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("TRANSICION_INVALIDA"));
		estadoDeLaAmbulancia(paramedico.ambulanciaId(), "EN_ATENCION");
	}

	@Test
	@DisplayName("PB-12 · CP-12-05 · el compañero de la unidad también marca los hitos; un paramédico de otra unidad, no")
	void quienMarca() throws Exception {
		Paramedico primero = escenario.paramedicoActivado();
		Paramedico companero = escenario.paramedicoActivado();
		long ambulancia = escenario.ambulancia();
		escenario.asignar(primero.id(), ambulancia);
		escenario.asignar(companero.id(), ambulancia);
		escenario.iniciarTurno(primero);
		escenario.iniciarTurno(companero);
		long atencion = escenario.tomar(primero, escenario.alertar(escenario.ciudadano()).incidenteId());

		hito(companero, atencion, "llegada", Escenario.LATITUD).andExpect(jsonPath("$.estado").value("EN_EL_LUGAR"));

		pedir(post("/atenciones/" + atencion + "/recogida"), escenario.paramedicoEnTurno().token(),
				Json.objeto("latitud", Escenario.LATITUD, "longitud", Escenario.LONGITUD))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("ATENCION_AJENA"));
		pedir(post("/atenciones/" + atencion + "/recogida"), escenario.ciudadano().token(),
				Json.objeto("latitud", Escenario.LATITUD, "longitud", Escenario.LONGITUD))
				.andExpect(status().isForbidden());
	}

	private ResultActions hito(Paramedico paramedico, long atencion, String hito, double latitud) throws Exception {
		return pedir(post("/atenciones/" + atencion + "/" + hito), paramedico.token(),
				Json.objeto("latitud", latitud, "longitud", Escenario.LONGITUD))
				.andExpect(status().isOk());
	}

	private void hitoRechazado(Paramedico paramedico, long atencion, String hito) throws Exception {
		pedir(post("/atenciones/" + atencion + "/" + hito), paramedico.token(),
				Json.objeto("latitud", Escenario.LATITUD, "longitud", Escenario.LONGITUD))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("TRANSICION_INVALIDA"));
	}

	private ResultActions paciente(Paramedico paramedico, long atencion, String cuerpo) throws Exception {
		return pedir(post("/atenciones/" + atencion + "/paciente"), paramedico.token(), cuerpo);
	}

	private ResultActions cancelar(Paramedico paramedico, long atencion, String motivo) throws Exception {
		return pedir(post("/atenciones/" + atencion + "/cancelar"), paramedico.token(), Json.objeto("motivo", motivo));
	}

	private void estadoDeLaAmbulancia(long ambulancia, String estado) throws Exception {
		pedir(get("/ambulancias"), escenario.admin())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.id == " + ambulancia + ")].estado", hasItem(estado)));
	}

}
