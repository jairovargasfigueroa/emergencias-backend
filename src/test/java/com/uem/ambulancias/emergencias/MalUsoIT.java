package com.uem.ambulancias.emergencias;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import com.uem.ambulancias.soporte.Escenario;
import com.uem.ambulancias.soporte.Escenario.Alerta;
import com.uem.ambulancias.soporte.Escenario.Ciudadano;
import com.uem.ambulancias.soporte.Json;
import com.uem.ambulancias.soporte.PruebaIT;
import com.uem.ambulancias.soporte.VerificadorDeTelefonoFalso;

@Tag("PB-23")
class MalUsoIT extends PruebaIT {

	private static final String FUNDAMENTO = "Se devolvió la llamada: no había ninguna emergencia y admitió que era una broma.";

	/** Cada alerta en un lugar distinto, para que no se agrupen en el mismo incidente. */
	private int separacion = 0;

	private Alerta alertarAparte(Ciudadano ciudadano) {
		separacion++;
		return escenario.alertar(ciudadano, Escenario.LATITUD + separacion * 0.05, Escenario.LONGITUD);
	}

	private void falsaAlarma(Ciudadano ciudadano) throws Exception {
		long incidente = alertarAparte(ciudadano).incidenteId();
		pedir(post("/incidentes/" + incidente + "/cierre"), escenario.admin(),
				Json.objeto("motivo", "FALSA_ALARMA_VERIFICADA", "detalle", FUNDAMENTO))
				.andExpect(status().isNoContent());
	}

	private ResultActions desactivar(long ciudadanoId, String motivo) throws Exception {
		return pedir(post("/ciudadanos/" + ciudadanoId + "/desactivacion"), escenario.admin(),
				Json.objeto("motivo", motivo));
	}

	private ResultActions ingresar(Ciudadano ciudadano) throws Exception {
		return pedir(post("/auth/ciudadano"), null,
				Json.objeto("idToken", VerificadorDeTelefonoFalso.tokenDe(ciudadano.telefono())));
	}

	@Test
	@DisplayName("PB-23 · CP-23-01 · cerrar como falsa alarma pide cómo se verificó, y queda en el incidente")
	void falsaAlarmaConFundamento() throws Exception {
		long incidente = escenario.alertar(escenario.ciudadano()).incidenteId();

		pedir(post("/incidentes/" + incidente + "/cierre"), escenario.admin(),
				Json.objeto("motivo", "FALSA_ALARMA_VERIFICADA", "detalle", "   "))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value("VALIDACION"));
		pedir(post("/incidentes/" + incidente + "/cierre"), escenario.admin(),
				Json.objeto("motivo", "FALSA_ALARMA_VERIFICADA", "detalle", FUNDAMENTO))
				.andExpect(status().isNoContent());

		pedir(get("/incidentes/" + incidente), escenario.admin())
				.andExpect(jsonPath("$.estado").value("FALSA_ALARMA"))
				.andExpect(jsonPath("$.detalleCierre").value(FUNDAMENTO));
	}

	@Test
	@DisplayName("PB-23 · CP-23-01 · la central ve quiénes tienen falsas alarmas verificadas, los que más primero")
	void listaPorCiudadano() throws Exception {
		Ciudadano reincidente = escenario.ciudadano();
		Ciudadano unaVez = escenario.ciudadano();
		Ciudadano retiro = escenario.ciudadano();
		falsaAlarma(reincidente);
		falsaAlarma(reincidente);
		falsaAlarma(unaVez);
		Alerta retirada = alertarAparte(retiro);
		pedir(post("/alertas/" + retirada.alertaId() + "/cancelacion"), retiro.token(),
				Json.objeto("motivo", "FALSA_ALARMA")).andExpect(status().isOk());

		pedir(get("/ciudadanos/falsas-alarmas"), escenario.admin())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].id").value(reincidente.id()))
				.andExpect(jsonPath("$[0].falsasAlarmas").value(2))
				.andExpect(jsonPath("$[0].activo").value(true))
				.andExpect(jsonPath("$[1].id").value(unaVez.id()))
				.andExpect(jsonPath("$[1].falsasAlarmas").value(1));
	}

	@Test
	@DisplayName("PB-23 · CP-23-04 · el historial separa las falsas alarmas verificadas de lo que el ciudadano retiró")
	void historialDistingue() throws Exception {
		Ciudadano ciudadano = escenario.ciudadano();
		falsaAlarma(ciudadano);
		Alerta retirada = alertarAparte(ciudadano);
		pedir(post("/alertas/" + retirada.alertaId() + "/cancelacion"), ciudadano.token(),
				Json.objeto("motivo", "FALSA_ALARMA")).andExpect(status().isOk());
		// Cerrado solo por lo que reportó la tripulación: no lleva fundamento de la central y no cuenta.
		long sinFundamento = alertarAparte(ciudadano).incidenteId();
		jdbc.update("""
				update incidente set estado = 'FALSA_ALARMA', motivo_cierre = 'FALSA_ALARMA_VERIFICADA',
				fecha_hora_cierre = now() where id = ?""", sinFundamento);

		pedir(get("/ciudadanos/" + ciudadano.id() + "/historial"), escenario.admin())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.falsasAlarmas.length()").value(1))
				.andExpect(jsonPath("$.falsasAlarmas[0].fundamento").value(FUNDAMENTO))
				.andExpect(jsonPath("$.falsasAlarmas[0].verificadaPor").isNotEmpty())
				.andExpect(jsonPath("$.otrasAlertas.length()").value(2))
				.andExpect(jsonPath("$.otrasAlertas[?(@.alertaId == " + retirada.alertaId() + ")].motivoRetiro")
						.value("FALSA_ALARMA"))
				.andExpect(jsonPath("$.sanciones.length()").value(0));
	}

	@Test
	@DisplayName("PB-23 · CP-23-02 · desactivar la cuenta pide un motivo y queda quién, cuándo y por qué")
	void desactivaConMotivo() throws Exception {
		Ciudadano ciudadano = escenario.ciudadano();
		falsaAlarma(ciudadano);

		desactivar(ciudadano.id(), " ").andExpect(status().isBadRequest());
		desactivar(ciudadano.id(), "Tres falsas alarmas en una semana.")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.activo").value(false))
				.andExpect(jsonPath("$.sanciones[0].tipo").value("DESACTIVACION"))
				.andExpect(jsonPath("$.sanciones[0].motivo").value("Tres falsas alarmas en una semana."))
				.andExpect(jsonPath("$.sanciones[0].administrador").isNotEmpty())
				.andExpect(jsonPath("$.sanciones[0].fechaHora").isNotEmpty());

		desactivar(ciudadano.id(), "Otra vez.")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("TRANSICION_INVALIDA"));
	}

	@Test
	@DisplayName("PB-23 · CP-23-02 · una cuenta desactivada pierde la sesión y no puede volver a entrar ni alertar")
	void bloqueaAccesoYAlertas() throws Exception {
		Ciudadano ciudadano = escenario.ciudadano();

		desactivar(ciudadano.id(), "Llamadas de broma reiteradas.").andExpect(status().isOk());

		pedir(post("/alertas"), ciudadano.token(), Json.objeto("latitud", Escenario.LATITUD, "longitud",
				Escenario.LONGITUD, "origenUbicacion", "GPS"))
				.andExpect(status().isUnauthorized());
		ingresar(ciudadano)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("CUENTA_DESACTIVADA"));
	}

	@Test
	@DisplayName("PB-23 · CP-23-03 · reactivar también queda registrado, y la persona vuelve a entrar con su historial")
	void reactivaConservandoHistorial() throws Exception {
		Ciudadano ciudadano = escenario.ciudadano();
		falsaAlarma(ciudadano);
		desactivar(ciudadano.id(), "Llamadas de broma reiteradas.").andExpect(status().isOk());

		pedir(post("/ciudadanos/" + ciudadano.id() + "/reactivacion"), escenario.admin(),
				Json.objeto("motivo", "Se comprobó que el teléfono lo usaba otra persona."))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.activo").value(true))
				.andExpect(jsonPath("$.sanciones.length()").value(2))
				.andExpect(jsonPath("$.sanciones[0].tipo").value("REACTIVACION"))
				.andExpect(jsonPath("$.sanciones[1].tipo").value("DESACTIVACION"))
				.andExpect(jsonPath("$.falsasAlarmas.length()").value(1));

		ingresar(ciudadano)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.ciudadano.id").value(ciudadano.id()));
	}

	@Test
	@DisplayName("PB-23 · CP-23-02 · solo se desactivan cuentas: una persona a cargo no existe para esto")
	void soloCuentasPropias() throws Exception {
		long persona = escenario.persona(escenario.ciudadano(), "Rosa Méndez");

		desactivar(persona, "No corresponde.").andExpect(status().isNotFound());
		pedir(get("/ciudadanos/" + persona + "/historial"), escenario.admin()).andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("PB-23 · CP-23-02 · solo la central ve las falsas alarmas y desactiva cuentas")
	void soloLaCentral() throws Exception {
		Ciudadano ciudadano = escenario.ciudadano();

		pedir(get("/ciudadanos/falsas-alarmas"), ciudadano.token()).andExpect(status().isForbidden());
		pedir(post("/ciudadanos/" + ciudadano.id() + "/desactivacion"), escenario.paramedicoActivado().token(),
				Json.objeto("motivo", "x")).andExpect(status().isForbidden());
		pedir(get("/ciudadanos/falsas-alarmas"), null).andExpect(status().isUnauthorized());
	}
}
