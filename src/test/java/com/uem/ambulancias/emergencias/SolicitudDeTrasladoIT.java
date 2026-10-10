package com.uem.ambulancias.emergencias;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import com.uem.ambulancias.soporte.Escenario;
import com.uem.ambulancias.soporte.Escenario.Ciudadano;
import com.uem.ambulancias.soporte.Escenario.Paramedico;
import com.uem.ambulancias.soporte.Json;
import com.uem.ambulancias.soporte.PruebaIT;

@Tag("PB-25")
class SolicitudDeTrasladoIT extends PruebaIT {

	/** Mañana a esta hora, al segundo: es lo que manda la app cuando se elige la hora de la cita. */
	private static Instant manana() {
		return Instant.now().plus(Duration.ofDays(1)).truncatedTo(ChronoUnit.SECONDS);
	}

	@Test
	@DisplayName("PB-25 · CP-25-01 · sin elegir pasajero el traslado es para quien lo pide; sin cita, es para ahora")
	void paraMiAhora() throws Exception {
		Ciudadano ciudadano = escenario.ciudadano();
		Instant antes = Instant.now();

		String respuesta = cuerpo(pedir(ciudadano, Escenario.pedidoDeTraslado())
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.estado").value("BUSCANDO_UNIDAD"))
				.andExpect(jsonPath("$.modoHorario").value("INMEDIATO"))
				.andExpect(jsonPath("$.horaCita").doesNotExist())
				.andExpect(jsonPath("$.pasajeroId").value(ciudadano.id()))
				.andExpect(jsonPath("$.tipoUnidad").value("IA")));

		Instant salida = Instant.parse(Json.texto(respuesta, "$.horaSalidaEstimada"));
		Instant limite = Instant.parse(Json.texto(respuesta, "$.horaLimiteSalida"));
		assertThat(salida).isBetween(antes, Instant.now());
		assertThat(Duration.between(salida, limite)).isEqualTo(Duration.ofMinutes(60));
	}

	@Test
	@DisplayName("PB-25 · CP-25-01 · se puede pedir para una persona de la agenda, no para una ajena ni para una borrada")
	void paraAlguienDeLaAgenda() throws Exception {
		Ciudadano ciudadano = escenario.ciudadano();
		long mama = escenario.persona(ciudadano, "Rosa Méndez");
		long borrada = escenario.persona(ciudadano, "Juan Méndez");
		pedir(delete("/personas/" + borrada), ciudadano.token()).andExpect(status().isNoContent());
		long ajena = escenario.persona(escenario.ciudadano(), "Persona de otro");

		pedir(ciudadano, con("pasajeroId", mama))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.pasajeroId").value(mama))
				.andExpect(jsonPath("$.pasajero").value("Rosa Méndez"));
		pedir(ciudadano, con("pasajeroId", ajena))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("PASAJERO_AJENO"));
		pedir(ciudadano, con("pasajeroId", borrada))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("PASAJERO_AJENO"));
		pedir(get("/personas"), ciudadano.token())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].nombreCompleto").value("Rosa Méndez"));
	}

	@Test
	@DisplayName("PB-25 · CP-25-02 · con cita queda programado, y la salida se calcula para llegar a tiempo")
	void conCita() throws Exception {
		Instant cita = manana();

		String respuesta = cuerpo(pedir(escenario.ciudadano(), con("horaCita", cita))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.estado").value("PROGRAMADO"))
				.andExpect(jsonPath("$.modoHorario").value("PROGRAMADO")));

		Instant salida = Instant.parse(Json.texto(respuesta, "$.horaSalidaEstimada"));
		Instant limite = Instant.parse(Json.texto(respuesta, "$.horaLimiteSalida"));
		Instant recogidaHasta = Instant.parse(Json.texto(respuesta, "$.horaRecogidaHasta"));
		assertThat(Instant.parse(Json.texto(respuesta, "$.horaCita"))).isEqualTo(cita);
		assertThat(Duration.between(salida, limite)).isEqualTo(Duration.ofMinutes(20));
		assertThat(limite).isBefore(cita);
		assertThat(recogidaHasta).isBefore(cita);
	}

	@Test
	@DisplayName("PB-25 · CP-25-02 · una cita a la que ya no se llega se rechaza")
	void horaInalcanzable() throws Exception {
		pedir(escenario.ciudadano(), con("horaCita", Instant.now().plus(Duration.ofMinutes(20))))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("HORA_INALCANZABLE"));
	}

	@Test
	@DisplayName("PB-25 · CP-25-02 · sin cómo se moviliza, sin origen o con coordenadas imposibles no se registra")
	void datosRequeridos() throws Exception {
		Ciudadano ciudadano = escenario.ciudadano();

		pedir(ciudadano, con("movilidad", null))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value("VALIDACION"));
		pedir(ciudadano, con("origenLatitud", null)).andExpect(status().isBadRequest());
		pedir(ciudadano, con("origenLatitud", 95)).andExpect(status().isBadRequest());
		pedir(ciudadano, con("destinoLongitud", -190)).andExpect(status().isBadRequest());
		pedir(ciudadano, con("pesoAproximado", 0)).andExpect(status().isBadRequest());
		pedir(ciudadano, con("acompanantes", -1)).andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("PB-25 · CP-25-02 · hace falta un destino: un centro del catálogo o un punto en el mapa")
	void destinoRequerido() throws Exception {
		Map<String, Object> pedido = Escenario.pedidoDeTraslado();
		pedido.remove("destinoLatitud");
		pedido.remove("destinoLongitud");

		pedir(escenario.ciudadano(), pedido)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("DESTINO_REQUERIDO"));
	}

	@Test
	@Tag("PB-09")
	@DisplayName("PB-09 · CP-09-05 · el destino del traslado puede ser un centro del catálogo, y queda en su ubicación")
	void destinoEnElCatalogo() throws Exception {
		long centro = escenario.centroDeSalud("Hospital Japonés", true);
		long cerrado = escenario.centroDeSalud("Posta cerrada", false);
		Map<String, Object> pedido = Escenario.pedidoDeTraslado();
		pedido.remove("destinoLatitud");
		pedido.remove("destinoLongitud");
		pedido.put("centroSaludDestinoId", centro);
		pedido.put("destinoDetalle", "Consultorio 12, segundo piso");
		Ciudadano ciudadano = escenario.ciudadano();

		pedir(ciudadano, pedido)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.centroSaludDestinoId").value(centro))
				.andExpect(jsonPath("$.centroSaludDestino").value("Hospital Japonés"))
				.andExpect(jsonPath("$.destino.latitud").value(Escenario.LATITUD))
				.andExpect(jsonPath("$.destino.longitud").value(Escenario.LONGITUD + 0.01))
				.andExpect(jsonPath("$.destinoDetalle").value("Consultorio 12, segundo piso"));
		pedido.put("centroSaludDestinoId", cerrado);
		pedir(ciudadano, pedido).andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("PB-25 · CP-25-02 · el tipo de unidad sale de las necesidades; se puede pedir una mayor, nunca una menor")
	void tipoDeUnidad() throws Exception {
		Ciudadano ciudadano = escenario.ciudadano();
		Map<String, Object> enCamillaConOxigeno = con("movilidad", "CAMILLA");
		enCamillaConOxigeno.put("oxigeno", true);

		pedir(ciudadano, enCamillaConOxigeno).andExpect(jsonPath("$.tipoUnidad").value("II"));
		pedir(ciudadano, con("equipo", true)).andExpect(jsonPath("$.tipoUnidad").value("III"));
		pedir(ciudadano, con("tipoUnidad", "III"))
				.andExpect(jsonPath("$.tipoUnidad").value("III"))
				.andExpect(jsonPath("$.tipoUnidadPedido").value("III"));
		enCamillaConOxigeno.put("tipoUnidad", "IA");
		pedir(ciudadano, enCamillaConOxigeno)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("UNIDAD_INSUFICIENTE"));
	}

	@Test
	@DisplayName("PB-25 · CP-25-02 · el contacto en el origen va completo, con nombre y teléfono, o no va")
	void contactoCompleto() throws Exception {
		Ciudadano ciudadano = escenario.ciudadano();
		Map<String, Object> soloNombre = con("contactoNombre", "Portera del edificio");

		pedir(ciudadano, soloNombre)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("VALIDACION"));
		soloNombre.put("contactoTelefono", "70011122");
		pedir(ciudadano, soloNombre)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.contactoNombre").value("Portera del edificio"))
				.andExpect(jsonPath("$.contactoTelefono").value("70011122"));
	}

	@Test
	@DisplayName("PB-25 · CP-25-03 · pedirlo no reserva ninguna ambulancia: la unidad libre sigue libre")
	void noReservaUnidad() throws Exception {
		Paramedico libre = escenario.unidadParaTraslados("III", 0.001);
		Ciudadano ciudadano = escenario.ciudadano();

		escenario.pedirTraslado(ciudadano, Escenario.pedidoDeTraslado());
		escenario.pedirTraslado(ciudadano, con("horaCita", manana()));

		pedir(get("/ambulancias"), escenario.admin())
				.andExpect(jsonPath("$[?(@.id == " + libre.ambulanciaId() + ")].estado", hasItem("DISPONIBLE")));
		assertThat(jdbc.queryForObject("select count(*) from atencion", Long.class)).isZero();
	}

	@Test
	@DisplayName("PB-25 · CP-25-03 · el ciudadano ve sus pedidos con su estado, del más nuevo al más viejo; los de otros, no")
	void misTraslados() throws Exception {
		Ciudadano ciudadano = escenario.ciudadano();
		long primero = escenario.pedirTraslado(ciudadano, con("horaCita", manana()));
		long segundo = escenario.pedirTraslado(ciudadano, Escenario.pedidoDeTraslado());
		escenario.pedirTraslado(escenario.ciudadano(), Escenario.pedidoDeTraslado());

		pedir(get("/traslados/mios"), ciudadano.token())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].id").value(segundo))
				.andExpect(jsonPath("$[0].estado").value("BUSCANDO_UNIDAD"))
				.andExpect(jsonPath("$[1].id").value(primero))
				.andExpect(jsonPath("$[1].estado").value("PROGRAMADO"));
	}

	@Test
	@DisplayName("PB-25 · CP-25-03 · mientras nadie salió se puede reprogramar, y siempre corregir referencia y contacto")
	void reprogramarYCorregir() throws Exception {
		Ciudadano ciudadano = escenario.ciudadano();
		long traslado = escenario.pedirTraslado(ciudadano, con("horaCita", manana()));
		Instant otraCita = manana().plus(Duration.ofDays(1));

		pedir(put("/traslados/" + traslado), ciudadano.token(), Json.objeto(con("horaCita", otraCita)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.horaCita").value(otraCita.toString()))
				.andExpect(jsonPath("$.estado").value("PROGRAMADO"));
		pedir(put("/traslados/" + traslado + "/detalles"), ciudadano.token(), Json.objeto("origenReferencia",
				"Casa de rejas verdes", "contactoNombre", "Pedro", "contactoTelefono", "70033344", "observaciones",
				"Usa andador"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.origenReferencia").value("Casa de rejas verdes"))
				.andExpect(jsonPath("$.observaciones").value("Usa andador"));
		pedir(put("/traslados/" + traslado + "/detalles"), escenario.ciudadano().token(),
				Json.objeto("origenReferencia", "otra"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("TRASLADO_AJENO"));
	}

	@Test
	@DisplayName("PB-25 · CP-25-03 · el ciudadano cancela su pedido; cancelado ya no se cancela ni se reprograma")
	void cancelar() throws Exception {
		Ciudadano ciudadano = escenario.ciudadano();
		long traslado = escenario.pedirTraslado(ciudadano, con("horaCita", manana()));

		pedir(post("/traslados/" + traslado + "/cancelar"), escenario.ciudadano().token())
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("TRASLADO_AJENO"));
		pedir(post("/traslados/" + traslado + "/cancelar"), ciudadano.token())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("CANCELADO"));
		pedir(post("/traslados/" + traslado + "/cancelar"), ciudadano.token())
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("TRASLADO_FINALIZADO"));
		pedir(put("/traslados/" + traslado), ciudadano.token(), Json.objeto(con("horaCita", manana())))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("TRASLADO_FINALIZADO"));
	}

	@Test
	@DisplayName("PB-25 · CP-25-04 · un traslado no es una emergencia: no crea incidente ni aparece entre ellos")
	void separadoDeLasEmergencias() throws Exception {
		escenario.pedirTraslado(escenario.ciudadano(), Escenario.pedidoDeTraslado());

		pedir(get("/incidentes"), escenario.admin()).andExpect(jsonPath("$.totalElementos").value(0));
		pedir(get("/operacion"), escenario.admin())
				.andExpect(jsonPath("$.incidentesSinCubrir.length()").value(0))
				.andExpect(jsonPath("$.trasladosSinCubrir.length()").value(1));
		assertThat(publicaciones.cantidadDeAvisosDeIncidenteNuevo()).isZero();
	}

	@Test
	@DisplayName("PB-25 · CP-25-04 · los traslados los pide un ciudadano: ni el paramédico ni la central desde esta ruta")
	void soloCiudadanos() throws Exception {
		String cuerpo = Json.objeto(Escenario.pedidoDeTraslado());

		pedir(post("/traslados"), escenario.paramedicoActivado().token(), cuerpo).andExpect(status().isForbidden());
		pedir(post("/traslados"), escenario.admin(), cuerpo).andExpect(status().isForbidden());
		pedir(get("/traslados/mios"), escenario.admin()).andExpect(status().isForbidden());
	}

	private static Map<String, Object> con(String campo, Object valor) {
		Map<String, Object> pedido = Escenario.pedidoDeTraslado();
		pedido.put(campo, valor);
		return pedido;
	}

	private ResultActions pedir(Ciudadano ciudadano, Map<String, Object> pedido) throws Exception {
		return pedir(post("/traslados"), ciudadano.token(), Json.objeto(pedido));
	}

}
