package com.uem.ambulancias.emergencias;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

@Tag("PB-16")
class DespachoIT extends PruebaIT {

	@Test
	@DisplayName("PB-16 · CP-16-01 · las candidatas son las unidades libres, de la más cercana a la más lejana; sin posición, al final")
	void candidatasPorCercania() throws Exception {
		Paramedico lejos = enTurnoEn(Escenario.LATITUD + 0.03);
		Paramedico sinPosicion = escenario.paramedicoEnTurno();
		Paramedico cerca = enTurnoEn(Escenario.LATITUD + 3 * Escenario.CIEN_METROS);
		Paramedico ocupado = enTurnoEn(Escenario.LATITUD + Escenario.CIEN_METROS);
		escenario.tomar(ocupado, escenario.alertar(escenario.ciudadano(), Escenario.LATITUD + 0.2, Escenario.LONGITUD)
				.incidenteId());
		long sinTurno = escenario.ambulancia();
		long incidente = escenario.alertar(escenario.ciudadano()).incidenteId();

		candidatas(incidente)
				.andExpect(jsonPath("$.length()").value(3))
				.andExpect(jsonPath("$[0].ambulanciaId").value(cerca.ambulanciaId()))
				.andExpect(jsonPath("$[0].distanciaMetros").value(closeTo(300.0, 15.0)))
				.andExpect(jsonPath("$[0].posicionReciente").value(true))
				.andExpect(jsonPath("$[0].yaLoTuvo").value(false))
				.andExpect(jsonPath("$[1].ambulanciaId").value(lejos.ambulanciaId()))
				.andExpect(jsonPath("$[2].ambulanciaId").value(sinPosicion.ambulanciaId()))
				.andExpect(jsonPath("$[2].distanciaMetros").doesNotExist())
				.andExpect(jsonPath("$[*].ambulanciaId", not(hasItem((int) sinTurno))))
				.andExpect(jsonPath("$[*].ambulanciaId", not(hasItem(ocupado.ambulanciaId().intValue()))));
	}

	@Test
	@DisplayName("PB-16 · CP-16-01 · la unidad que ya estuvo en el caso y lo dejó figura marcada")
	void yaLoTuvo() throws Exception {
		long incidente = escenario.alertar(escenario.ciudadano()).incidenteId();
		Paramedico paramedico = escenario.paramedicoEnTurno();
		long atencion = escenario.tomar(paramedico, incidente);
		pedir(post("/atenciones/" + atencion + "/cancelar"), paramedico.token(), Json.objeto("motivo", "DESVIADA"))
				.andExpect(status().isOk());

		candidatas(incidente)
				.andExpect(jsonPath("$[?(@.ambulanciaId == " + paramedico.ambulanciaId() + ")].yaLoTuvo",
						contains(true)));
	}

	@Test
	@DisplayName("PB-16 · CP-16-02 · al despachar se crea la atención de esa unidad, queda quién la mandó y la tripulación recibe el push")
	void despachar() throws Exception {
		Ciudadano ciudadano = escenario.ciudadano();
		escenario.recibirAvisos(ciudadano, "push-ciudadano");
		long incidente = escenario.alertar(ciudadano).incidenteId();
		Paramedico paramedico = escenario.paramedicoEnTurno();
		escenario.recibirAvisos(paramedico, "push-tripulacion");

		despachar(incidente, paramedico.ambulanciaId()).andExpect(status().isNoContent());

		pedir(get("/incidentes/" + incidente), escenario.admin())
				.andExpect(jsonPath("$.estado").value("EN_ATENCION"))
				.andExpect(jsonPath("$.atenciones[0].ambulanciaId").value(paramedico.ambulanciaId()))
				.andExpect(jsonPath("$.atenciones[0].estado").value("EN_CAMINO"))
				.andExpect(jsonPath("$.atenciones[0].paramedicoResponsable.id").value(paramedico.id()));
		pedir(get("/paramedicos/actual/atencion"), paramedico.token())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.incidenteId").value(incidente));
		Long despachadaPor = jdbc.queryForObject("select asignado_por_id from atencion where incidente_id = ?",
				Long.class, incidente);
		Long administrador = jdbc.queryForObject("select id from usuario where rol = 'ADMIN'", Long.class);
		assertThat(despachadaPor).isEqualTo(administrador);
		assertThat(publicaciones.avisadosDelDespacho(incidente)).containsExactly("push-tripulacion");
		assertThat(publicaciones.avisosAlCiudadano("push-ciudadano").getLast().datos())
				.containsEntry("tipo", "UNIDAD_EN_CAMINO");
	}

	@Test
	@DisplayName("PB-16 · CP-16-03 · como refuerzo, la unidad enviada se suma sin reemplazar a la que ya iba")
	void refuerzo() throws Exception {
		long incidente = escenario.alertar(escenario.ciudadano()).incidenteId();
		Paramedico primera = escenario.paramedicoEnTurno();
		long atencionPrimera = escenario.tomar(primera, incidente);
		escenario.marcar(primera, atencionPrimera, "llegada");
		Paramedico refuerzo = escenario.paramedicoEnTurno();

		despachar(incidente, refuerzo.ambulanciaId()).andExpect(status().isNoContent());

		pedir(get("/incidentes/" + incidente), escenario.admin())
				.andExpect(jsonPath("$.unidadesAcudiendo").value(2))
				.andExpect(jsonPath("$.atenciones[0].id").value(atencionPrimera))
				.andExpect(jsonPath("$.atenciones[0].estado").value("EN_EL_LUGAR"))
				.andExpect(jsonPath("$.atenciones[1].ambulanciaId").value(refuerzo.ambulanciaId()))
				.andExpect(jsonPath("$.atenciones[1].estado").value("EN_CAMINO"));
	}

	@Test
	@DisplayName("PB-16 · CP-16-04 · no se despacha a un incidente cerrado")
	void incidenteCerrado() throws Exception {
		Ciudadano ciudadano = escenario.ciudadano();
		Alerta alerta = escenario.alertar(ciudadano);
		pedir(post("/alertas/" + alerta.alertaId() + "/cancelacion"), ciudadano.token(),
				Json.objeto("motivo", "FALSA_ALARMA"))
				.andExpect(status().isOk());

		despachar(alerta.incidenteId(), escenario.paramedicoEnTurno().ambulanciaId())
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("INCIDENTE_CERRADO"));
	}

	@Test
	@DisplayName("PB-16 · CP-16-04 · no se despacha una unidad ocupada, averiada o sin nadie de turno")
	void unidadNoDisponible() throws Exception {
		long incidente = escenario.alertar(escenario.ciudadano()).incidenteId();
		Paramedico ocupado = escenario.paramedicoEnTurno();
		escenario.tomar(ocupado, escenario.alertar(escenario.ciudadano(), Escenario.LATITUD + 0.2, Escenario.LONGITUD)
				.incidenteId());
		Paramedico averiado = escenario.paramedicoEnTurno();
		pedir(post("/ambulancias/" + averiado.ambulanciaId() + "/fuera-de-servicio"), escenario.admin())
				.andExpect(status().isOk());
		long sinTurno = escenario.ambulancia();

		for (long ambulancia : new long[] { ocupado.ambulanciaId(), averiado.ambulanciaId(), sinTurno }) {
			despachar(incidente, ambulancia)
					.andExpect(status().isConflict())
					.andExpect(jsonPath("$.codigo").value("AMBULANCIA_NO_DISPONIBLE"));
		}
		pedir(get("/incidentes/" + incidente), escenario.admin())
				.andExpect(jsonPath("$.estado").value("ACTIVO"))
				.andExpect(jsonPath("$.atenciones.length()").value(0));
	}

	@Test
	@DisplayName("PB-16 · CP-16-04 · sin unidad elegida o con un incidente que no existe se rechaza; solo despacha la central")
	void pedidoInvalido() throws Exception {
		long incidente = escenario.alertar(escenario.ciudadano()).incidenteId();
		Paramedico paramedico = escenario.paramedicoEnTurno();

		pedir(post("/incidentes/" + incidente + "/despacho"), escenario.admin(), Json.objeto("ambulanciaId", null))
				.andExpect(status().isBadRequest());
		despachar(999_999, paramedico.ambulanciaId()).andExpect(status().isNotFound());
		pedir(post("/incidentes/" + incidente + "/despacho"), paramedico.token(),
				Json.objeto("ambulanciaId", paramedico.ambulanciaId()))
				.andExpect(status().isForbidden());
		pedir(get("/incidentes/" + incidente + "/unidades"), paramedico.token()).andExpect(status().isForbidden());
	}

	private Paramedico enTurnoEn(double latitud) {
		Paramedico paramedico = escenario.paramedicoEnTurno();
		escenario.reportarPosicion(paramedico, latitud, Escenario.LONGITUD);
		return paramedico;
	}

	private ResultActions candidatas(long incidente) throws Exception {
		return pedir(get("/incidentes/" + incidente + "/unidades"), escenario.admin()).andExpect(status().isOk());
	}

	private ResultActions despachar(long incidente, long ambulancia) throws Exception {
		return pedir(post("/incidentes/" + incidente + "/despacho"), escenario.admin(),
				Json.objeto("ambulanciaId", ambulancia));
	}

}
