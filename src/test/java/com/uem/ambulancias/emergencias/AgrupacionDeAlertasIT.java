package com.uem.ambulancias.emergencias;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.uem.ambulancias.soporte.Escenario;
import com.uem.ambulancias.soporte.Escenario.Alerta;
import com.uem.ambulancias.soporte.Escenario.Ciudadano;
import com.uem.ambulancias.soporte.Json;
import com.uem.ambulancias.soporte.PruebaIT;

/**
 * Agrupación con los valores de las pruebas, que son los de producción: radio de 150 m y ventana de 30 minutos.
 */
@Tag("PB-05")
class AgrupacionDeAlertasIT extends PruebaIT {

	@Test
	@DisplayName("PB-05 · CP-05-05 · dos personas que avisan a 100 m quedan en el mismo incidente, con el mayor número de afectados")
	void mismoSuceso() throws Exception {
		Alerta primera = alertar(escenario.ciudadano(), Escenario.LATITUD, 2);
		Alerta segunda = alertar(escenario.ciudadano(), Escenario.LATITUD + Escenario.CIEN_METROS, 4);

		assertThat(segunda.incidenteId()).isEqualTo(primera.incidenteId());
		pedir(get("/incidentes/" + primera.incidenteId()), escenario.admin())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.cantidadAlertas").value(2))
				.andExpect(jsonPath("$.cantidadAfectados").value(4));
	}

	@Test
	@DisplayName("PB-05 · CP-05-05 · un aviso a 200 m es otro incidente")
	void fueraDelRadio() {
		Alerta primera = escenario.alertar(escenario.ciudadano());
		Alerta lejos = escenario.alertar(escenario.ciudadano(), Escenario.LATITUD + 2 * Escenario.CIEN_METROS,
				Escenario.LONGITUD);

		assertThat(lejos.incidenteId()).isNotEqualTo(primera.incidenteId());
	}

	@Test
	@DisplayName("PB-05 · CP-05-05 · un aviso en el mismo lugar después de 30 minutos es otro incidente")
	void fueraDeLaVentana() {
		Alerta vieja = escenario.alertar(escenario.ciudadano());
		jdbc.update("update incidente set fecha_hora_creacion = now() - interval '31 minutes' where id = ?",
				vieja.incidenteId());

		Alerta nueva = escenario.alertar(escenario.ciudadano());

		assertThat(nueva.incidenteId()).isNotEqualTo(vieja.incidenteId());
	}

	@Test
	@DisplayName("PB-05 · CP-05-05 · un aviso junto a un incidente ya cerrado abre uno nuevo")
	void incidenteCerrado() throws Exception {
		Ciudadano ciudadano = escenario.ciudadano();
		Alerta cerrada = escenario.alertar(ciudadano);
		pedir(post("/alertas/" + cerrada.alertaId() + "/cancelacion"), ciudadano.token(),
				Json.objeto("motivo", "ERROR"))
				.andExpect(status().isOk());

		Alerta nueva = escenario.alertar(escenario.ciudadano());

		assertThat(nueva.incidenteId()).isNotEqualTo(cerrada.incidenteId());
	}

	@Test
	@DisplayName("PB-05 · CP-05-05 · un aviso junto a un incidente que ya está en atención se suma a ese incidente")
	void incidenteEnAtencion() {
		Alerta primera = escenario.alertar(escenario.ciudadano());
		escenario.tomar(escenario.paramedicoEnTurno(), primera.incidenteId());

		Alerta segunda = escenario.alertar(escenario.ciudadano());

		assertThat(segunda.incidenteId()).isEqualTo(primera.incidenteId());
	}

	@Test
	@DisplayName("PB-05 · CP-05-06 · ocho avisos simultáneos del mismo lugar abren un solo incidente")
	void avisosSimultaneos() throws Exception {
		int cantidad = 8;
		List<Ciudadano> ciudadanos = new ArrayList<>();
		for (int i = 0; i < cantidad; i++) {
			ciudadanos.add(escenario.ciudadano());
		}

		CountDownLatch largada = new CountDownLatch(1);
		ExecutorService hilos = Executors.newFixedThreadPool(cantidad);
		try {
			List<Future<Long>> incidentes = new ArrayList<>();
			for (Ciudadano ciudadano : ciudadanos) {
				Callable<Long> aviso = () -> {
					largada.await();
					return alertar(ciudadano, Escenario.LATITUD, null).incidenteId();
				};
				incidentes.add(hilos.submit(aviso));
			}
			largada.countDown();

			Set<Long> distintos = incidentes.stream().map(AgrupacionDeAlertasIT::esperar).collect(Collectors.toSet());

			assertThat(distintos).hasSize(1);
			assertThat(jdbc.queryForObject("select count(*) from incidente", Long.class)).isEqualTo(1);
			assertThat(jdbc.queryForObject("select count(*) from alerta", Long.class)).isEqualTo(cantidad);
		} finally {
			hilos.shutdownNow();
		}
	}

	private Alerta alertar(Ciudadano ciudadano, double latitud, Integer afectados) throws Exception {
		String respuesta = cuerpo(pedir(post("/alertas"), ciudadano.token(), Json.objeto("latitud", latitud,
				"longitud", Escenario.LONGITUD, "origenUbicacion", "GPS", "cantidadAfectados", afectados))
				.andExpect(status().isCreated()));
		return new Alerta(Json.numero(respuesta, "$.alertaId"), Json.numero(respuesta, "$.incidenteId"));
	}

	private static Long esperar(Future<Long> resultado) {
		try {
			return resultado.get();
		} catch (Exception e) {
			throw new IllegalStateException("Un aviso simultáneo falló: " + e.getMessage(), e);
		}
	}

}
