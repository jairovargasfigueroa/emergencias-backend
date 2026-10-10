package com.uem.ambulancias.flujos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.uem.ambulancias.emergencias.service.PlanificadorDeTraslados;
import com.uem.ambulancias.soporte.Escenario;
import com.uem.ambulancias.soporte.Escenario.Ciudadano;
import com.uem.ambulancias.soporte.Escenario.Paramedico;
import com.uem.ambulancias.soporte.Json;
import com.uem.ambulancias.soporte.PruebaIT;

/**
 * El Sprint 4 de punta a punta: un traslado programado desde que la familia lo pide hasta que la unidad vuelve a
 * quedar libre, y uno que nadie tomó y resolvió la central.
 */
@Tag("flujo")
@Tag("sprint-4")
class TrasladoProgramadoIT extends PruebaIT {

	@Autowired
	private PlanificadorDeTraslados planificador;

	@Test
	@DisplayName("Flujo Sprint 4 · PB-25, PB-27 y PB-28 · la mamá va en camilla a su cita en la clínica, y la unidad vuelve a quedar libre")
	void trasladoProgramado() throws Exception {
		long clinica = escenario.centroDeSalud("Clínica Foianini", true);
		Paramedico unidad = escenario.unidadParaTraslados("II", 0.002);
		escenario.recibirAvisos(unidad, "push-tripulacion");
		Ciudadano hijo = escenario.ciudadano();
		escenario.recibirAvisos(hijo, "push-familia");

		// PB-25: el hijo agenda a su mamá y pide el traslado en camilla para su cita de mañana.
		long mama = escenario.persona(hijo, "Rosa Méndez");
		Map<String, Object> pedido = Escenario.pedidoDeTraslado();
		pedido.remove("destinoLatitud");
		pedido.remove("destinoLongitud");
		pedido.put("pasajeroId", mama);
		pedido.put("movilidad", "CAMILLA");
		pedido.put("centroSaludDestinoId", clinica);
		pedido.put("horaCita", Instant.now().plus(Duration.ofDays(1)).truncatedTo(ChronoUnit.SECONDS));
		long traslado = escenario.pedirTraslado(hijo, pedido);

		// PB-27: hoy no se busca unidad; el día de la cita, a la hora de salir, se asigna sola.
		planificador.barrer();
		assertThat(estado(traslado)).isEqualTo("PROGRAMADO");
		jdbc.update("""
				update traslado set hora_salida_estimada = now() - interval '1 minute',
				  hora_limite_salida = now() + interval '19 minutes' where id = ?
				""", traslado);
		planificador.barrer();
		assertThat(estado(traslado)).isEqualTo("ASIGNADO");
		assertThat(publicaciones.avisosDeTrasladoA("push-tripulacion", traslado)).containsExactly("ASIGNADO");

		// PB-28: la tripulación lo ve, la busca, la lleva y la entrega en la clínica.
		long atencion = Json.numero(cuerpo(pedir(get("/paramedicos/actual/atencion"), unidad.token())
				.andExpect(jsonPath("$.traslado.pasajero").value("Rosa Méndez"))
				.andExpect(jsonPath("$.traslado.centroSaludDestino").value("Clínica Foianini"))), "$.id");
		escenario.hastaElHospital(unidad, atencion);
		pedir(post("/atenciones/" + atencion + "/entrega"), unidad.token(), Json.objeto("latitud",
				Escenario.LATITUD, "longitud", Escenario.LONGITUD + 0.01, "centroSaludId", clinica))
				.andExpect(status().isOk());
		assertThat(estado(traslado)).isEqualTo("COMPLETADO");

		// Se libera aparte, y recién ahí la unidad vuelve a estar disponible.
		estadoDeLaAmbulancia(unidad.ambulanciaId(), "EN_ATENCION");
		escenario.liberar(unidad, atencion);
		estadoDeLaAmbulancia(unidad.ambulanciaId(), "DISPONIBLE");

		// La familia y la central lo ven terminado.
		assertThat(publicaciones.avisosAlCiudadano("push-familia"))
				.extracting(aviso -> aviso.datos().get("tipo"))
				.containsExactly("UNIDAD_ASIGNADA", "UNIDAD_EN_LA_PUERTA");
		pedir(get("/traslados/mios"), hijo.token()).andExpect(jsonPath("$[0].estado").value("COMPLETADO"));
		pedir(get("/traslados/" + traslado), escenario.admin())
				.andExpect(jsonPath("$.traslado.estado").value("COMPLETADO"))
				.andExpect(jsonPath("$.placa").isNotEmpty())
				.andExpect(jsonPath("$.hitos.length()").value(6));
	}

	@Test
	@DisplayName("Flujo Sprint 4 · PB-27 y PB-28 · nadie lo toma, la central asigna una unidad a mano, y el paciente no estaba listo")
	void laCentralLoResuelve() throws Exception {
		Paramedico sinPosicion = escenario.paramedicoEnTurno("II");
		Ciudadano ciudadano = escenario.ciudadano();
		long traslado = escenario.pedirTraslado(ciudadano, Escenario.pedidoDeTraslado());

		// PB-27: el barrido no encuentra unidad con señal, y el traslado aparece en la bandeja de la central.
		planificador.barrer();
		pedir(get("/traslados/problemas"), escenario.admin())
				.andExpect(jsonPath("$[0].traslado.id").value(traslado))
				.andExpect(jsonPath("$[0].problema").value("SIN_UNIDAD"));
		pedir(post("/traslados/" + traslado + "/asignar"), escenario.admin(),
				Json.objeto("ambulanciaId", sinPosicion.ambulanciaId()))
				.andExpect(status().isOk());
		pedir(get("/traslados/problemas"), escenario.admin()).andExpect(jsonPath("$.length()").value(0));

		// PB-28: llegan, el paciente no está listo, esperan los 15 minutos y se retiran sin traslado.
		long atencion = jdbc.queryForObject("select id from atencion where traslado_id = ?", Long.class, traslado);
		escenario.marcar(sinPosicion, atencion, "llegada");
		pedir(post("/atenciones/" + atencion + "/no-listo"), sinPosicion.token()).andExpect(status().isOk());
		jdbc.update("update atencion set espera_hasta = now() - interval '1 minute' where id = ?", atencion);
		pedir(post("/atenciones/" + atencion + "/sin-traslado"), sinPosicion.token(), Json.objeto("latitud",
				Escenario.LATITUD, "longitud", Escenario.LONGITUD, "motivo", "PACIENTE_NO_LISTO"))
				.andExpect(status().isOk());
		escenario.liberar(sinPosicion, atencion);

		assertThat(estado(traslado)).isEqualTo("NO_REALIZADO");
		pedir(get("/traslados/" + traslado), escenario.admin())
				.andExpect(jsonPath("$.motivoSinTraslado").value("PACIENTE_NO_LISTO"));
		estadoDeLaAmbulancia(sinPosicion.ambulanciaId(), "DISPONIBLE");
	}

	private String estado(long traslado) {
		return jdbc.queryForObject("select estado from traslado where id = ?", String.class, traslado);
	}

	private void estadoDeLaAmbulancia(long ambulancia, String estado) throws Exception {
		pedir(get("/ambulancias"), escenario.admin())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.id == " + ambulancia + ")].estado", hasItem(estado)));
	}

}
