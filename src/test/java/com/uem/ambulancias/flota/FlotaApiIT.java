package com.uem.ambulancias.flota;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.uem.ambulancias.soporte.Escenario;
import com.uem.ambulancias.soporte.Escenario.Paramedico;
import com.uem.ambulancias.soporte.Json;
import com.uem.ambulancias.soporte.PruebaIT;

@Tag("PB-01")
class FlotaApiIT extends PruebaIT {

	@Test
	@DisplayName("PB-01 · CP-01-01 · una ambulancia válida se registra activa, sin turno y con la placa normalizada")
	void registrarAmbulancia() throws Exception {
		pedir(post("/ambulancias"), escenario.admin(), Json.objeto("placa", " abc 123 ", "tipoUnidad", "II"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.placa").value("ABC123"))
				.andExpect(jsonPath("$.estado").value("SIN_TURNO"))
				.andExpect(jsonPath("$.activa").value(true));
	}

	@Test
	@DisplayName("PB-01 · CP-01-02 · una placa repetida se rechaza aunque cambien los espacios o las mayúsculas")
	void placaDuplicada() throws Exception {
		pedir(post("/ambulancias"), escenario.admin(), Json.objeto("placa", "ABC123", "tipoUnidad", "II"))
				.andExpect(status().isCreated());

		pedir(post("/ambulancias"), escenario.admin(), Json.objeto("placa", "abc 123", "tipoUnidad", "III"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("PLACA_DUPLICADA"));
	}

	@Test
	@DisplayName("PB-01 · CP-01-02 · una ambulancia sin placa o sin tipo se rechaza")
	void datosInvalidos() throws Exception {
		pedir(post("/ambulancias"), escenario.admin(), Json.objeto("placa", "", "tipoUnidad", "II"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value("VALIDACION"));
		pedir(post("/ambulancias"), escenario.admin(), Json.objeto("placa", "ABC123"))
				.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("PB-01 · CP-01-03 · registrar un paramédico crea su cuenta, y un teléfono repetido se rechaza")
	void telefonoDuplicado() throws Exception {
		String telefono = Escenario.nuevoTelefono();
		pedir(post("/paramedicos"), escenario.admin(), Json.objeto("nombreCompleto", "Ana Rojas", "telefono", telefono))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.activo").value(true))
				.andExpect(jsonPath("$.activado").value(false));

		pedir(post("/paramedicos"), escenario.admin(), Json.objeto("nombreCompleto", "Otra", "telefono", telefono))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("TELEFONO_DUPLICADO"));
	}

	@Test
	@DisplayName("PB-01 · CP-01-04 · un paramédico queda asignado a una ambulancia, y repetirlo se rechaza")
	void asignar() throws Exception {
		long paramedico = escenario.paramedicoRegistrado(Escenario.nuevoTelefono());
		long ambulancia = escenario.ambulancia();

		pedir(post("/asignaciones"), escenario.admin(),
				Json.objeto("paramedicoId", paramedico, "ambulanciaId", ambulancia))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.vigente").value(true));

		pedir(post("/asignaciones"), escenario.admin(),
				Json.objeto("paramedicoId", paramedico, "ambulanciaId", ambulancia))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("YA_ASIGNADO"));
	}

	@Test
	@DisplayName("PB-01 · CP-01-04 · cambiar de ambulancia pide confirmación, cierra la anterior y conserva el historial")
	void reasignar() throws Exception {
		long paramedico = escenario.paramedicoRegistrado(Escenario.nuevoTelefono());
		long primera = escenario.ambulancia();
		long segunda = escenario.ambulancia();
		escenario.asignar(paramedico, primera);

		pedir(post("/asignaciones"), escenario.admin(), Json.objeto("paramedicoId", paramedico, "ambulanciaId", segunda))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("REASIGNACION_REQUIERE_CONFIRMACION"));

		pedir(post("/asignaciones"), escenario.admin(),
				Json.objeto("paramedicoId", paramedico, "ambulanciaId", segunda, "confirmarReasignacion", true))
				.andExpect(status().isCreated());

		pedir(get("/paramedicos/" + paramedico + "/asignaciones"), escenario.admin())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[?(@.vigente == true)]", hasSize(1)))
				.andExpect(jsonPath("$[?(@.vigente == true)].ambulanciaId", hasItem((int) segunda)));
	}

	@Test
	@DisplayName("PB-01 · CP-01-04 · no se asigna a una ambulancia desactivada ni a un paramédico desactivado")
	void asignarInactivos() throws Exception {
		long ambulanciaDesactivada = escenario.ambulancia();
		pedir(post("/ambulancias/" + ambulanciaDesactivada + "/desactivar"), escenario.admin())
				.andExpect(status().isOk());
		long paramedico = escenario.paramedicoRegistrado(Escenario.nuevoTelefono());

		pedir(post("/asignaciones"), escenario.admin(),
				Json.objeto("paramedicoId", paramedico, "ambulanciaId", ambulanciaDesactivada))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("AMBULANCIA_INACTIVA"));

		pedir(post("/paramedicos/" + paramedico + "/desactivar"), escenario.admin()).andExpect(status().isOk());
		pedir(post("/asignaciones"), escenario.admin(),
				Json.objeto("paramedicoId", paramedico, "ambulanciaId", escenario.ambulancia()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("PARAMEDICO_INACTIVO"));
	}

	@Test
	@DisplayName("PB-01 · CP-01-05 · una unidad atendiendo no se pone fuera de servicio, y con gente de turno no se desactiva")
	void unidadOcupada() throws Exception {
		Paramedico paramedico = escenario.paramedicoEnTurno();
		escenario.tomar(paramedico, escenario.alertar(escenario.ciudadano()).incidenteId());

		pedir(post("/ambulancias/" + paramedico.ambulanciaId() + "/fuera-de-servicio"), escenario.admin())
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("AMBULANCIA_EN_ATENCION"));
		pedir(post("/ambulancias/" + paramedico.ambulanciaId() + "/desactivar"), escenario.admin())
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("PARAMEDICO_EN_TURNO"));
	}

	@Test
	@DisplayName("PB-01 · CP-01-06 · a un paramédico en turno no se lo desactiva ni se le cambia la ambulancia")
	void paramedicoEnTurno() throws Exception {
		Paramedico paramedico = escenario.paramedicoEnTurno();

		pedir(post("/paramedicos/" + paramedico.id() + "/desactivar"), escenario.admin())
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("PARAMEDICO_EN_TURNO"));
		pedir(post("/asignaciones"), escenario.admin(), Json.objeto("paramedicoId", paramedico.id(), "ambulanciaId",
				escenario.ambulancia(), "confirmarReasignacion", true))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("PARAMEDICO_EN_TURNO"));
	}

	@Test
	@DisplayName("PB-01 · CP-01-07 · dar de baja una ambulancia no la borra: sigue en la flota como inactiva")
	void bajaLogica() throws Exception {
		long ambulancia = escenario.ambulancia();

		pedir(post("/ambulancias/" + ambulancia + "/desactivar"), escenario.admin())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.activa").value(false));
		pedir(get("/ambulancias"), escenario.admin())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.id == " + ambulancia + ")].activa", hasItem(false)));
	}

	@Test
	@DisplayName("PB-01 · CP-01-08 · solo la central gestiona la flota: ciudadano y paramédico reciben 403, sin sesión 401")
	void permisos() throws Exception {
		String cuerpo = Json.objeto("placa", "ABC123", "tipoUnidad", "II");

		pedir(post("/ambulancias"), escenario.ciudadano().token(), cuerpo).andExpect(status().isForbidden());
		pedir(post("/ambulancias"), escenario.paramedicoActivado().token(), cuerpo).andExpect(status().isForbidden());
		pedir(post("/ambulancias"), null, cuerpo).andExpect(status().isUnauthorized());
	}

}
