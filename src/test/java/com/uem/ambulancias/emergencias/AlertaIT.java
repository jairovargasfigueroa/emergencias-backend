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
import com.uem.ambulancias.soporte.Escenario.Paramedico;
import com.uem.ambulancias.soporte.Json;
import com.uem.ambulancias.soporte.PruebaIT;

@Tag("PB-05")
class AlertaIT extends PruebaIT {

	@Test
	@DisplayName("PB-05 · CP-05-01 · con la ubicación del GPS la alerta abre un incidente activo en ese punto")
	void alertaConGps() throws Exception {
		Ciudadano ciudadano = escenario.ciudadano();

		String respuesta = cuerpo(emitir(ciudadano, Json.objeto("latitud", Escenario.LATITUD, "longitud",
				Escenario.LONGITUD, "origenUbicacion", "GPS"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.alertaId").isNumber())
				.andExpect(jsonPath("$.fechaHora").isNotEmpty()));

		incidente(Json.numero(respuesta, "$.incidenteId"))
				.andExpect(jsonPath("$.estado").value("ACTIVO"))
				.andExpect(jsonPath("$.latitud").value(Escenario.LATITUD))
				.andExpect(jsonPath("$.longitud").value(Escenario.LONGITUD))
				.andExpect(jsonPath("$.cantidadAlertas").value(1))
				.andExpect(jsonPath("$.alertas[0].id").value(Json.numero(respuesta, "$.alertaId")))
				.andExpect(jsonPath("$.alertas[0].estado").value("VINCULADA"))
				.andExpect(jsonPath("$.alertas[0].origenUbicacion").value("GPS"))
				.andExpect(jsonPath("$.alertas[0].emisor.id").value(ciudadano.id()));
	}

	@Test
	@DisplayName("PB-05 · CP-05-01 · sin GPS, el punto que marca a mano en el mapa vale igual")
	void alertaConPinManual() throws Exception {
		String respuesta = cuerpo(emitir(escenario.ciudadano(), Json.objeto("latitud", Escenario.LATITUD,
				"longitud", Escenario.LONGITUD, "origenUbicacion", "MANUAL"))
				.andExpect(status().isCreated()));

		incidente(Json.numero(respuesta, "$.incidenteId"))
				.andExpect(jsonPath("$.alertas[0].origenUbicacion").value("MANUAL"));
	}

	@Test
	@DisplayName("PB-05 · CP-05-02 · sin ubicación o con una ubicación imposible no se emite")
	void ubicacionInvalida() throws Exception {
		Ciudadano ciudadano = escenario.ciudadano();

		emitir(ciudadano, Json.objeto("origenUbicacion", "GPS"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value("VALIDACION"));
		emitir(ciudadano, Json.objeto("latitud", 91, "longitud", Escenario.LONGITUD, "origenUbicacion", "GPS"))
				.andExpect(status().isBadRequest());
		emitir(ciudadano, Json.objeto("latitud", Escenario.LATITUD, "longitud", Escenario.LONGITUD))
				.andExpect(status().isBadRequest());
		emitir(ciudadano, Json.objeto("latitud", Escenario.LATITUD, "longitud", Escenario.LONGITUD,
				"origenUbicacion", "GPS", "cantidadAfectados", -1))
				.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("PB-05 · CP-05-03 · los afectados y la descripción son opcionales y, si llegan, quedan guardados")
	void datosOpcionales() throws Exception {
		String respuesta = cuerpo(emitir(escenario.ciudadano(), Json.objeto("latitud", Escenario.LATITUD,
				"longitud", Escenario.LONGITUD, "origenUbicacion", "GPS", "cantidadAfectados", 3, "descripcion",
				"  Choque de moto en la esquina  "))
				.andExpect(status().isCreated()));

		incidente(Json.numero(respuesta, "$.incidenteId"))
				.andExpect(jsonPath("$.cantidadAfectados").value(3))
				.andExpect(jsonPath("$.alertas[0].cantidadAfectados").value(3))
				.andExpect(jsonPath("$.alertas[0].descripcion").value("Choque de moto en la esquina"));
	}

	@Test
	@DisplayName("PB-05 · CP-05-03 · mientras espera completa los datos, y el total del incidente solo sube")
	void completarDetalles() throws Exception {
		Ciudadano ciudadano = escenario.ciudadano();
		Alerta alerta = escenario.alertar(ciudadano);

		detalles(ciudadano, alerta, Json.objeto("cantidadAfectados", 3, "descripcion", "Hay un herido grave"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.incidenteId").value(alerta.incidenteId()));
		detalles(ciudadano, alerta, Json.objeto("cantidadAfectados", 2)).andExpect(status().isOk());

		incidente(alerta.incidenteId())
				.andExpect(jsonPath("$.cantidadAfectados").value(3))
				.andExpect(jsonPath("$.alertas[0].cantidadAfectados").value(2))
				.andExpect(jsonPath("$.alertas[0].descripcion").value("Hay un herido grave"));
	}

	@Test
	@DisplayName("PB-05 · CP-05-03 · no se completan datos de una alerta ajena ni después de que llegó la unidad")
	void detallesNoPermitidos() throws Exception {
		Ciudadano ciudadano = escenario.ciudadano();
		Alerta alerta = escenario.alertar(ciudadano);

		detalles(escenario.ciudadano(), alerta, Json.objeto("cantidadAfectados", 2))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("ALERTA_AJENA"));

		Paramedico paramedico = escenario.paramedicoEnTurno();
		long atencion = escenario.tomar(paramedico, alerta.incidenteId());
		llegar(paramedico, atencion);

		detalles(ciudadano, alerta, Json.objeto("cantidadAfectados", 2))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("DETALLES_NO_EDITABLES"));
	}

	@Test
	@DisplayName("PB-05 · CP-05-04 · si nadie más pidió y nadie salió, retirar el pedido cierra el incidente")
	void cancelarSinUnidad() throws Exception {
		Ciudadano ciudadano = escenario.ciudadano();
		Alerta alerta = escenario.alertar(ciudadano);

		cancelar(ciudadano, alerta, Json.objeto("motivo", "FALSA_ALARMA", "emisorEsPaciente", true))
				.andExpect(status().isOk());

		incidente(alerta.incidenteId())
				.andExpect(jsonPath("$.estado").value("CANCELADO"))
				.andExpect(jsonPath("$.fechaHoraCierre").isNotEmpty())
				.andExpect(jsonPath("$.alertas[0].estado").value("CANCELADA"))
				.andExpect(jsonPath("$.alertas[0].motivoCancelacion").value("FALSA_ALARMA"))
				.andExpect(jsonPath("$.alertas[0].emisorEsPaciente").value(true));
		cancelar(ciudadano, alerta, Json.objeto("motivo", "ERROR"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("TRANSICION_INVALIDA"));
	}

	@Test
	@DisplayName("PB-05 · CP-05-04 · con una unidad en camino, retirar el pedido no cierra el incidente")
	void cancelarConUnidadEnCamino() throws Exception {
		Ciudadano ciudadano = escenario.ciudadano();
		Alerta alerta = escenario.alertar(ciudadano);
		escenario.tomar(escenario.paramedicoEnTurno(), alerta.incidenteId());

		cancelar(ciudadano, alerta, Json.objeto("motivo", "YA_FUE_ATENDIDO")).andExpect(status().isOk());

		incidente(alerta.incidenteId())
				.andExpect(jsonPath("$.estado").value("EN_ATENCION"))
				.andExpect(jsonPath("$.alertas[0].estado").value("CANCELADA"));
	}

	@Test
	@DisplayName("PB-05 · CP-05-04 · si otra persona también pidió, el incidente sigue abierto")
	void cancelarConOtroPedido() throws Exception {
		Ciudadano primero = escenario.ciudadano();
		Alerta alerta = escenario.alertar(primero);
		escenario.alertar(escenario.ciudadano());

		cancelar(primero, alerta, Json.objeto("motivo", "ERROR")).andExpect(status().isOk());

		incidente(alerta.incidenteId()).andExpect(jsonPath("$.estado").value("ACTIVO"));
	}

	@Test
	@DisplayName("PB-05 · CP-05-04 · no se retira un pedido ajeno, ni uno cuya unidad ya llegó, ni sin motivo")
	void cancelarNoPermitido() throws Exception {
		Ciudadano ciudadano = escenario.ciudadano();
		Alerta alerta = escenario.alertar(ciudadano);

		cancelar(ciudadano, alerta, Json.objeto("emisorEsPaciente", true))
				.andExpect(status().isBadRequest());
		cancelar(escenario.ciudadano(), alerta, Json.objeto("motivo", "ERROR"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("ALERTA_AJENA"));

		Paramedico paramedico = escenario.paramedicoEnTurno();
		llegar(paramedico, escenario.tomar(paramedico, alerta.incidenteId()));

		cancelar(ciudadano, alerta, Json.objeto("motivo", "YA_FUE_ATENDIDO"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("TRANSICION_INVALIDA"));
	}

	@Test
	@DisplayName("PB-05 · CP-05-05 · solo un ciudadano con sesión puede pedir una ambulancia")
	void soloCiudadanos() throws Exception {
		String cuerpo = Json.objeto("latitud", Escenario.LATITUD, "longitud", Escenario.LONGITUD,
				"origenUbicacion", "GPS");

		pedir(post("/alertas"), escenario.paramedicoActivado().token(), cuerpo).andExpect(status().isForbidden());
		pedir(post("/alertas"), escenario.admin(), cuerpo).andExpect(status().isForbidden());
		pedir(post("/alertas"), null, cuerpo).andExpect(status().isUnauthorized());
	}

	private ResultActions emitir(Ciudadano ciudadano, String cuerpo) throws Exception {
		return pedir(post("/alertas"), ciudadano.token(), cuerpo);
	}

	private ResultActions detalles(Ciudadano ciudadano, Alerta alerta, String cuerpo) throws Exception {
		return pedir(post("/alertas/" + alerta.alertaId() + "/detalles"), ciudadano.token(), cuerpo);
	}

	private ResultActions cancelar(Ciudadano ciudadano, Alerta alerta, String cuerpo) throws Exception {
		return pedir(post("/alertas/" + alerta.alertaId() + "/cancelacion"), ciudadano.token(), cuerpo);
	}

	private void llegar(Paramedico paramedico, long atencion) throws Exception {
		pedir(post("/atenciones/" + atencion + "/llegada"), paramedico.token(),
				Json.objeto("latitud", Escenario.LATITUD, "longitud", Escenario.LONGITUD))
				.andExpect(status().isOk());
	}

	/** El incidente como lo ve la central. */
	private ResultActions incidente(long id) throws Exception {
		return pedir(get("/incidentes/" + id), escenario.admin()).andExpect(status().isOk());
	}

}
