package com.uem.ambulancias.flujos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.uem.ambulancias.emergencias.domain.EstadoIncidente;
import com.uem.ambulancias.soporte.Escenario;
import com.uem.ambulancias.soporte.Escenario.Alerta;
import com.uem.ambulancias.soporte.Escenario.Ciudadano;
import com.uem.ambulancias.soporte.Escenario.Paramedico;
import com.uem.ambulancias.soporte.Json;
import com.uem.ambulancias.soporte.PruebaIT;

/**
 * El Sprint 2 de punta a punta: la tripulación lleva una emergencia hasta el final y la central coordina lo que
 * queda sin cubrir o no se va a atender.
 */
@Tag("flujo")
@Tag("sprint-2")
class AtencionCompletaYCierreIT extends PruebaIT {

	@Test
	@DisplayName("Flujo Sprint 2 · PB-09 a PB-13 y PB-15 · de la alerta a la entrega en una clínica, y la unidad libre para la siguiente")
	void atencionCompleta() throws Exception {
		long clinica = escenario.centroDeSalud("Clínica Foianini", true);
		Paramedico paramedico = escenario.paramedicoEnTurno();
		Ciudadano ciudadano = escenario.ciudadano();
		Alerta alerta = escenario.alertar(ciudadano);
		long incidente = alerta.incidenteId();

		// PB-10: mientras espera, cuenta qué pasó.
		pedir(post("/alertas/" + alerta.alertaId() + "/detalles"), ciudadano.token(),
				Json.objeto("cantidadAfectados", 1, "descripcion", "Mi mamá se cayó de la escalera"))
				.andExpect(status().isOk());

		// PB-12: la unidad va, llega, sube a la paciente con sus datos y llega a la clínica.
		long atencion = escenario.tomar(paramedico, incidente);
		escenario.marcar(paramedico, atencion, "llegada");
		pedir(post("/atenciones/" + atencion + "/recogida"), paramedico.token(), Json.objeto("latitud",
				Escenario.LATITUD, "longitud", Escenario.LONGITUD, "nombrePaciente", "Rosa Méndez", "documentoPaciente",
				"3344556 SC"))
				.andExpect(status().isOk());
		escenario.marcar(paramedico, atencion, "hospital");

		// PB-09: la entrega queda en la clínica del catálogo, y el incidente se da por atendido.
		pedir(post("/atenciones/" + atencion + "/entrega"), paramedico.token(), Json.objeto("latitud",
				Escenario.LATITUD, "longitud", Escenario.LONGITUD, "centroSaludId", clinica))
				.andExpect(status().isOk());
		pedir(get("/incidentes/" + incidente), escenario.admin())
				.andExpect(jsonPath("$.estado").value("ATENDIDO"))
				.andExpect(jsonPath("$.atenciones[0].nombrePaciente").value("Rosa Méndez"))
				.andExpect(jsonPath("$.atenciones[0].centroSalud.nombre").value("Clínica Foianini"));
		assertThat(publicaciones.ultimoSeguimiento(incidente).estado()).isEqualTo(EstadoIncidente.ATENDIDO);

		// PB-13: entregar no la libera; liberarse, sí.
		estadoDeLaAmbulancia(paramedico.ambulanciaId(), "EN_ATENCION");
		escenario.liberar(paramedico, atencion);
		estadoDeLaAmbulancia(paramedico.ambulanciaId(), "DISPONIBLE");

		// PB-15: la central ve toda la historia en la bitácora, de lo más nuevo a lo más viejo.
		pedir(get("/operacion"), escenario.admin())
				.andExpect(jsonPath("$.eventos[*].tipo",
						contains("LIBERACION", "ENTREGA", "HOSPITAL", "RECOGIDA", "LLEGADA", "TOMA")));

		// Y la unidad ya puede tomar la siguiente emergencia.
		long siguiente = escenario.alertar(escenario.ciudadano(), Escenario.LATITUD + 0.05, Escenario.LONGITUD)
				.incidenteId();
		pedir(post("/incidentes/" + siguiente + "/tomar"), paramedico.token()).andExpect(status().isCreated());
	}

	@Test
	@DisplayName("Flujo Sprint 2 · PB-13, PB-15 y PB-16 · nadie toma el caso, la central manda una unidad y era una falsa alarma")
	void laCentralCubreElCaso() throws Exception {
		Paramedico paramedico = escenario.paramedicoEnTurno();
		escenario.reportarPosicion(paramedico, Escenario.LATITUD + 0.01, Escenario.LONGITUD);
		long incidente = escenario.alertar(escenario.ciudadano()).incidenteId();

		// PB-15: el caso aparece sin cubrir en el centro de control.
		pedir(get("/operacion"), escenario.admin())
				.andExpect(jsonPath("$.incidentesSinCubrir[*].id", contains((int) incidente)));

		// PB-16: la central elige la candidata más cercana y la manda.
		pedir(get("/incidentes/" + incidente + "/unidades"), escenario.admin())
				.andExpect(jsonPath("$[0].ambulanciaId").value(paramedico.ambulanciaId()));
		pedir(post("/incidentes/" + incidente + "/despacho"), escenario.admin(),
				Json.objeto("ambulanciaId", paramedico.ambulanciaId()))
				.andExpect(status().isNoContent());
		long atencion = Json.numero(cuerpo(pedir(get("/paramedicos/actual/atencion"), paramedico.token())
				.andExpect(status().isOk())), "$.id");

		// PB-13: llegan y no hay nadie: falsa alarma verificada.
		escenario.marcar(paramedico, atencion, "llegada");
		pedir(post("/atenciones/" + atencion + "/sin-traslado"), paramedico.token(), Json.objeto("latitud",
				Escenario.LATITUD, "longitud", Escenario.LONGITUD, "motivo", "NO_HABIA_PACIENTE"))
				.andExpect(status().isOk());
		escenario.liberar(paramedico, atencion);

		pedir(get("/incidentes/" + incidente), escenario.admin())
				.andExpect(jsonPath("$.estado").value("FALSA_ALARMA"))
				.andExpect(jsonPath("$.motivoCierre").value("FALSA_ALARMA_VERIFICADA"));
		pedir(get("/operacion"), escenario.admin()).andExpect(jsonPath("$.incidentesSinCubrir.length()").value(0));
		estadoDeLaAmbulancia(paramedico.ambulanciaId(), "DISPONIBLE");
	}

	@Test
	@DisplayName("Flujo Sprint 2 · PB-11, PB-14 y PB-17 · la unidad se avería, se busca otra y la central cierra el caso que ya atendieron otros")
	void laCentralCierraElCaso() throws Exception {
		Ciudadano ciudadano = escenario.ciudadano();
		escenario.recibirAvisos(ciudadano, "push-ciudadano");
		Ciudadano vecino = escenario.ciudadano();
		Alerta alerta = escenario.alertar(ciudadano);
		Alerta delVecino = escenario.alertar(vecino);
		long incidente = alerta.incidenteId();
		Paramedico paramedico = escenario.paramedicoEnTurno();
		long atencion = escenario.tomar(paramedico, incidente);

		// PB-11: el vecino retira su pedido; la que pidió primero sigue esperando.
		pedir(post("/alertas/" + delVecino.alertaId() + "/cancelacion"), vecino.token(),
				Json.objeto("motivo", "YA_FUE_ATENDIDO"))
				.andExpect(status().isOk());

		// PB-14: la unidad se avería y el caso vuelve a buscar unidad.
		pedir(post("/atenciones/" + atencion + "/cancelar"), paramedico.token(), Json.objeto("motivo", "AVERIA"))
				.andExpect(status().isOk());
		pedir(get("/incidentes/" + incidente), escenario.admin()).andExpect(jsonPath("$.estado").value("ACTIVO"));

		// PB-17: la central llama, confirma que se la llevaron por otro medio y cierra.
		pedir(post("/incidentes/" + incidente + "/cierre"), escenario.admin(),
				Json.objeto("motivo", "ATENDIDO_EXTERNAMENTE"))
				.andExpect(status().isNoContent());

		pedir(get("/incidentes/" + incidente), escenario.admin())
				.andExpect(jsonPath("$.estado").value("ATENDIDO_EXTERNAMENTE"))
				.andExpect(jsonPath("$.atenciones[0].motivoCancelacion").value("AVERIA"))
				.andExpect(jsonPath("$.alertas[1].motivoCancelacion").value("YA_FUE_ATENDIDO"));
		assertThat(publicaciones.fueRetirado(incidente)).isTrue();
		assertThat(publicaciones.avisosAlCiudadano("push-ciudadano"))
				.extracting(aviso -> aviso.datos().get("tipo"))
				.containsExactly("UNIDAD_EN_CAMINO", "BUSCANDO_OTRA_UNIDAD", "CERRADO_POR_LA_CENTRAL");
		estadoDeLaAmbulancia(paramedico.ambulanciaId(), "FUERA_DE_SERVICIO");
	}

	private void estadoDeLaAmbulancia(long ambulancia, String estado) throws Exception {
		pedir(get("/ambulancias"), escenario.admin())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.id == " + ambulancia + ")].estado", hasItem(estado)));
	}

}
