package com.uem.ambulancias.seguridad;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.uem.ambulancias.soporte.Escenario;
import com.uem.ambulancias.soporte.Escenario.Ciudadano;
import com.uem.ambulancias.soporte.Json;
import com.uem.ambulancias.soporte.PruebaIT;
import com.uem.ambulancias.soporte.VerificadorDeTelefonoFalso;

@Tag("PB-03")
class RegistroCiudadanoIT extends PruebaIT {

	private static final String TELEFONO = "+59160000123";

	@Test
	@DisplayName("PB-03 · CP-03-01 · con el teléfono verificado por SMS se crea la cuenta y ya puede pedir ayuda")
	void primeraVez() throws Exception {
		String respuesta = cuerpo(entrar(TELEFONO, "Ana Rojas", true)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.token").isNotEmpty())
				.andExpect(jsonPath("$.ciudadano.nombreCompleto").value("Ana Rojas"))
				.andExpect(jsonPath("$.ciudadano.telefono").value("60000123")));

		pedir(post("/alertas"), Json.texto(respuesta, "$.token"), Json.objeto("latitud", Escenario.LATITUD, "longitud",
				Escenario.LONGITUD, "origenUbicacion", "GPS"))
				.andExpect(status().isCreated());
	}

	@Test
	@DisplayName("PB-03 · CP-03-02 · el mismo número vuelve a la misma cuenta, sin pedir el nombre otra vez")
	void mismoNumeroMismaCuenta() throws Exception {
		long id = Json.numero(cuerpo(entrar(TELEFONO, "Ana Rojas", true).andExpect(status().isCreated())),
				"$.ciudadano.id");

		entrar(TELEFONO, null, null)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.ciudadano.id").value(id))
				.andExpect(jsonPath("$.ciudadano.nombreCompleto").value("Ana Rojas"));
	}

	@Test
	@DisplayName("PB-03 · CP-03-03 · la primera vez exige el nombre y la aceptación del aviso de privacidad")
	void primeraVezSinDatos() throws Exception {
		entrar(TELEFONO, null, true)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("NOMBRE_REQUERIDO"));
		entrar(TELEFONO, "Ana Rojas", false)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("NOMBRE_REQUERIDO"));
	}

	@Test
	@DisplayName("PB-03 · CP-03-04 · si el teléfono no se pudo verificar, no hay sesión")
	void verificacionInvalida() throws Exception {
		pedir(post("/auth/ciudadano"), null,
				Json.objeto("idToken", "token-vencido", "nombreCompleto", "Ana Rojas", "aceptaPrivacidad", true))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("VERIFICACION_TELEFONO_INVALIDA"))
				.andExpect(jsonPath("$.token").doesNotExist());
	}

	@Test
	@DisplayName("PB-03 · CP-03-05 · con su sesión, el ciudadano no entra a lo de la central ni a lo del paramédico")
	void soloLoSuyo() throws Exception {
		Ciudadano ciudadano = escenario.ciudadano();

		pedir(get("/incidentes"), ciudadano.token()).andExpect(status().isForbidden());
		pedir(get("/paramedicos/actual"), ciudadano.token()).andExpect(status().isForbidden());
	}

	private org.springframework.test.web.servlet.ResultActions entrar(String telefono, String nombre, Boolean acepta)
			throws Exception {
		return pedir(post("/auth/ciudadano"), null, Json.objeto("idToken", VerificadorDeTelefonoFalso.tokenDe(telefono),
				"nombreCompleto", nombre, "aceptaPrivacidad", acepta));
	}

}
