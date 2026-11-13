package com.uem.ambulancias.emergencias;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import com.uem.ambulancias.soporte.Escenario;
import com.uem.ambulancias.soporte.Escenario.Paramedico;
import com.uem.ambulancias.soporte.Json;
import com.uem.ambulancias.soporte.PruebaIT;

@Tag("PB-22")
class ReporteDeOperacionIT extends PruebaIT {

	private static final ZoneId LA_PAZ = ZoneId.of("America/La_Paz");

	private ResultActions reporte(LocalDate desde, LocalDate hasta) throws Exception {
		return pedir(get("/reportes/operacion").param("desde", desde.toString()).param("hasta", hasta.toString()),
				escenario.admin());
	}

	private long incidenteEn(double desplazamiento) {
		return escenario.alertar(escenario.ciudadano(), Escenario.LATITUD + desplazamiento, Escenario.LONGITUD)
				.incidenteId();
	}

	@Test
	@DisplayName("PB-22 · CP-22-01 · cuenta emergencias y traslados del período con su estado final")
	void cuentaServicios() throws Exception {
		LocalDate hoy = LocalDate.now(LA_PAZ);
		long atendido = incidenteEn(0);
		long falsa = incidenteEn(0.05);
		incidenteEn(0.10);
		Paramedico paramedico = escenario.paramedicoEnTurno();
		long atencion = escenario.tomar(paramedico, atendido);
		escenario.hastaElHospital(paramedico, atencion);
		pedir(post("/incidentes/" + falsa + "/cierre"), escenario.admin(),
				Json.objeto("motivo", "FALSA_ALARMA_VERIFICADA", "detalle", "Nadie en el lugar."))
				.andExpect(status().isNoContent());
		escenario.pedirTraslado(escenario.ciudadano(), Escenario.pedidoDeTraslado());

		reporte(hoy, hoy)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.desde").value(hoy.toString()))
				.andExpect(jsonPath("$.generadoEn").isNotEmpty())
				.andExpect(jsonPath("$.emergencias.total").value(3))
				.andExpect(jsonPath("$.emergencias.porEstado.EN_ATENCION").value(1))
				.andExpect(jsonPath("$.emergencias.porEstado.FALSA_ALARMA").value(1))
				.andExpect(jsonPath("$.emergencias.porEstado.ACTIVO").value(1))
				.andExpect(jsonPath("$.emergencias.porEstado.CANCELADO").value(0))
				.andExpect(jsonPath("$.traslados.total").value(1))
				.andExpect(jsonPath("$.traslados.porEstado.BUSCANDO_UNIDAD").value(1));
	}

	@Test
	@DisplayName("PB-22 · CP-22-02 · el tiempo de respuesta sale de los hitos y lo incompleto se cuenta aparte")
	void tiemposDeRespuesta() throws Exception {
		LocalDate hoy = LocalDate.now(LA_PAZ);
		long conLlegada = incidenteEn(0);
		long sinLlegada = incidenteEn(0.05);
		incidenteEn(0.10);
		Paramedico primero = escenario.paramedicoEnTurno();
		escenario.marcar(primero, escenario.tomar(primero, conLlegada), "llegada");
		Paramedico segundo = escenario.paramedicoEnTurno();
		escenario.tomar(segundo, sinLlegada);

		reporte(hoy, hoy)
				.andExpect(jsonPath("$.tiemposDeEmergencia.hastaLaToma.medidos").value(2))
				.andExpect(jsonPath("$.tiemposDeEmergencia.hastaLaLlegada.medidos").value(1))
				.andExpect(jsonPath("$.tiemposDeEmergencia.hastaLaLlegada.promedioSegundos").isNumber())
				.andExpect(jsonPath("$.tiemposDeEmergencia.sinLlegada").value(1))
				.andExpect(jsonPath("$.tiemposDeEmergencia.sinUnidad").value(1))
				.andExpect(jsonPath("$.criterio.length()").value(5));
	}

	@Test
	@DisplayName("PB-22 · CP-22-03 · desglosa por día y por unidad, con totales que cuadran")
	void desglose() throws Exception {
		LocalDate hoy = LocalDate.now(LA_PAZ);
		long ayer = incidenteEn(0);
		jdbc.update("update incidente set fecha_hora_creacion = fecha_hora_creacion - interval '1 day' where id = ?",
				ayer);
		long deHoy = incidenteEn(0.05);
		Paramedico paramedico = escenario.paramedicoEnTurno();
		escenario.marcar(paramedico, escenario.tomar(paramedico, deHoy), "llegada");

		reporte(hoy.minusDays(1), hoy)
				.andExpect(jsonPath("$.emergencias.total").value(2))
				.andExpect(jsonPath("$.porDia.length()").value(2))
				.andExpect(jsonPath("$.porDia[0].dia").value(hoy.minusDays(1).toString()))
				.andExpect(jsonPath("$.porDia[0].emergencias").value(1))
				.andExpect(jsonPath("$.porDia[1].emergencias").value(1))
				.andExpect(jsonPath("$.porUnidad.length()").value(1))
				.andExpect(jsonPath("$.porUnidad[0].placa").isNotEmpty())
				.andExpect(jsonPath("$.porUnidad[0].emergencias").value(1))
				.andExpect(jsonPath("$.porUnidad[0].hastaLaLlegada.medidos").value(1));

		reporte(hoy, hoy)
				.andExpect(jsonPath("$.emergencias.total").value(1))
				.andExpect(jsonPath("$.porDia.length()").value(1));
	}

	@Test
	@DisplayName("PB-22 · CP-22-04 · un período sin datos da ceros y sin tiempos inventados")
	void periodoVacio() throws Exception {
		LocalDate haceUnMes = LocalDate.now(LA_PAZ).minusMonths(1);

		reporte(haceUnMes, haceUnMes)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.emergencias.total").value(0))
				.andExpect(jsonPath("$.tiemposDeEmergencia.hastaLaLlegada.medidos").value(0))
				.andExpect(jsonPath("$.tiemposDeEmergencia.hastaLaLlegada.promedioSegundos").doesNotExist())
				.andExpect(jsonPath("$.porUnidad.length()").value(0));
	}

	@Test
	@DisplayName("PB-22 · CP-22-01 · el período tiene que ir en orden y no pasar de un año")
	void validaElPeriodo() throws Exception {
		LocalDate hoy = LocalDate.now(LA_PAZ);

		reporte(hoy, hoy.minusDays(1))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value("VALIDACION"));
		reporte(hoy.minusDays(400), hoy).andExpect(status().isBadRequest());
		pedir(get("/reportes/operacion").param("desde", "ayer").param("hasta", hoy.toString()), escenario.admin())
				.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("PB-22 · CP-22-01 · solo la central ve los reportes")
	void soloLaCentral() throws Exception {
		String hoy = LocalDate.now(LA_PAZ).toString();

		pedir(get("/reportes/operacion").param("desde", hoy).param("hasta", hoy), escenario.ciudadano().token())
				.andExpect(status().isForbidden());
		pedir(get("/reportes/operacion").param("desde", hoy).param("hasta", hoy), null)
				.andExpect(status().isUnauthorized());
	}
}
