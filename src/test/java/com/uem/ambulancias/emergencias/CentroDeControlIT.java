package com.uem.ambulancias.emergencias;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import com.uem.ambulancias.emergencias.domain.EstadoIncidente;
import com.uem.ambulancias.emergencias.service.UnidadPublicada;
import com.uem.ambulancias.flota.domain.EstadoAmbulancia;
import com.uem.ambulancias.soporte.Escenario;
import com.uem.ambulancias.soporte.Escenario.Paramedico;
import com.uem.ambulancias.soporte.Json;
import com.uem.ambulancias.soporte.PruebaIT;

/**
 * El centro de control: {@code /operacion} trae toda la operación en una sola respuesta, y lo que cambia se publica
 * en vivo (en las pruebas, anotado en vez de ir a Firebase).
 */
@Tag("PB-15")
class CentroDeControlIT extends PruebaIT {

	@Test
	@DisplayName("PB-15 · CP-15-01 · cada unidad con su estado, su tripulación de turno, su atención y su última posición")
	void unidades() throws Exception {
		Paramedico paramedico = escenario.paramedicoEnTurno();
		escenario.reportarPosicion(paramedico, Escenario.LATITUD + 0.01, Escenario.LONGITUD);
		pedir(post("/alertas"), escenario.ciudadano().token(), Json.objeto("latitud", Escenario.LATITUD, "longitud",
				Escenario.LONGITUD, "origenUbicacion", "GPS", "descripcion", "Señor caído en la vereda"))
				.andExpect(status().isCreated());
		long incidente = Json.numero(cuerpo(pedir(get("/incidentes"), escenario.admin())), "$.contenido[0].id");
		long atencion = escenario.tomar(paramedico, incidente);
		String unidad = "$.unidades[?(@.ambulanciaId == " + paramedico.ambulanciaId() + ")]";

		operacion()
				.andExpect(jsonPath(unidad + ".estado", contains("EN_ATENCION")))
				.andExpect(jsonPath(unidad + ".tipoUnidad", contains("II")))
				.andExpect(jsonPath(unidad + ".tripulacion[0].id", contains((int) paramedico.id())))
				.andExpect(jsonPath(unidad + ".tripulacion[0].telefono", contains(paramedico.telefono())))
				.andExpect(jsonPath(unidad + ".turnoDesde").isNotEmpty())
				.andExpect(jsonPath(unidad + ".atencion.id", contains((int) atencion)))
				.andExpect(jsonPath(unidad + ".atencion.estado", contains("EN_CAMINO")))
				.andExpect(jsonPath(unidad + ".atencion.incidenteId", contains((int) incidente)))
				.andExpect(jsonPath(unidad + ".atencion.etiqueta", contains("Señor caído en la vereda")))
				.andExpect(jsonPath(unidad + ".ultimaPosicion.latitud", contains(Escenario.LATITUD + 0.01)))
				.andExpect(jsonPath(unidad + ".ultimaPosicion.en").isNotEmpty());
	}

	@Test
	@DisplayName("PB-15 · CP-15-01 · se ve toda la flota en servicio, también la que no tiene gente o está averiada; la dada de baja, no")
	void todaLaFlota() throws Exception {
		long sinTurno = escenario.ambulancia();
		long averiada = escenario.ambulancia();
		long deBaja = escenario.ambulancia();
		pedir(post("/ambulancias/" + averiada + "/fuera-de-servicio"), escenario.admin()).andExpect(status().isOk());
		pedir(post("/ambulancias/" + deBaja + "/desactivar"), escenario.admin()).andExpect(status().isOk());

		operacion()
				.andExpect(jsonPath("$.unidades.length()").value(2))
				.andExpect(jsonPath("$.unidades[?(@.ambulanciaId == " + sinTurno + ")].estado", contains("SIN_TURNO")))
				.andExpect(jsonPath("$.unidades[?(@.ambulanciaId == " + sinTurno + ")].tripulacion[*]").isEmpty())
				.andExpect(jsonPath("$.unidades[?(@.ambulanciaId == " + averiada + ")].estado",
						contains("FUERA_DE_SERVICIO")))
				.andExpect(jsonPath("$.unidades[*].ambulanciaId", not(hasItem((int) deBaja))));
	}

	@Test
	@DisplayName("PB-15 · CP-15-01 · los incidentes sin unidad aparecen aparte, el más viejo primero; los cubiertos, no")
	void incidentesSinCubrir() throws Exception {
		long viejo = escenario.alertar(escenario.ciudadano(), Escenario.LATITUD + 0.05, Escenario.LONGITUD)
				.incidenteId();
		long nuevo = escenario.alertar(escenario.ciudadano(), Escenario.LATITUD + 0.10, Escenario.LONGITUD)
				.incidenteId();
		long cubierto = escenario.alertar(escenario.ciudadano()).incidenteId();
		escenario.tomar(escenario.paramedicoEnTurno(), cubierto);

		operacion()
				.andExpect(jsonPath("$.incidentesSinCubrir.length()").value(2))
				.andExpect(jsonPath("$.incidentesSinCubrir[0].id").value(viejo))
				.andExpect(jsonPath("$.incidentesSinCubrir[1].id").value(nuevo))
				.andExpect(jsonPath("$.incidentesSinCubrir[0].estado").value("ACTIVO"))
				.andExpect(jsonPath("$.incidentesSinCubrir[0].desde").isNotEmpty());
	}

	@Test
	@DisplayName("PB-15 · CP-15-01 · la bitácora cuenta qué hizo cada unidad, del hito más nuevo al más viejo")
	void bitacora() throws Exception {
		Paramedico paramedico = escenario.paramedicoEnTurno();
		long incidente = escenario.alertar(escenario.ciudadano()).incidenteId();
		long atencion = escenario.tomar(paramedico, incidente);
		escenario.marcar(paramedico, atencion, "llegada");
		pedir(post("/atenciones/" + atencion + "/cancelar"), paramedico.token(),
				Json.objeto("motivo", "NO_SE_ENCONTRO_PACIENTE"))
				.andExpect(status().isOk());

		operacion()
				.andExpect(jsonPath("$.eventos.length()").value(3))
				.andExpect(jsonPath("$.eventos[0].tipo").value("CANCELACION"))
				.andExpect(jsonPath("$.eventos[0].detalle").value("NO_SE_ENCONTRO_PACIENTE"))
				.andExpect(jsonPath("$.eventos[1].tipo").value("LLEGADA"))
				.andExpect(jsonPath("$.eventos[2].tipo").value("TOMA"))
				.andExpect(jsonPath("$.eventos[2].ambulanciaId").value(paramedico.ambulanciaId()))
				.andExpect(jsonPath("$.eventos[2].incidenteId").value(incidente));
	}

	@Test
	@DisplayName("PB-15 · CP-15-02 · cada posición viaja con su hora y con el umbral de señal perdida, para no mostrar una vieja como actual")
	void posicionAntigua() throws Exception {
		Paramedico paramedico = escenario.paramedicoEnTurno();
		escenario.reportarPosicion(paramedico, Escenario.LATITUD, Escenario.LONGITUD);
		jdbc.update("update ambulancia set ultima_posicion_en = now() - interval '5 minutes' where id = ?",
				paramedico.ambulanciaId());

		String respuesta = cuerpo(operacion().andExpect(jsonPath("$.umbralSinSenalSeg").value(60)));

		Instant en = Instant.parse(Json.<List<String>>leer(respuesta,
				"$.unidades[?(@.ambulanciaId == " + paramedico.ambulanciaId() + ")].ultimaPosicion.en").getFirst());
		assertThat(Duration.between(en, Instant.now())).isGreaterThan(Duration.ofSeconds(60));
	}

	@Test
	@DisplayName("PB-15 · CP-15-03 · lo que cambia se publica en vivo: el estado de la unidad, su atención y su posición")
	void cambiosEnVivo() throws Exception {
		Paramedico paramedico = escenario.paramedicoEnTurno();
		long incidente = escenario.alertar(escenario.ciudadano()).incidenteId();

		long atencion = escenario.tomar(paramedico, incidente);
		UnidadPublicada tomada = publicaciones.ultimaUnidadPublicada(paramedico.ambulanciaId());
		assertThat(tomada.estado()).isEqualTo(EstadoAmbulancia.EN_ATENCION);
		assertThat(tomada.atencionId()).isEqualTo(atencion);

		escenario.reportarPosicion(paramedico, Escenario.LATITUD + 0.002, Escenario.LONGITUD);
		assertThat(publicaciones.posicionesPublicadas(paramedico.ambulanciaId())).singleElement()
				.satisfies(posicion -> assertThat(posicion.latitud()).isEqualTo(Escenario.LATITUD + 0.002));

		pedir(post("/atenciones/" + atencion + "/cancelar"), paramedico.token(), Json.objeto("motivo", "AVERIA"))
				.andExpect(status().isOk());
		UnidadPublicada averiada = publicaciones.ultimaUnidadPublicada(paramedico.ambulanciaId());
		assertThat(averiada.estado()).isEqualTo(EstadoAmbulancia.FUERA_DE_SERVICIO);
		assertThat(averiada.atencionId()).isNull();
	}

	@Test
	@DisplayName("PB-15 · CP-15-03 · cuando sale de turno el último tripulante, la unidad se saca del mapa")
	void sinTripulacionSaleDelMapa() throws Exception {
		Paramedico paramedico = escenario.paramedicoEnTurno();
		escenario.reportarPosicion(paramedico, Escenario.LATITUD, Escenario.LONGITUD);
		assertThat(publicaciones.posicionRetirada(paramedico.ambulanciaId())).isFalse();

		pedir(post("/paramedicos/actual/turno/cierre"), paramedico.token()).andExpect(status().isOk());

		assertThat(publicaciones.posicionRetirada(paramedico.ambulanciaId())).isTrue();
	}

	@Test
	@DisplayName("PB-15 · CP-15-04 · solo el administrador ve la operación completa")
	void soloAdministrador() throws Exception {
		pedir(get("/operacion"), escenario.paramedicoActivado().token()).andExpect(status().isForbidden());
		pedir(get("/operacion"), escenario.ciudadano().token()).andExpect(status().isForbidden());
		pedir(get("/operacion"), null).andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("PB-15 · CP-15-05 · si la tripulación no responde, la central cancela su atención y el incidente vuelve a buscarse")
	void centralCancelaAtencion() throws Exception {
		Paramedico paramedico = escenario.paramedicoEnTurno();
		long incidente = escenario.alertar(escenario.ciudadano()).incidenteId();
		long atencion = escenario.tomar(paramedico, incidente);

		cerrarAtencion(atencion, Json.objeto("cierre", "CANCELAR", "dejarDisponible", false))
				.andExpect(status().isNoContent());

		pedir(get("/incidentes/" + incidente), escenario.admin())
				.andExpect(jsonPath("$.estado").value("ACTIVO"))
				.andExpect(jsonPath("$.atenciones[0].estado").value("CANCELADA"))
				.andExpect(jsonPath("$.atenciones[0].motivoCancelacion").value("CERRADA_POR_CENTRAL"))
				.andExpect(jsonPath("$.atenciones[0].cerradaPor").value(startsWith("Administrador")));
		estadoDeLaAmbulancia(paramedico.ambulanciaId(), "FUERA_DE_SERVICIO");
		assertThat(publicaciones.ultimoSeguimiento(incidente).estado()).isEqualTo(EstadoIncidente.ACTIVO);
	}

	@Test
	@DisplayName("PB-15 · CP-15-05 · con el paciente a bordo la central no cancela: la da por entregada y deja la unidad libre")
	void centralDaPorEntregada() throws Exception {
		long centro = escenario.centroDeSalud("Hospital Japonés", true);
		Paramedico paramedico = escenario.paramedicoEnTurno();
		long incidente = escenario.alertar(escenario.ciudadano()).incidenteId();
		long atencion = escenario.tomar(paramedico, incidente);
		escenario.marcar(paramedico, atencion, "llegada");
		escenario.marcar(paramedico, atencion, "recogida");

		cerrarAtencion(atencion, Json.objeto("cierre", "CANCELAR", "dejarDisponible", true))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("TRANSICION_INVALIDA"));
		cerrarAtencion(atencion, Json.objeto("cierre", "DAR_POR_ENTREGADA", "dejarDisponible", true, "centroSaludId",
				centro))
				.andExpect(status().isNoContent());

		pedir(get("/incidentes/" + incidente), escenario.admin())
				.andExpect(jsonPath("$.estado").value("ATENDIDO"))
				.andExpect(jsonPath("$.atenciones[0].estado").value("PACIENTE_ENTREGADO"))
				.andExpect(jsonPath("$.atenciones[0].centroSalud.id").value(centro))
				.andExpect(jsonPath("$.atenciones[0].horaLiberacion").isNotEmpty());
		estadoDeLaAmbulancia(paramedico.ambulanciaId(), "DISPONIBLE");
	}

	@Test
	@DisplayName("PB-15 · CP-15-05 · la central libera una unidad que resolvió y no marcó que quedó libre; la tripulación no usa esta vía")
	void centralLibera() throws Exception {
		Paramedico paramedico = escenario.paramedicoEnTurno();
		long atencion = escenario.tomar(paramedico, escenario.alertar(escenario.ciudadano()).incidenteId());
		escenario.marcar(paramedico, atencion, "llegada");
		pedir(post("/atenciones/" + atencion + "/sin-traslado"), paramedico.token(), Json.objeto("latitud",
				Escenario.LATITUD, "longitud", Escenario.LONGITUD, "motivo", "ATENDIDO_EN_EL_LUGAR"))
				.andExpect(status().isOk());

		pedir(post("/operacion/atenciones/" + atencion + "/cierre"), paramedico.token(),
				Json.objeto("cierre", "LIBERAR", "dejarDisponible", true))
				.andExpect(status().isForbidden());
		cerrarAtencion(atencion, Json.objeto("cierre", "LIBERAR", "dejarDisponible", true))
				.andExpect(status().isNoContent());

		estadoDeLaAmbulancia(paramedico.ambulanciaId(), "DISPONIBLE");
		pedir(get("/paramedicos/actual/atencion"), paramedico.token()).andExpect(status().isNoContent());
	}

	private ResultActions operacion() throws Exception {
		return pedir(get("/operacion"), escenario.admin()).andExpect(status().isOk());
	}

	private ResultActions cerrarAtencion(long atencion, String cuerpo) throws Exception {
		return pedir(post("/operacion/atenciones/" + atencion + "/cierre"), escenario.admin(), cuerpo);
	}

	private void estadoDeLaAmbulancia(long ambulancia, String estado) throws Exception {
		pedir(get("/ambulancias"), escenario.admin())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.id == " + ambulancia + ")].estado", hasItem(estado)));
	}

}
