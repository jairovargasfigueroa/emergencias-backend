package com.uem.ambulancias.emergencias;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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

@Tag("PB-07")
class TomaDeIncidenteIT extends PruebaIT {

	@Test
	@DisplayName("PB-07 · CP-07-01 · la primera unidad toma el incidente: sale en camino y la unidad queda ocupada")
	void tomar() throws Exception {
		long incidente = alertaConDatos();
		Paramedico paramedico = escenario.paramedicoEnTurno();

		tomar(paramedico, incidente)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.incidenteId").value(incidente))
				.andExpect(jsonPath("$.ambulanciaId").value(paramedico.ambulanciaId()))
				.andExpect(jsonPath("$.estado").value("EN_CAMINO"))
				.andExpect(jsonPath("$.horaToma").isNotEmpty())
				.andExpect(jsonPath("$.incidente.latitud").value(Escenario.LATITUD))
				.andExpect(jsonPath("$.incidente.cantidadAfectados").value(2))
				.andExpect(jsonPath("$.incidente.descripciones[0]").value("Chico atropellado"));

		pedir(get("/incidentes/" + incidente), escenario.admin())
				.andExpect(jsonPath("$.estado").value("EN_ATENCION"))
				.andExpect(jsonPath("$.unidadesAcudiendo").value(1));
		estadoDeLaAmbulancia(paramedico.ambulanciaId(), "EN_ATENCION");
		pedir(get("/paramedicos/actual/atencion"), paramedico.token())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.incidenteId").value(incidente));
	}

	@Test
	@DisplayName("PB-07 · CP-07-01 · el compañero de la misma unidad ve la misma atención, y la unidad no puede tomar otra")
	void companeroDeUnidad() throws Exception {
		Paramedico primero = escenario.paramedicoActivado();
		Paramedico segundo = escenario.paramedicoActivado();
		long ambulancia = escenario.ambulancia();
		escenario.asignar(primero.id(), ambulancia);
		escenario.asignar(segundo.id(), ambulancia);
		escenario.iniciarTurno(primero);
		escenario.iniciarTurno(segundo);
		long incidente = escenario.alertar(escenario.ciudadano()).incidenteId();

		long atencion = escenario.tomar(primero, incidente);

		pedir(get("/paramedicos/actual/atencion"), segundo.token())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(atencion));
		long otro = escenario.alertar(escenario.ciudadano(), Escenario.LATITUD + 0.1, Escenario.LONGITUD)
				.incidenteId();
		tomar(segundo, otro)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("AMBULANCIA_NO_DISPONIBLE"));
	}

	@Test
	@DisplayName("PB-07 · CP-07-02 · si otra unidad ya lo tomó, el 409 dice quién va y cuántos afectados hay, y no crea nada")
	void yaTomado() throws Exception {
		long incidente = alertaConDatos();
		Paramedico primero = escenario.paramedicoEnTurno();
		Paramedico segundo = escenario.paramedicoEnTurno();
		escenario.tomar(primero, incidente);

		tomar(segundo, incidente)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("INCIDENTE_YA_TOMADO"))
				.andExpect(jsonPath("$.incidenteId").value(incidente))
				.andExpect(jsonPath("$.cantidadAfectados").value(2))
				.andExpect(jsonPath("$.unidadesAcudiendo[0].ambulanciaId").value(primero.ambulanciaId()))
				.andExpect(jsonPath("$.unidadesAcudiendo[0].placa").isNotEmpty());

		estadoDeLaAmbulancia(segundo.ambulanciaId(), "DISPONIBLE");
		pedir(get("/paramedicos/actual/atencion"), segundo.token()).andExpect(status().isNoContent());
	}

	@Test
	@DisplayName("PB-07 · CP-07-03 · otra unidad se suma sabiendo que ya va una, y el incidente queda con las dos")
	void sumarse() throws Exception {
		long incidente = escenario.alertar(escenario.ciudadano()).incidenteId();
		Paramedico primero = escenario.paramedicoEnTurno();
		Paramedico segundo = escenario.paramedicoEnTurno();
		escenario.tomar(primero, incidente);

		pedir(post("/incidentes/" + incidente + "/sumarse"), segundo.token())
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.estado").value("EN_CAMINO"))
				.andExpect(jsonPath("$.ambulanciaId").value(segundo.ambulanciaId()));

		pedir(get("/incidentes/" + incidente), escenario.admin())
				.andExpect(jsonPath("$.estado").value("EN_ATENCION"))
				.andExpect(jsonPath("$.unidadesAcudiendo").value(2))
				.andExpect(jsonPath("$.atenciones.length()").value(2));
		estadoDeLaAmbulancia(segundo.ambulanciaId(), "EN_ATENCION");
	}

	@Test
	@DisplayName("PB-07 · CP-07-04 · un incidente cerrado no se toma ni admite sumarse")
	void incidenteCerrado() throws Exception {
		Ciudadano ciudadano = escenario.ciudadano();
		Alerta alerta = escenario.alertar(ciudadano);
		pedir(post("/alertas/" + alerta.alertaId() + "/cancelacion"), ciudadano.token(),
				Json.objeto("motivo", "FALSA_ALARMA"))
				.andExpect(status().isOk());
		Paramedico paramedico = escenario.paramedicoEnTurno();

		tomar(paramedico, alerta.incidenteId())
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("INCIDENTE_CERRADO"));
		pedir(post("/incidentes/" + alerta.incidenteId() + "/sumarse"), paramedico.token())
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("INCIDENTE_CERRADO"));
		estadoDeLaAmbulancia(paramedico.ambulanciaId(), "DISPONIBLE");
	}

	@Test
	@DisplayName("PB-07 · CP-07-05 · una unidad fuera de servicio no toma, y un incidente que no existe da 404")
	void unidadNoDisponible() throws Exception {
		long incidente = escenario.alertar(escenario.ciudadano()).incidenteId();
		Paramedico paramedico = escenario.paramedicoEnTurno();
		pedir(post("/ambulancias/" + paramedico.ambulanciaId() + "/fuera-de-servicio"), escenario.admin())
				.andExpect(status().isOk());

		tomar(paramedico, incidente)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("AMBULANCIA_NO_DISPONIBLE"));
		tomar(escenario.paramedicoEnTurno(), 999_999).andExpect(status().isNotFound());
		pedir(get("/incidentes/" + incidente), escenario.admin()).andExpect(jsonPath("$.estado").value("ACTIVO"));
	}

	@Test
	@DisplayName("PB-07 · CP-07-05 · solo el paramédico toma: ni el ciudadano ni la central")
	void soloParamedicos() throws Exception {
		long incidente = escenario.alertar(escenario.ciudadano()).incidenteId();

		pedir(post("/incidentes/" + incidente + "/tomar"), escenario.ciudadano().token())
				.andExpect(status().isForbidden());
		pedir(post("/incidentes/" + incidente + "/tomar"), escenario.admin()).andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("PB-07 · CP-07-06 · cuatro unidades que toman a la vez: una lo toma y las demás reciben el 409 para sumarse")
	void tomaSimultanea() throws Exception {
		int cantidad = 4;
		long incidente = escenario.alertar(escenario.ciudadano()).incidenteId();
		List<Paramedico> paramedicos = new ArrayList<>();
		for (int i = 0; i < cantidad; i++) {
			paramedicos.add(escenario.paramedicoEnTurno());
		}

		CountDownLatch largada = new CountDownLatch(1);
		ExecutorService hilos = Executors.newFixedThreadPool(cantidad);
		try {
			List<Future<Integer>> estados = new ArrayList<>();
			for (Paramedico paramedico : paramedicos) {
				Callable<Integer> toma = () -> {
					largada.await();
					return tomar(paramedico, incidente).andReturn().getResponse().getStatus();
				};
				estados.add(hilos.submit(toma));
			}
			largada.countDown();

			List<Integer> respuestas = new ArrayList<>();
			for (Future<Integer> estado : estados) {
				respuestas.add(estado.get());
			}

			assertThat(respuestas).containsOnlyOnce(201).containsOnly(201, 409);
			assertThat(jdbc.queryForObject("select count(*) from atencion where incidente_id = ?", Long.class,
					incidente)).isEqualTo(1);
			assertThat(jdbc.queryForObject("select count(*) from ambulancia where estado = 'EN_ATENCION'",
					Long.class)).isEqualTo(1);
		} finally {
			hilos.shutdownNow();
		}
	}

	private long alertaConDatos() throws Exception {
		String respuesta = cuerpo(pedir(post("/alertas"), escenario.ciudadano().token(), Json.objeto("latitud",
				Escenario.LATITUD, "longitud", Escenario.LONGITUD, "origenUbicacion", "GPS", "cantidadAfectados", 2,
				"descripcion", "Chico atropellado"))
				.andExpect(status().isCreated()));
		return Json.numero(respuesta, "$.incidenteId");
	}

	private ResultActions tomar(Paramedico paramedico, long incidente) throws Exception {
		return pedir(post("/incidentes/" + incidente + "/tomar"), paramedico.token());
	}

	private void estadoDeLaAmbulancia(long ambulancia, String estado) throws Exception {
		pedir(get("/ambulancias"), escenario.admin())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.id == " + ambulancia + ")].estado", hasItem(estado)));
	}

}
