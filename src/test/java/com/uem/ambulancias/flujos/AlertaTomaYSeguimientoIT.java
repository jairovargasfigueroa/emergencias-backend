package com.uem.ambulancias.flujos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.uem.ambulancias.emergencias.domain.EstadoAtencion;
import com.uem.ambulancias.emergencias.domain.EstadoIncidente;
import com.uem.ambulancias.emergencias.service.SeguimientoPublicado;
import com.uem.ambulancias.soporte.Escenario;
import com.uem.ambulancias.soporte.Escenario.Alerta;
import com.uem.ambulancias.soporte.Escenario.Ciudadano;
import com.uem.ambulancias.soporte.Escenario.Paramedico;
import com.uem.ambulancias.soporte.Json;
import com.uem.ambulancias.soporte.PruebaIT;

/**
 * El Sprint 1 de punta a punta, como pasa en la vida real: la central arma la unidad, el paramédico activa su acceso
 * y entra de turno, un ciudadano pide ayuda, la unidad más cercana toma el caso y quien pidió la ve venir.
 */
@Tag("flujo")
@Tag("sprint-1")
class AlertaTomaYSeguimientoIT extends PruebaIT {

	@Test
	@DisplayName("Flujo Sprint 1 · PB-01 a PB-08 · de armar la unidad a que la ambulancia llega a quien pidió ayuda")
	void deLaUnidadALaLlegada() throws Exception {
		String central = escenario.admin();

		// PB-01: la central registra la unidad y al paramédico, y se los asigna.
		long ambulancia = Json.numero(cuerpo(pedir(post("/ambulancias"), central,
				Json.objeto("placa", "4521 KTR", "tipoUnidad", "II"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.placa").value("4521KTR"))), "$.id");
		String telefono = Escenario.nuevoTelefono();
		long paramedicoId = escenario.paramedicoRegistrado(telefono);
		escenario.asignar(paramedicoId, ambulancia);

		// PB-02: con el código que le da la central crea su PIN y vincula su teléfono.
		String codigo = escenario.codigoDeActivacion(paramedicoId);
		String activacion = cuerpo(pedir(post("/auth/paramedico/activacion"), null,
				Json.objeto("telefono", telefono, "codigo", codigo, "pin", Escenario.PIN))
				.andExpect(status().isOk()));
		Paramedico paramedico = new Paramedico(paramedicoId, telefono, Json.texto(activacion, "$.token"),
				Json.texto(activacion, "$.claveDispositivo"), ambulancia);
		escenario.recibirAvisos(paramedico, "push-paramedico");

		// PB-04: entra de turno con su PIN y su unidad queda disponible, a 1 km del lugar.
		escenario.iniciarTurno(paramedico);
		escenario.reportarPosicion(paramedico, Escenario.LATITUD + 0.009, Escenario.LONGITUD);
		estadoDeLaAmbulancia(ambulancia, "DISPONIBLE");

		// PB-03 y PB-05: un ciudadano verificado pide ayuda con lo que sabe.
		Ciudadano ciudadano = escenario.ciudadano();
		escenario.recibirAvisos(ciudadano, "push-ciudadano");
		String respuesta = cuerpo(pedir(post("/alertas"), ciudadano.token(), Json.objeto("latitud", Escenario.LATITUD,
				"longitud", Escenario.LONGITUD, "origenUbicacion", "GPS", "cantidadAfectados", 1, "descripcion",
				"Mi papá se desmayó y no reacciona"))
				.andExpect(status().isCreated()));
		Alerta alerta = new Alerta(Json.numero(respuesta, "$.alertaId"), Json.numero(respuesta, "$.incidenteId"));
		long incidente = alerta.incidenteId();

		// PB-06: al paramédico le llega el push y lo ve en su lista.
		assertThat(publicaciones.ultimoAvisoDeIncidenteNuevo()).containsExactly("push-paramedico");
		assertThat(publicaciones.incidentesAbiertos(incidente).getLast().descripciones())
				.containsExactly("Mi papá se desmayó y no reacciona");

		// PB-05: un vecino avisa de lo mismo a 100 m y se suma al mismo incidente, sin otro push a las unidades.
		Alerta vecino = escenario.alertar(escenario.ciudadano(), Escenario.LATITUD + Escenario.CIEN_METROS,
				Escenario.LONGITUD);
		assertThat(vecino.incidenteId()).isEqualTo(incidente);
		assertThat(publicaciones.cantidadDeAvisosDeIncidenteNuevo()).isEqualTo(1);

		// PB-07: lo toma y sale en camino.
		long atencion = Json.numero(cuerpo(pedir(post("/incidentes/" + incidente + "/tomar"), paramedico.token())
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.estado").value("EN_CAMINO"))), "$.id");
		estadoDeLaAmbulancia(ambulancia, "EN_ATENCION");

		// PB-08: el ciudadano ve qué unidad va, recibe el push y la ve acercarse.
		SeguimientoPublicado enCamino = publicaciones.ultimoSeguimiento(incidente);
		assertThat(enCamino.estado()).isEqualTo(EstadoIncidente.EN_ATENCION);
		assertThat(enCamino.unidades()).singleElement()
				.satisfies(unidad -> assertThat(unidad.placa()).isEqualTo("4521KTR"));
		escenario.reportarPosicion(paramedico, Escenario.LATITUD + 0.004, Escenario.LONGITUD);
		assertThat(publicaciones.posicionesEnSeguimiento(incidente)).hasSize(1);

		// PB-04: con la atención en curso no puede salir de turno.
		pedir(post("/paramedicos/actual/turno/cierre"), paramedico.token())
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("TRANSICION_INVALIDA"));

		// La unidad llega y quien pidió ayuda se entera.
		pedir(post("/atenciones/" + atencion + "/llegada"), paramedico.token(),
				Json.objeto("latitud", Escenario.LATITUD, "longitud", Escenario.LONGITUD))
				.andExpect(status().isOk());
		assertThat(publicaciones.ultimoSeguimiento(incidente).unidades()).singleElement()
				.satisfies(unidad -> assertThat(unidad.estado()).isEqualTo(EstadoAtencion.EN_EL_LUGAR));
		assertThat(publicaciones.avisosAlCiudadano("push-ciudadano"))
				.extracting(aviso -> aviso.datos().get("tipo"))
				.containsExactly("UNIDAD_EN_CAMINO", "UNIDAD_LLEGO");

		// La central ve todo el caso: dos pedidos, una unidad en el lugar.
		pedir(get("/incidentes/" + incidente), central)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("EN_ATENCION"))
				.andExpect(jsonPath("$.cantidadAlertas").value(2))
				.andExpect(jsonPath("$.unidades[0]").value("4521KTR"))
				.andExpect(jsonPath("$.atenciones[0].estado").value("EN_EL_LUGAR"));
	}

	@Test
	@DisplayName("Flujo Sprint 1 · PB-06 a PB-08 · la unidad se avería en camino y otra toma la posta")
	void otraUnidadTomaLaPosta() throws Exception {
		Paramedico primera = escenario.paramedicoEnTurno();
		escenario.reportarPosicion(primera, Escenario.LATITUD + 0.005, Escenario.LONGITUD);
		escenario.recibirAvisos(primera, "push-primera");
		Paramedico segunda = escenario.paramedicoEnTurno();
		escenario.reportarPosicion(segunda, Escenario.LATITUD + 0.02, Escenario.LONGITUD);
		escenario.recibirAvisos(segunda, "push-segunda");
		Ciudadano ciudadano = escenario.ciudadano();
		escenario.recibirAvisos(ciudadano, "push-ciudadano");

		long incidente = escenario.alertar(ciudadano).incidenteId();
		assertThat(publicaciones.ultimoAvisoDeIncidenteNuevo()).containsExactly("push-primera", "push-segunda");
		long atencion = escenario.tomar(primera, incidente);

		pedir(post("/atenciones/" + atencion + "/cancelar"), primera.token(), Json.objeto("motivo", "AVERIA"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("CANCELADA"));

		estadoDeLaAmbulancia(primera.ambulanciaId(), "FUERA_DE_SERVICIO");
		assertThat(publicaciones.ultimoAvisoDeIncidenteNuevo()).containsExactly("push-segunda");
		assertThat(publicaciones.ultimoSeguimiento(incidente).estado()).isEqualTo(EstadoIncidente.ACTIVO);

		pedir(post("/incidentes/" + incidente + "/tomar"), segunda.token()).andExpect(status().isCreated());

		SeguimientoPublicado seguimiento = publicaciones.ultimoSeguimiento(incidente);
		assertThat(seguimiento.estado()).isEqualTo(EstadoIncidente.EN_ATENCION);
		assertThat(seguimiento.unidades()).singleElement()
				.satisfies(unidad -> assertThat(unidad.ambulanciaId()).isEqualTo(segunda.ambulanciaId()));
		assertThat(publicaciones.avisosAlCiudadano("push-ciudadano"))
				.extracting(aviso -> aviso.datos().get("tipo"))
				.containsExactly("UNIDAD_EN_CAMINO", "BUSCANDO_OTRA_UNIDAD", "UNIDAD_EN_CAMINO");
	}

	private void estadoDeLaAmbulancia(long ambulancia, String estado) throws Exception {
		pedir(get("/ambulancias"), escenario.admin())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.id == " + ambulancia + ")].estado", hasItem(estado)));
	}

}
