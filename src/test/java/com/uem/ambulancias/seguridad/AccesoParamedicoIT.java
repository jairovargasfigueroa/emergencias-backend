package com.uem.ambulancias.seguridad;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.uem.ambulancias.soporte.Escenario;
import com.uem.ambulancias.soporte.Escenario.Paramedico;
import com.uem.ambulancias.soporte.Json;
import com.uem.ambulancias.soporte.PruebaIT;

@Tag("PB-02")
class AccesoParamedicoIT extends PruebaIT {

	@Test
	@DisplayName("PB-02 · CP-02-01 · con el código de la central crea su PIN, vincula el teléfono y después entra con ellos")
	void activarYEntrar() throws Exception {
		String telefono = Escenario.nuevoTelefono();
		long id = escenario.paramedicoRegistrado(telefono);
		String codigo = escenario.codigoDeActivacion(id);

		String activacion = cuerpo(pedir(post("/auth/paramedico/activacion"), null,
				Json.objeto("telefono", telefono, "codigo", codigo, "pin", Escenario.PIN))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").isNotEmpty())
				.andExpect(jsonPath("$.claveDispositivo").isNotEmpty())
				.andExpect(jsonPath("$.paramedico.activado").value(true)));

		pedir(post("/auth/paramedico"), null, Json.objeto("telefono", telefono, "pin", Escenario.PIN,
				"claveDispositivo", Json.texto(activacion, "$.claveDispositivo")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").isNotEmpty());
	}

	@Test
	@DisplayName("PB-02 · CP-02-02 · un código incorrecto se rechaza y descuenta un intento")
	void codigoIncorrecto() throws Exception {
		String telefono = Escenario.nuevoTelefono();
		escenario.codigoDeActivacion(escenario.paramedicoRegistrado(telefono));

		activar(telefono, "ZZZZ-ZZZZ", Escenario.PIN)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("CODIGO_ACTIVACION_INVALIDO"));
	}

	@Test
	@DisplayName("PB-02 · CP-02-02 · un código vencido se rechaza")
	void codigoVencido() throws Exception {
		String telefono = Escenario.nuevoTelefono();
		long id = escenario.paramedicoRegistrado(telefono);
		String codigo = escenario.codigoDeActivacion(id);
		jdbc.update("update usuario set codigo_activacion_vence_en = now() - interval '1 minute' where id = ?", id);

		activar(telefono, codigo, Escenario.PIN)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("CODIGO_ACTIVACION_VENCIDO"));
	}

	@Test
	@DisplayName("PB-02 · CP-02-02 · sin un código pendiente no se puede activar")
	void sinCodigoPendiente() throws Exception {
		String telefono = Escenario.nuevoTelefono();
		escenario.paramedicoRegistrado(telefono);

		activar(telefono, "ABCD-EFGH", Escenario.PIN)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("CODIGO_ACTIVACION_INVALIDO"));
	}

	@ParameterizedTest(name = "PIN {0}")
	@ValueSource(strings = { "111111", "123456", "654321", "121212", "123123", "112233" })
	@DisplayName("PB-02 · CP-02-03 · un PIN fácil de adivinar se rechaza: iguales, seguidos, repetidos o de los más usados")
	void pinDebil(String pin) throws Exception {
		String telefono = Escenario.nuevoTelefono();
		String codigo = escenario.codigoDeActivacion(escenario.paramedicoRegistrado(telefono));

		activar(telefono, codigo, pin)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("PIN_DEBIL"));
	}

	@Test
	@DisplayName("PB-02 · CP-02-03 · un PIN con números del propio teléfono se rechaza")
	void pinConElTelefono() throws Exception {
		String telefono = Escenario.nuevoTelefono();
		String codigo = escenario.codigoDeActivacion(escenario.paramedicoRegistrado(telefono));

		activar(telefono, codigo, telefono.substring(1, 7))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("PIN_DEBIL"));
	}

	@Test
	@DisplayName("PB-02 · CP-02-04 · cuatro PIN incorrectos avisan cuántos quedan y el quinto bloquea, aunque después acierte")
	void bloqueoPorIntentos() throws Exception {
		Paramedico paramedico = escenario.paramedicoActivado();

		for (int restantes = 4; restantes >= 1; restantes--) {
			ingresar(paramedico, "905182")
					.andExpect(status().isConflict())
					.andExpect(jsonPath("$.codigo").value("PIN_INCORRECTO"));
		}
		ingresar(paramedico, "905182")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("PIN_BLOQUEADO"));
		ingresar(paramedico, Escenario.PIN)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("PIN_BLOQUEADO"));
	}

	@Test
	@DisplayName("PB-02 · CP-02-05 · desde un teléfono que no está vinculado no se entra, aunque el PIN sea correcto")
	void otroTelefono() throws Exception {
		Paramedico paramedico = escenario.paramedicoActivado();

		pedir(post("/auth/paramedico"), null, Json.objeto("telefono", paramedico.telefono(), "pin", Escenario.PIN,
				"claveDispositivo", "clave-de-otro-telefono"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("DISPOSITIVO_NO_VINCULADO"));
	}

	@Test
	@DisplayName("PB-02 · CP-02-05 · quien todavía no activó su cuenta no puede entrar")
	void sinActivar() throws Exception {
		String telefono = Escenario.nuevoTelefono();
		escenario.paramedicoRegistrado(telefono);

		pedir(post("/auth/paramedico"), null,
				Json.objeto("telefono", telefono, "pin", Escenario.PIN, "claveDispositivo", "cualquiera"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("PARAMEDICO_SIN_ACTIVAR"));
	}

	@Test
	@DisplayName("PB-02 · CP-02-06 · un código nuevo cierra las sesiones abiertas y deja sin efecto el PIN anterior")
	void codigoNuevoCierraSesiones() throws Exception {
		Paramedico paramedico = escenario.paramedicoActivado();
		pedir(get("/paramedicos/actual"), paramedico.token()).andExpect(status().isOk());

		escenario.codigoDeActivacion(paramedico.id());

		pedir(get("/paramedicos/actual"), paramedico.token()).andExpect(status().isUnauthorized());
		ingresar(paramedico, Escenario.PIN)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("PARAMEDICO_SIN_ACTIVAR"));
	}

	@Test
	@DisplayName("PB-02 · CP-02-07 · no se genera un código para quien está en turno ni para un paramédico desactivado")
	void codigoNoPermitido() throws Exception {
		Paramedico enTurno = escenario.paramedicoEnTurno();
		pedir(post("/paramedicos/" + enTurno.id() + "/activacion"), escenario.admin())
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("PARAMEDICO_EN_TURNO"));

		long desactivado = escenario.paramedicoRegistrado(Escenario.nuevoTelefono());
		pedir(post("/paramedicos/" + desactivado + "/desactivar"), escenario.admin()).andExpect(status().isOk());
		pedir(post("/paramedicos/" + desactivado + "/activacion"), escenario.admin())
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("PARAMEDICO_INACTIVO"));
	}

	@Test
	@DisplayName("PB-02 · CP-02-07 · una cuenta desactivada ya no entra")
	void cuentaDesactivada() throws Exception {
		Paramedico paramedico = escenario.paramedicoActivado();
		pedir(post("/paramedicos/" + paramedico.id() + "/desactivar"), escenario.admin()).andExpect(status().isOk());

		ingresar(paramedico, Escenario.PIN).andExpect(status().isNotFound());
	}

	private org.springframework.test.web.servlet.ResultActions activar(String telefono, String codigo, String pin)
			throws Exception {
		return pedir(post("/auth/paramedico/activacion"), null,
				Json.objeto("telefono", telefono, "codigo", codigo, "pin", pin));
	}

	private org.springframework.test.web.servlet.ResultActions ingresar(Paramedico paramedico, String pin)
			throws Exception {
		return pedir(post("/auth/paramedico"), null, Json.objeto("telefono", paramedico.telefono(), "pin", pin,
				"claveDispositivo", paramedico.claveDispositivo()));
	}

}
