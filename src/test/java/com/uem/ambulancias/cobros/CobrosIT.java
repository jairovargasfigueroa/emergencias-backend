package com.uem.ambulancias.cobros;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

@Tag("PB-29")
class CobrosIT extends PruebaIT {

	private static final LocalDate HOY = LocalDate.now(ZoneId.of("America/La_Paz"));

	private record Servicio(long incidenteId, long atencionId) {
	}

	/** Una emergencia atendida de punta a punta: el paciente quedó entregado. */
	private Servicio servicioTerminado() throws Exception {
		Paramedico paramedico = escenario.paramedicoEnTurno();
		long incidente = escenario.alertar(escenario.ciudadano()).incidenteId();
		long atencion = escenario.tomar(paramedico, incidente);
		escenario.hastaElHospital(paramedico, atencion);
		pedir(post("/atenciones/" + atencion + "/entrega"), paramedico.token(), Json.objeto("latitud",
				Escenario.LATITUD, "longitud", Escenario.LONGITUD, "destinoDescripcion", "Hospital Japonés"))
				.andExpect(status().isOk());
		return new Servicio(incidente, atencion);
	}

	private ResultActions tarifa(String tipo, String monto, LocalDate desde) throws Exception {
		return pedir(post("/tarifas"), escenario.admin(),
				"{\"tipoServicio\":\"" + tipo + "\",\"monto\":" + monto + ",\"vigenteDesde\":\"" + desde + "\"}");
	}

	private ResultActions cobrar(long atencionId, String modalidad, String importe, String estado) throws Exception {
		return pedir(post("/cobros"), escenario.admin(), "{\"atencionId\":" + atencionId + ",\"modalidad\":\""
				+ modalidad + "\",\"importe\":" + importe + ",\"estado\":\"" + estado + "\"}");
	}

	private long cobroDe(long atencionId) throws Exception {
		return Json.numero(cuerpo(cobrar(atencionId, "PAGO_DIRECTO", "350", "PENDIENTE")), "$.cobro.id");
	}

	@Test
	@DisplayName("PB-29 · CP-29-02 · una tarifa nueva rige desde su día y la anterior queda en el historial")
	void tarifasConVigencia() throws Exception {
		pedir(get("/tarifas"), escenario.admin())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.emergencia").doesNotExist())
				.andExpect(jsonPath("$.historial.length()").value(0));

		tarifa("EMERGENCIA", "350.00", HOY).andExpect(status().isCreated())
				.andExpect(jsonPath("$.emergencia.monto").value(350.0));
		tarifa("EMERGENCIA", "400.00", HOY.plusDays(1)).andExpect(status().isCreated());
		tarifa("TRASLADO", "180.50", HOY.minusDays(3)).andExpect(status().isCreated());

		pedir(get("/tarifas"), escenario.admin())
				.andExpect(jsonPath("$.emergencia.monto").value(350.0))
				.andExpect(jsonPath("$.emergencia.registradaPor").isNotEmpty())
				.andExpect(jsonPath("$.traslado.monto").value(180.5))
				.andExpect(jsonPath("$.historial.length()").value(3))
				.andExpect(jsonPath("$.historial[0].monto").value(400.0))
				.andExpect(jsonPath("$.historial[0].vigenteDesde").value(HOY.plusDays(1).toString()));
	}

	@Test
	@DisplayName("PB-29 · CP-29-02 · la tarifa pide tipo, monto válido y vigencia")
	void validaLaTarifa() throws Exception {
		tarifa("EMERGENCIA", "-10", HOY).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value("VALIDACION"));
		tarifa("EMERGENCIA", "10.555", HOY).andExpect(status().isBadRequest());
		pedir(post("/tarifas"), escenario.admin(), Json.objeto("tipoServicio", "EMERGENCIA", "monto", 100))
				.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("PB-29 · CP-29-01 · un servicio terminado se cobra con modalidad, importe, estado y responsable")
	void registraCobro() throws Exception {
		tarifa("EMERGENCIA", "350.00", HOY).andExpect(status().isCreated());
		Servicio servicio = servicioTerminado();

		pedir(get("/cobros/servicios/" + servicio.atencionId()), escenario.admin())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tipoServicio").value("EMERGENCIA"))
				.andExpect(jsonPath("$.terminado").value(true))
				.andExpect(jsonPath("$.tarifaDeReferencia.monto").value(350.0))
				.andExpect(jsonPath("$.cobro").doesNotExist());

		cobrar(servicio.atencionId(), "SOAT", "350.00", "PENDIENTE")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.cobro.modalidad").value("SOAT"))
				.andExpect(jsonPath("$.cobro.importe").value(350.0))
				.andExpect(jsonPath("$.cobro.estado").value("PENDIENTE"))
				.andExpect(jsonPath("$.cobro.registradoPor").isNotEmpty())
				.andExpect(jsonPath("$.cobro.fechaRegistro").isNotEmpty())
				.andExpect(jsonPath("$.cambios.length()").value(0));

		cobrar(servicio.atencionId(), "SOAT", "350.00", "PENDIENTE")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("COBRO_YA_REGISTRADO"));
	}

	@Test
	@DisplayName("PB-29 · CP-29-01 · un servicio en curso todavía no se cobra, y anulado no es un estado inicial")
	void soloServiciosTerminados() throws Exception {
		long terminado = servicioTerminado().atencionId();
		// Lejos del anterior, para que la alerta no se sume a ese incidente.
		Paramedico paramedico = escenario.paramedicoEnTurno();
		long enCurso = escenario.tomar(paramedico, escenario.alertar(escenario.ciudadano(), Escenario.LATITUD + 0.05,
				Escenario.LONGITUD).incidenteId());

		cobrar(enCurso, "PAGO_DIRECTO", "100", "PENDIENTE")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("SERVICIO_NO_TERMINADO"));
		pedir(get("/cobros/servicios/" + enCurso), escenario.admin())
				.andExpect(jsonPath("$.terminado").value(false));

		cobrar(terminado, "PAGO_DIRECTO", "100", "ANULADO")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("TRANSICION_INVALIDA"));
		cobrar(999999, "PAGO_DIRECTO", "100", "PENDIENTE").andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("PB-29 · CP-29-03 · un ajuste conserva el importe previo, el motivo, la fecha y quién lo hizo")
	void ajusteConservaLoAnterior() throws Exception {
		long cobro = cobroDe(servicioTerminado().atencionId());

		pedir(put("/cobros/" + cobro), escenario.admin(),
				"{\"modalidad\":\"SEGURO_MEDICO\",\"importe\":300,\"estado\":\"PAGADO\",\"motivo\":\" \"}")
				.andExpect(status().isBadRequest());
		pedir(put("/cobros/" + cobro), escenario.admin(), "{\"modalidad\":\"SEGURO_MEDICO\",\"importe\":300,"
				+ "\"estado\":\"PAGADO\",\"motivo\":\"El seguro reconoció 300 Bs.\"}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.cobro.importe").value(300.0))
				.andExpect(jsonPath("$.cobro.estado").value("PAGADO"))
				.andExpect(jsonPath("$.cambios.length()").value(1))
				.andExpect(jsonPath("$.cambios[0].tipo").value("AJUSTE"))
				.andExpect(jsonPath("$.cambios[0].importeAnterior").value(350.0))
				.andExpect(jsonPath("$.cambios[0].modalidadAnterior").value("PAGO_DIRECTO"))
				.andExpect(jsonPath("$.cambios[0].estadoAnterior").value("PENDIENTE"))
				.andExpect(jsonPath("$.cambios[0].importeNuevo").value(300.0))
				.andExpect(jsonPath("$.cambios[0].motivo").value("El seguro reconoció 300 Bs."))
				.andExpect(jsonPath("$.cambios[0].responsable").isNotEmpty())
				.andExpect(jsonPath("$.cambios[0].fechaHora").isNotEmpty());
	}

	@Test
	@DisplayName("PB-29 · CP-29-03 · anular deja el importe que tenía y después ya no se modifica")
	void anulacion() throws Exception {
		long cobro = cobroDe(servicioTerminado().atencionId());

		pedir(post("/cobros/" + cobro + "/anulacion"), escenario.admin(),
				Json.objeto("motivo", "Servicio sin costo por convenio."))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.cobro.estado").value("ANULADO"))
				.andExpect(jsonPath("$.cobro.importe").value(350.0))
				.andExpect(jsonPath("$.cambios[0].tipo").value("ANULACION"))
				.andExpect(jsonPath("$.cambios[0].importeAnterior").value(350.0))
				.andExpect(jsonPath("$.cambios[0].estadoNuevo").value("ANULADO"));

		pedir(put("/cobros/" + cobro), escenario.admin(), "{\"modalidad\":\"SOAT\",\"importe\":100,"
				+ "\"estado\":\"PAGADO\",\"motivo\":\"x\"}")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("COBRO_ANULADO"));
		pedir(post("/cobros/" + cobro + "/anulacion"), escenario.admin(), Json.objeto("motivo", "x"))
				.andExpect(status().isConflict());
	}

	@Test
	@DisplayName("PB-29 · CP-29-04 · cobrar, ajustar o anular no cambia cómo terminó el servicio")
	void noTocaElDesenlace() throws Exception {
		Servicio servicio = servicioTerminado();
		long cobro = cobroDe(servicio.atencionId());
		pedir(post("/cobros/" + cobro + "/anulacion"), escenario.admin(), Json.objeto("motivo", "Error de carga."))
				.andExpect(status().isOk());

		pedir(get("/incidentes/" + servicio.incidenteId()), escenario.admin())
				.andExpect(jsonPath("$.atenciones[0].estado").value("PACIENTE_ENTREGADO"));
		pedir(get("/cobros/servicios/" + servicio.atencionId()), escenario.admin())
				.andExpect(jsonPath("$.estadoAtencion").value("PACIENTE_ENTREGADO"));
	}

	@Test
	@DisplayName("PB-29 · CP-29-04 · solo la central ve y registra cobros y tarifas")
	void soloLaCentral() throws Exception {
		String ciudadano = escenario.ciudadano().token();
		String paramedico = escenario.paramedicoActivado().token();

		pedir(get("/tarifas"), ciudadano).andExpect(status().isForbidden());
		pedir(get("/cobros/servicios/1"), paramedico).andExpect(status().isForbidden());
		pedir(post("/cobros"), paramedico, Json.objeto("atencionId", 1)).andExpect(status().isForbidden());
		pedir(get("/tarifas"), null).andExpect(status().isUnauthorized());
	}
}
