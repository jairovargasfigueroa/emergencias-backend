package com.uem.ambulancias.flota;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import com.uem.ambulancias.soporte.Escenario;
import com.uem.ambulancias.soporte.Escenario.Paramedico;
import com.uem.ambulancias.soporte.Json;
import com.uem.ambulancias.soporte.PruebaIT;

@Tag("PB-04")
class TurnoIT extends PruebaIT {

	@Test
	@DisplayName("PB-04 · CP-04-01 · con su PIN y su ambulancia asignada inicia el turno, y la unidad queda disponible")
	void iniciar() throws Exception {
		Paramedico paramedico = escenario.paramedicoActivado();
		long ambulancia = escenario.ambulancia();
		escenario.asignar(paramedico.id(), ambulancia);

		iniciarTurno(paramedico, Escenario.PIN, paramedico.claveDispositivo())
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.ambulanciaId").value(ambulancia));

		estadoDeLaAmbulancia(ambulancia, "DISPONIBLE");
		pedir(get("/paramedicos/actual"), paramedico.token())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.turno").isNotEmpty());
	}

	@Test
	@DisplayName("PB-04 · CP-04-02 · sin una ambulancia asignada no se inicia el turno")
	void sinAsignacion() throws Exception {
		Paramedico paramedico = escenario.paramedicoActivado();

		iniciarTurno(paramedico, Escenario.PIN, paramedico.claveDispositivo())
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("SIN_SERVICIO"));
	}

	@Test
	@DisplayName("PB-04 · CP-04-02 · con la ambulancia dada de baja no se inicia el turno")
	void ambulanciaDeBaja() throws Exception {
		Paramedico paramedico = escenario.paramedicoActivado();
		long ambulancia = escenario.ambulancia();
		escenario.asignar(paramedico.id(), ambulancia);
		pedir(post("/ambulancias/" + ambulancia + "/desactivar"), escenario.admin()).andExpect(status().isOk());

		iniciarTurno(paramedico, Escenario.PIN, paramedico.claveDispositivo())
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("SIN_SERVICIO"));
	}

	@Test
	@DisplayName("PB-04 · CP-04-01 · iniciar el turno pide el PIN correcto y el teléfono vinculado")
	void iniciarPideElPin() throws Exception {
		Paramedico paramedico = escenario.paramedicoActivado();
		escenario.asignar(paramedico.id(), escenario.ambulancia());

		iniciarTurno(paramedico, "905182", paramedico.claveDispositivo())
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("PIN_INCORRECTO"));
		iniciarTurno(paramedico, Escenario.PIN, "clave-de-otro-telefono")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("DISPOSITIVO_NO_VINCULADO"));
	}

	@Test
	@DisplayName("PB-04 · CP-04-03 · no se abre un segundo turno sobre uno abierto")
	void turnoRepetido() throws Exception {
		Paramedico paramedico = escenario.paramedicoEnTurno();

		iniciarTurno(paramedico, Escenario.PIN, paramedico.claveDispositivo())
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("TRANSICION_INVALIDA"));
	}

	@Test
	@DisplayName("PB-04 · CP-04-04 · con una atención en curso no se puede salir de turno")
	void salirConAtencion() throws Exception {
		Paramedico paramedico = escenario.paramedicoEnTurno();
		escenario.tomar(paramedico, escenario.alertar(escenario.ciudadano()).incidenteId());

		pedir(post("/paramedicos/actual/turno/cierre"), paramedico.token())
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("TRANSICION_INVALIDA"));
	}

	@Test
	@DisplayName("PB-04 · CP-04-04 · al salir el último de turno, la unidad deja de contar como disponible y no puede tomar")
	void salirDeTurno() throws Exception {
		Paramedico paramedico = escenario.paramedicoEnTurno();

		pedir(post("/paramedicos/actual/turno/cierre"), paramedico.token()).andExpect(status().isOk());

		estadoDeLaAmbulancia(paramedico.ambulanciaId(), "SIN_TURNO");
		long incidente = escenario.alertar(escenario.ciudadano()).incidenteId();
		pedir(post("/incidentes/" + incidente + "/tomar"), paramedico.token())
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("SIN_TURNO"));
	}

	@Test
	@DisplayName("PB-04 · CP-04-04 · con dos tripulantes, la unidad sigue disponible hasta que sale el último")
	void dosTripulantes() throws Exception {
		Paramedico primero = escenario.paramedicoActivado();
		Paramedico segundo = escenario.paramedicoActivado();
		long ambulancia = escenario.ambulancia();
		escenario.asignar(primero.id(), ambulancia);
		escenario.asignar(segundo.id(), ambulancia);
		escenario.iniciarTurno(primero);
		escenario.iniciarTurno(segundo);

		pedir(post("/paramedicos/actual/turno/cierre"), primero.token()).andExpect(status().isOk());
		estadoDeLaAmbulancia(ambulancia, "DISPONIBLE");

		pedir(post("/paramedicos/actual/turno/cierre"), segundo.token()).andExpect(status().isOk());
		estadoDeLaAmbulancia(ambulancia, "SIN_TURNO");
	}

	@Test
	@DisplayName("PB-04 · CP-04-05 · la central puede cerrarle el turno, pero no con una atención en curso")
	void cierreDesdeLaCentral() throws Exception {
		Paramedico libre = escenario.paramedicoEnTurno();
		pedir(post("/paramedicos/" + libre.id() + "/turno/cierre"), escenario.admin())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.enTurno").value(false));

		Paramedico ocupado = escenario.paramedicoEnTurno();
		escenario.tomar(ocupado, escenario.alertar(escenario.ciudadano()).incidenteId());
		pedir(post("/paramedicos/" + ocupado.id() + "/turno/cierre"), escenario.admin())
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("TRANSICION_INVALIDA"));
	}

	@Test
	@DisplayName("PB-04 · CP-04-05 · cerrar un turno que no existe se rechaza")
	void cerrarSinTurno() throws Exception {
		Paramedico paramedico = escenario.paramedicoActivado();

		pedir(post("/paramedicos/actual/turno/cierre"), paramedico.token())
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("TRANSICION_INVALIDA"));
	}

	private ResultActions iniciarTurno(Paramedico paramedico, String pin, String claveDispositivo) throws Exception {
		return pedir(post("/paramedicos/actual/turno/inicio"), paramedico.token(),
				Json.objeto("pin", pin, "claveDispositivo", claveDispositivo));
	}

	private void estadoDeLaAmbulancia(long ambulancia, String estado) throws Exception {
		pedir(get("/ambulancias"), escenario.admin())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.id == " + ambulancia + ")].estado", hasItem(estado)));
	}

}
