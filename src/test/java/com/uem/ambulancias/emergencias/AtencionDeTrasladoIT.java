package com.uem.ambulancias.emergencias;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import com.uem.ambulancias.emergencias.service.PlanificadorDeTraslados;
import com.uem.ambulancias.soporte.Escenario;
import com.uem.ambulancias.soporte.Escenario.Ciudadano;
import com.uem.ambulancias.soporte.Escenario.Paramedico;
import com.uem.ambulancias.soporte.Json;
import com.uem.ambulancias.soporte.PruebaIT;

@Tag("PB-28")
class AtencionDeTrasladoIT extends PruebaIT {

	@Autowired
	private PlanificadorDeTraslados planificador;

	@Test
	@DisplayName("PB-28 · CP-28-01 · el paramédico ve su traslado con origen, destino, horario, necesidades y a quién busca")
	void veSuTraslado() throws Exception {
		Paramedico unidad = escenario.unidadParaTraslados("II", 0.002);
		Ciudadano ciudadano = escenario.ciudadano();
		long mama = escenario.persona(ciudadano, "Rosa Méndez");
		Map<String, Object> pedido = Escenario.pedidoDeTraslado();
		pedido.put("pasajeroId", mama);
		pedido.put("movilidad", "SILLA_DE_RUEDAS");
		pedido.put("oxigeno", true);
		pedido.put("observaciones", "Usa oxígeno las 24 horas");
		pedido.put("origenReferencia", "Casa de rejas verdes");
		pedido.put("contactoNombre", "Pedro Méndez");
		pedido.put("contactoTelefono", "70033344");
		Asignado asignado = asignar(ciudadano, pedido);

		pedir(get("/paramedicos/actual/atencion"), unidad.token())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(asignado.atencion()))
				.andExpect(jsonPath("$.incidenteId").doesNotExist())
				.andExpect(jsonPath("$.estado").value("EN_CAMINO"))
				.andExpect(jsonPath("$.nombrePaciente").value("Rosa Méndez"))
				.andExpect(jsonPath("$.traslado.id").value(asignado.traslado()))
				.andExpect(jsonPath("$.traslado.pasajero").value("Rosa Méndez"))
				.andExpect(jsonPath("$.traslado.movilidad").value("SILLA_DE_RUEDAS"))
				.andExpect(jsonPath("$.traslado.oxigeno").value(true))
				.andExpect(jsonPath("$.traslado.observaciones").value("Usa oxígeno las 24 horas"))
				.andExpect(jsonPath("$.traslado.origen.latitud").value(Escenario.LATITUD))
				.andExpect(jsonPath("$.traslado.destino.latitud").value(Escenario.LATITUD + 0.018))
				.andExpect(jsonPath("$.traslado.origenReferencia").value("Casa de rejas verdes"))
				.andExpect(jsonPath("$.traslado.contactoTelefono").value("70033344"))
				.andExpect(jsonPath("$.traslado.horaRecogidaDesde").isNotEmpty())
				.andExpect(jsonPath("$.traslado.horaRecogidaHasta").isNotEmpty());
		pedir(get("/paramedicos/actual/traslados"), unidad.token())
				.andExpect(jsonPath("$[0].traslado.id").value(asignado.traslado()));
	}

	@Test
	@DisplayName("PB-28 · CP-28-02 · los hitos van en orden hasta la entrega, y el traslado queda completado")
	void hitosHastaLaEntrega() throws Exception {
		Paramedico unidad = escenario.unidadParaTraslados("II", 0.002);
		Ciudadano ciudadano = escenario.ciudadano();
		escenario.recibirAvisos(ciudadano, "push-ciudadano");
		long clinica = escenario.centroDeSalud("Clínica Foianini", true);
		Asignado asignado = asignar(ciudadano, Escenario.pedidoDeTraslado());

		hito(unidad, asignado, "recogida")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("TRANSICION_INVALIDA"));
		escenario.hastaElHospital(unidad, asignado.atencion());
		pedir(post("/atenciones/" + asignado.atencion() + "/entrega"), unidad.token(), Json.objeto("latitud",
				Escenario.LATITUD, "longitud", Escenario.LONGITUD, "centroSaludId", clinica))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("PACIENTE_ENTREGADO"));

		assertThat(estado(asignado)).isEqualTo("COMPLETADO");
		assertThat(publicaciones.avisosAlCiudadano("push-ciudadano"))
				.extracting(aviso -> aviso.datos().get("tipo"))
				.contains("UNIDAD_ASIGNADA", "UNIDAD_EN_LA_PUERTA");
		pedir(get("/traslados/" + asignado.traslado()), escenario.admin())
				.andExpect(jsonPath("$.traslado.estado").value("COMPLETADO"))
				.andExpect(jsonPath("$.estadoAtencion").value("PACIENTE_ENTREGADO"));
	}

	@Test
	@DisplayName("PB-28 · CP-28-04 · terminar el traslado no libera la unidad: se libera aparte, y el traslado sigue completado")
	void liberarAparte() throws Exception {
		Paramedico unidad = escenario.unidadParaTraslados("II", 0.002);
		Asignado asignado = asignar(escenario.ciudadano(), Escenario.pedidoDeTraslado());
		escenario.hastaElHospital(unidad, asignado.atencion());
		pedir(post("/atenciones/" + asignado.atencion() + "/entrega"), unidad.token(),
				Json.objeto("latitud", Escenario.LATITUD, "longitud", Escenario.LONGITUD))
				.andExpect(status().isOk());
		estadoDeLaAmbulancia(unidad.ambulanciaId(), "EN_ATENCION");

		escenario.liberar(unidad, asignado.atencion());

		estadoDeLaAmbulancia(unidad.ambulanciaId(), "DISPONIBLE");
		assertThat(estado(asignado)).isEqualTo("COMPLETADO");
		pedir(get("/paramedicos/actual/atencion"), unidad.token()).andExpect(status().isNoContent());
	}

	@Test
	@Tag("PB-13")
	@DisplayName("PB-13 · CP-13-05 · paciente no listo: se espera 15 minutos y recién ahí la unidad se retira, sin traslado")
	void pacienteNoListo() throws Exception {
		Paramedico unidad = escenario.unidadParaTraslados("II", 0.002);
		Asignado asignado = asignar(escenario.ciudadano(), Escenario.pedidoDeTraslado());
		escenario.marcar(unidad, asignado.atencion(), "llegada");

		sinTraslado(unidad, asignado, "PACIENTE_NO_LISTO")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("ESPERA_EN_CURSO"));
		String respuesta = cuerpo(pedir(post("/atenciones/" + asignado.atencion() + "/no-listo"), unidad.token())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.horaAvisoNoListo").isNotEmpty()));
		Instant aviso = Instant.parse(Json.texto(respuesta, "$.horaAvisoNoListo"));
		Instant hasta = Instant.parse(Json.texto(respuesta, "$.esperaHasta"));
		assertThat(Duration.between(aviso, hasta)).isEqualTo(Duration.ofMinutes(15));
		sinTraslado(unidad, asignado, "PACIENTE_NO_LISTO")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("ESPERA_EN_CURSO"));

		jdbc.update("update atencion set espera_hasta = now() - interval '1 minute' where id = ?",
				asignado.atencion());
		sinTraslado(unidad, asignado, "PACIENTE_NO_LISTO")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("SIN_TRASLADO"));

		assertThat(estado(asignado)).isEqualTo("NO_REALIZADO");
		estadoDeLaAmbulancia(unidad.ambulanciaId(), "EN_ATENCION");
	}

	@Test
	@Tag("PB-13")
	@DisplayName("PB-13 · CP-13-05 · 'no listo' es de traslados, y se marca en la puerta")
	void noListoSoloEnLaPuerta() throws Exception {
		Paramedico unidad = escenario.unidadParaTraslados("II", 0.002);
		Asignado asignado = asignar(escenario.ciudadano(), Escenario.pedidoDeTraslado());
		pedir(post("/atenciones/" + asignado.atencion() + "/no-listo"), unidad.token())
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("TRANSICION_INVALIDA"));

		Paramedico deEmergencia = escenario.paramedicoEnTurno();
		long atencion = escenario.tomar(deEmergencia, escenario.alertar(escenario.ciudadano()).incidenteId());
		escenario.marcar(deEmergencia, atencion, "llegada");
		pedir(post("/atenciones/" + atencion + "/no-listo"), deEmergencia.token())
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("VALIDACION"));
	}

	@Test
	@Tag("PB-13")
	@DisplayName("PB-13 · CP-13-06 · si la unidad no corresponde, se corrige la ficha y el traslado vuelve a buscar una mayor")
	void unidadNoCorresponde() throws Exception {
		Paramedico chica = escenario.unidadParaTraslados("IA", 0.002);
		Asignado asignado = asignar(escenario.ciudadano(), Escenario.pedidoDeTraslado());
		escenario.marcar(chica, asignado.atencion(), "llegada");

		pedir(post("/atenciones/" + asignado.atencion() + "/unidad-no-corresponde"), chica.token(),
				Json.objeto("latitud", Escenario.LATITUD, "longitud", Escenario.LONGITUD, "movilidad", "CAMILLA",
						"oxigeno", true, "equipo", false))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("SIN_TRASLADO"))
				.andExpect(jsonPath("$.motivoSinTraslado").value("UNIDAD_NO_CORRESPONDE"));

		pedir(get("/traslados/" + asignado.traslado()), escenario.admin())
				.andExpect(jsonPath("$.traslado.estado").value("BUSCANDO_UNIDAD"))
				.andExpect(jsonPath("$.traslado.tipoUnidad").value("II"))
				.andExpect(jsonPath("$.traslado.movilidad").value("CAMILLA"));
	}

	@Test
	@Tag("PB-13")
	@DisplayName("PB-13 · CP-13-06 · si con lo que marcó la misma unidad alcanza, no es que no corresponda")
	void unidadAlcanza() throws Exception {
		Paramedico unidad = escenario.unidadParaTraslados("II", 0.002);
		Asignado asignado = asignar(escenario.ciudadano(), Escenario.pedidoDeTraslado());
		escenario.marcar(unidad, asignado.atencion(), "llegada");

		pedir(post("/atenciones/" + asignado.atencion() + "/unidad-no-corresponde"), unidad.token(),
				Json.objeto("latitud", Escenario.LATITUD, "longitud", Escenario.LONGITUD, "movilidad", "CAMILLA",
						"oxigeno", false, "equipo", false))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("UNIDAD_ALCANZA"));
		sinTraslado(unidad, asignado, "UNIDAD_NO_CORRESPONDE")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("VALIDACION"));
		assertThat(estado(asignado)).isEqualTo("ASIGNADO");
	}

	@Test
	@Tag("PB-13")
	@DisplayName("PB-13 · CP-13-07 · en un traslado no hay 'atendido en el lugar': nadie espera que se lo atienda ahí")
	void motivoDeEmergenciaSinTraslado() throws Exception {
		Paramedico unidad = escenario.unidadParaTraslados("II", 0.002);
		Asignado asignado = asignar(escenario.ciudadano(), Escenario.pedidoDeTraslado());
		escenario.marcar(unidad, asignado.atencion(), "llegada");

		sinTraslado(unidad, asignado, "ATENDIDO_EN_EL_LUGAR")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("VALIDACION"));
	}

	@Test
	@Tag("PB-12")
	@DisplayName("PB-12 · CP-12-06 · la tripulación devuelve un traslado antes de llegar, y vuelve a buscarse otra unidad")
	void devolverAntesDeLlegar() throws Exception {
		Paramedico unidad = escenario.unidadParaTraslados("II", 0.002);
		escenario.recibirAvisos(unidad, "push-tripulacion");
		Asignado asignado = asignar(escenario.ciudadano(), Escenario.pedidoDeTraslado());

		cancelar(unidad, asignado, "RECHAZADA_POR_PARAMEDICO")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("CANCELADA"));

		assertThat(estado(asignado)).isEqualTo("BUSCANDO_UNIDAD");
		estadoDeLaAmbulancia(unidad.ambulanciaId(), "DISPONIBLE");
		planificador.barrer();
		assertThat(estado(asignado)).isEqualTo("BUSCANDO_UNIDAD");
	}

	@Test
	@Tag("PB-12")
	@DisplayName("PB-12 · CP-12-06 · los motivos de cancelar cambian en un traslado: devolver solo en camino, desviarse no con el paciente a bordo")
	void motivosDeCancelacionEnTraslado() throws Exception {
		Paramedico unidad = escenario.unidadParaTraslados("II", 0.002);
		Asignado asignado = asignar(escenario.ciudadano(), Escenario.pedidoDeTraslado());
		cancelar(unidad, asignado, "NO_SE_ENCONTRO_PACIENTE")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("VALIDACION"));
		cancelar(unidad, asignado, "CANCELADA_POR_SOLICITANTE")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("VALIDACION"));

		escenario.marcar(unidad, asignado.atencion(), "llegada");
		cancelar(unidad, asignado, "RECHAZADA_POR_PARAMEDICO")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("VALIDACION"));

		escenario.marcar(unidad, asignado.atencion(), "recogida");
		cancelar(unidad, asignado, "DESVIADA")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("VALIDACION"));
		assertThat(estado(asignado)).isEqualTo("ASIGNADO");
	}

	@Test
	@Tag("PB-12")
	@DisplayName("PB-12 · CP-12-06 · con una avería la unidad queda fuera de servicio y el traslado vuelve a buscar unidad")
	void averia() throws Exception {
		Paramedico unidad = escenario.unidadParaTraslados("II", 0.002);
		Asignado asignado = asignar(escenario.ciudadano(), Escenario.pedidoDeTraslado());

		cancelar(unidad, asignado, "AVERIA").andExpect(status().isOk());

		estadoDeLaAmbulancia(unidad.ambulanciaId(), "FUERA_DE_SERVICIO");
		assertThat(estado(asignado)).isEqualTo("BUSCANDO_UNIDAD");
	}

	@Test
	@DisplayName("PB-28 · CP-28-05 · si quien lo pidió lo cancela con la unidad en camino, la unidad queda libre y se le avisa")
	void canceladoEnCamino() throws Exception {
		Paramedico unidad = escenario.unidadParaTraslados("II", 0.002);
		escenario.recibirAvisos(unidad, "push-tripulacion");
		Ciudadano ciudadano = escenario.ciudadano();
		Asignado asignado = asignar(ciudadano, Escenario.pedidoDeTraslado());

		pedir(post("/traslados/" + asignado.traslado() + "/cancelar"), ciudadano.token())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("CANCELADO"));

		estadoDeLaAmbulancia(unidad.ambulanciaId(), "DISPONIBLE");
		assertThat(jdbc.queryForObject("select motivo_cancelacion from atencion where id = ?", String.class,
				asignado.atencion())).isEqualTo("CANCELADA_POR_SOLICITANTE");
		assertThat(publicaciones.avisosDeTrasladoA("push-tripulacion", asignado.traslado()))
				.containsExactly("ASIGNADO", "RETIRADO_CANCELADA_POR_SOLICITANTE");
	}

	@Test
	@DisplayName("PB-28 · CP-28-05 · con la unidad ya en la puerta el pedido no se cancela desde la app: se habla con la tripulación")
	void noSeCancelaEnLaPuerta() throws Exception {
		Paramedico unidad = escenario.unidadParaTraslados("II", 0.002);
		Ciudadano ciudadano = escenario.ciudadano();
		Asignado asignado = asignar(ciudadano, Escenario.pedidoDeTraslado());
		escenario.marcar(unidad, asignado.atencion(), "llegada");

		pedir(post("/traslados/" + asignado.traslado() + "/cancelar"), ciudadano.token())
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("UNIDAD_EN_EL_LUGAR"));
		assertThat(estado(asignado)).isEqualTo("ASIGNADO");
	}

	@Test
	@DisplayName("PB-28 · CP-28-06 · solo la tripulación de esa unidad marca los hitos del traslado")
	void soloSuTripulacion() throws Exception {
		escenario.unidadParaTraslados("II", 0.002);
		Asignado asignado = asignar(escenario.ciudadano(), Escenario.pedidoDeTraslado());

		pedir(post("/atenciones/" + asignado.atencion() + "/llegada"), escenario.paramedicoEnTurno().token(),
				Json.objeto("latitud", Escenario.LATITUD, "longitud", Escenario.LONGITUD))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("ATENCION_AJENA"));
	}

	/** El ciudadano lo pide para ahora y el barrido se lo asigna a la unidad lista que haya. */
	private Asignado asignar(Ciudadano ciudadano, Map<String, Object> pedido) {
		long traslado = escenario.pedirTraslado(ciudadano, pedido);
		planificador.barrer();
		long atencion = jdbc.queryForObject(
				"select id from atencion where traslado_id = ? and estado = 'EN_CAMINO'", Long.class, traslado);
		return new Asignado(traslado, atencion);
	}

	private ResultActions hito(Paramedico unidad, Asignado asignado, String hito) throws Exception {
		return pedir(post("/atenciones/" + asignado.atencion() + "/" + hito), unidad.token(),
				Json.objeto("latitud", Escenario.LATITUD, "longitud", Escenario.LONGITUD));
	}

	private ResultActions sinTraslado(Paramedico unidad, Asignado asignado, String motivo) throws Exception {
		return pedir(post("/atenciones/" + asignado.atencion() + "/sin-traslado"), unidad.token(),
				Json.objeto("latitud", Escenario.LATITUD, "longitud", Escenario.LONGITUD, "motivo", motivo));
	}

	private ResultActions cancelar(Paramedico unidad, Asignado asignado, String motivo) throws Exception {
		return pedir(post("/atenciones/" + asignado.atencion() + "/cancelar"), unidad.token(),
				Json.objeto("motivo", motivo));
	}

	private String estado(Asignado asignado) {
		return jdbc.queryForObject("select estado from traslado where id = ?", String.class, asignado.traslado());
	}

	private void estadoDeLaAmbulancia(long ambulancia, String estado) throws Exception {
		pedir(get("/ambulancias"), escenario.admin())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.id == " + ambulancia + ")].estado", hasItem(estado)));
	}

	private record Asignado(long traslado, long atencion) {
	}

}
