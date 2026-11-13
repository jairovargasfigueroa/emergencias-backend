package com.uem.ambulancias.emergencias;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import com.uem.ambulancias.emergencias.service.PlanificadorDeTraslados;
import com.uem.ambulancias.soporte.Escenario;
import com.uem.ambulancias.soporte.Escenario.Ciudadano;
import com.uem.ambulancias.soporte.Json;
import com.uem.ambulancias.soporte.PruebaIT;

@Tag("PB-26")
class TrasladoDesdeLaCentralIT extends PruebaIT {

	private static final ZoneId LA_PAZ = ZoneId.of("America/La_Paz");

	@Autowired
	private PlanificadorDeTraslados planificador;

	/** El cuerpo que arma el panel: quién viaja y, aparte, el viaje igual que en la app. */
	private static String pedidoPorTelefono(String nombre, String telefono, boolean confirmar,
			Map<String, Object> viaje) {
		return "{\"pasajeroNombre\":" + (nombre == null ? "null" : "\"" + nombre + "\"")
				+ ",\"pasajeroTelefono\":" + (telefono == null ? "null" : "\"" + telefono + "\"")
				+ ",\"confirmarDuplicado\":" + confirmar
				+ ",\"pedido\":" + (viaje == null ? "null" : Json.objeto(viaje)) + "}";
	}

	private ResultActions cargar(String token, String nombre, String telefono, boolean confirmar) throws Exception {
		return pedir(post("/traslados/central"), token,
				pedidoPorTelefono(nombre, telefono, confirmar, Escenario.pedidoDeTraslado()));
	}

	@Test
	@DisplayName("PB-26 · CP-26-01 · la central carga un pedido telefónico y recibe su número y su estado")
	void cargaUnPedido() throws Exception {
		String admin = escenario.admin();

		String respuesta = cuerpo(cargar(admin, "  Rosa Méndez  ", "71234567", false)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.traslado.id").isNumber())
				.andExpect(jsonPath("$.traslado.estado").value("BUSCANDO_UNIDAD"))
				.andExpect(jsonPath("$.traslado.pasajero").value("Rosa Méndez"))
				.andExpect(jsonPath("$.traslado.tipoUnidad").value("IA"))
				.andExpect(jsonPath("$.registradoPorLaCentral").isNotEmpty())
				.andExpect(jsonPath("$.telefonoSolicitante").value("71234567")));
		long traslado = Json.numero(respuesta, "$.traslado.id");

		pedir(get("/traslados/" + traslado), admin)
				.andExpect(jsonPath("$.registradoPorLaCentral").isNotEmpty())
				.andExpect(jsonPath("$.telefonoSolicitante").value("71234567"));
	}

	@Test
	@DisplayName("PB-26 · CP-26-02 · entra a la misma agenda del día que los pedidos de la app")
	void mismaAgenda() throws Exception {
		long porTelefono = Json.numero(cuerpo(cargar(escenario.admin(), "Rosa Méndez", "71234567", false)),
				"$.traslado.id");
		long porLaApp = escenario.pedirTraslado(escenario.ciudadano(), Escenario.pedidoDeTraslado());

		pedir(get("/traslados").param("dia", LocalDate.now(LA_PAZ).toString()), escenario.admin())
				.andExpect(jsonPath("$[*].traslado.id", hasItem((int) porTelefono)))
				.andExpect(jsonPath("$[*].traslado.id", hasItem((int) porLaApp)));
	}

	@Test
	@DisplayName("PB-26 · CP-26-02 · la búsqueda de unidad lo asigna igual que a uno de la app")
	void mismaBusqueda() throws Exception {
		escenario.unidadParaTraslados("II", 0.002);
		long traslado = Json.numero(cuerpo(cargar(escenario.admin(), "Rosa Méndez", "71234567", false)),
				"$.traslado.id");

		planificador.barrer();

		pedir(get("/traslados/" + traslado), escenario.admin())
				.andExpect(jsonPath("$.traslado.estado").value("ASIGNADO"))
				.andExpect(jsonPath("$.placa").isNotEmpty());
	}

	@Test
	@DisplayName("PB-26 · CP-26-03 · el mismo teléfono con un traslado vigente ese día pide confirmación")
	void avisaDuplicado() throws Exception {
		String admin = escenario.admin();
		long primero = Json.numero(cuerpo(cargar(admin, "Rosa Méndez", "71234567", false)), "$.traslado.id");

		cargar(admin, "Rosa Mendez", "+591 712-34567", false)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("TRASLADO_POSIBLE_DUPLICADO"))
				.andExpect(jsonPath("$.detail", containsString("N.º " + primero)));

		cargar(admin, "Rosa Mendez", "+591 712-34567", true)
				.andExpect(status().isCreated());
	}

	@Test
	@DisplayName("PB-26 · CP-26-03 · también avisa si quien llama ya lo pidió por la app")
	void avisaSiYaLoPidioPorLaApp() throws Exception {
		Ciudadano ciudadano = escenario.ciudadano();
		escenario.pedirTraslado(ciudadano, Escenario.pedidoDeTraslado());

		cargar(escenario.admin(), "Pedro Rojas", ciudadano.telefono(), false)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("TRASLADO_POSIBLE_DUPLICADO"));
	}

	@Test
	@DisplayName("PB-26 · CP-26-03 · un traslado ya cancelado no cuenta como duplicado")
	void canceladoNoCuenta() throws Exception {
		Ciudadano ciudadano = escenario.ciudadano();
		long anterior = escenario.pedirTraslado(ciudadano, Escenario.pedidoDeTraslado());
		pedir(post("/traslados/" + anterior + "/cancelar"), ciudadano.token()).andExpect(status().isOk());

		cargar(escenario.admin(), "Pedro Rojas", ciudadano.telefono(), false).andExpect(status().isCreated());
	}

	@Test
	@DisplayName("PB-26 · CP-26-03 · quien viaja otra vez se reusa: no se duplica la persona")
	void reusaLaPersona() throws Exception {
		String admin = escenario.admin();
		long primero = Json.numero(cuerpo(cargar(admin, "Rosa Méndez", "71234567", false)), "$.traslado.id");
		long segundo = Json.numero(cuerpo(cargar(admin, "rosa méndez", "71234567", true)), "$.traslado.id");

		long pasajeroDelPrimero = Json.numero(cuerpo(pedir(get("/traslados/" + primero), admin)),
				"$.traslado.pasajeroId");
		pedir(get("/traslados/" + segundo), admin)
				.andExpect(jsonPath("$.traslado.pasajeroId").value(pasajeroDelPrimero));
	}

	@Test
	@DisplayName("PB-26 · CP-26-04 · sin nombre, teléfono o datos del viaje no se registra")
	void validaLosDatos() throws Exception {
		String admin = escenario.admin();

		cargar(admin, " ", "71234567", false)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value("VALIDACION"));
		cargar(admin, "Rosa Méndez", "abc", false).andExpect(status().isBadRequest());
		pedir(post("/traslados/central"), admin, pedidoPorTelefono("Rosa Méndez", "71234567", false, null))
				.andExpect(status().isBadRequest());
		Map<String, Object> sinOrigen = Escenario.pedidoDeTraslado();
		sinOrigen.remove("origenLatitud");
		pedir(post("/traslados/central"), admin, pedidoPorTelefono("Rosa Méndez", "71234567", false, sinOrigen))
				.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("PB-26 · CP-26-04 · solo la central carga pedidos telefónicos")
	void soloLaCentral() throws Exception {
		cargar(escenario.ciudadano().token(), "Rosa Méndez", "71234567", false).andExpect(status().isForbidden());
		cargar(escenario.paramedicoActivado().token(), "Rosa Méndez", "71234567", false)
				.andExpect(status().isForbidden());
		cargar(null, "Rosa Méndez", "71234567", false).andExpect(status().isUnauthorized());
	}
}
