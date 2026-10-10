package com.uem.ambulancias.emergencias;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import com.uem.ambulancias.emergencias.service.AsignadorDeTraslados;
import com.uem.ambulancias.emergencias.service.PlanificadorDeTraslados;
import com.uem.ambulancias.soporte.Escenario;
import com.uem.ambulancias.soporte.Escenario.Ciudadano;
import com.uem.ambulancias.soporte.Escenario.Paramedico;
import com.uem.ambulancias.soporte.Json;
import com.uem.ambulancias.soporte.PruebaIT;

/**
 * La agenda de traslados de la central. El barrido que abre las búsquedas y asigna unidades corre cada dos minutos en
 * producción; acá cada prueba lo dispara cuando quiere, y mueve las horas del traslado en la base en vez de esperar.
 */
@Tag("PB-27")
class TrasladosDeLaCentralIT extends PruebaIT {

	private static final ZoneId LA_PAZ = ZoneId.of("America/La_Paz");

	@Autowired
	private PlanificadorDeTraslados planificador;

	@Autowired
	private AsignadorDeTraslados asignador;

	@Test
	@DisplayName("PB-27 · CP-27-01 · la agenda muestra los traslados de cada día, aparte de las emergencias")
	void agendaDelDia() throws Exception {
		Ciudadano ciudadano = escenario.ciudadano();
		long ahora = escenario.pedirTraslado(ciudadano, Escenario.pedidoDeTraslado());
		long programado = escenario.pedirTraslado(ciudadano, conCita(manana()));
		escenario.alertar(escenario.ciudadano());

		pedir(get("/traslados").param("dia", diaDeSalida(ahora).toString()), escenario.admin())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].traslado.id", hasItem((int) ahora)))
				.andExpect(jsonPath("$[*].traslado.id", not(hasItem((int) programado))));
		pedir(get("/traslados").param("dia", diaDeSalida(programado).toString()), escenario.admin())
				.andExpect(jsonPath("$[*].traslado.id", hasItem((int) programado)));
		pedir(get("/traslados").param("dia", LocalDate.now(LA_PAZ).plusDays(5).toString()), escenario.admin())
				.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	@DisplayName("PB-27 · CP-27-02 · la búsqueda no empieza al pedirlo sino a la hora de salir")
	void buscaALaHoraDeSalir() throws Exception {
		Paramedico unidad = escenario.unidadParaTraslados("II", 0.002);
		escenario.recibirAvisos(unidad, "push-tripulacion");
		Ciudadano ciudadano = escenario.ciudadano();
		escenario.recibirAvisos(ciudadano, "push-ciudadano");
		long traslado = escenario.pedirTraslado(ciudadano, conCita(manana()));

		planificador.barrer();
		assertThat(estado(traslado)).isEqualTo("PROGRAMADO");
		estadoDeLaAmbulancia(unidad.ambulanciaId(), "DISPONIBLE");

		jdbc.update("update traslado set hora_salida_estimada = now() - interval '1 minute' where id = ?", traslado);
		planificador.barrer();

		assertThat(estado(traslado)).isEqualTo("ASIGNADO");
		estadoDeLaAmbulancia(unidad.ambulanciaId(), "EN_ATENCION");
		pedir(get("/traslados/" + traslado), escenario.admin())
				.andExpect(jsonPath("$.estadoAtencion").value("EN_CAMINO"))
				.andExpect(jsonPath("$.paramedico").isNotEmpty());
		assertThat(publicaciones.avisosDeTrasladoA("push-tripulacion", traslado)).containsExactly("ASIGNADO");
		assertThat(publicaciones.avisosAlCiudadano("push-ciudadano").getLast().datos())
				.containsEntry("tipo", "UNIDAD_ASIGNADA");
	}

	@Test
	@DisplayName("PB-27 · CP-27-02 · se asigna sola la unidad más cercana que alcanza y que reportó su posición hace poco")
	void unidadQueSeElige() throws Exception {
		escenario.unidadParaTraslados("IA", 0.001);
		Paramedico sinSenal = escenario.unidadParaTraslados("II", 0.002);
		jdbc.update("update ambulancia set ultima_posicion_en = now() - interval '10 minutes' where id = ?",
				sinSenal.ambulanciaId());
		Paramedico laQueVa = escenario.unidadParaTraslados("III", 0.01);
		escenario.unidadParaTraslados("II", 0.03);
		long traslado = escenario.pedirTraslado(escenario.ciudadano(), con("movilidad", "CAMILLA"));

		planificador.barrer();

		assertThat(jdbc.queryForObject("select ambulancia_id from atencion where traslado_id = ?", Long.class,
				traslado)).isEqualTo(laQueVa.ambulanciaId());
	}

	@Test
	@DisplayName("PB-27 · CP-27-01 · sin unidad que sirva, el traslado queda en la bandeja de problemas; uno que aún no sale, no")
	void bandejaDeProblemas() throws Exception {
		Ciudadano ciudadano = escenario.ciudadano();
		long sinUnidad = escenario.pedirTraslado(ciudadano, Escenario.pedidoDeTraslado());
		long programado = escenario.pedirTraslado(ciudadano, conCita(manana()));

		planificador.barrer();

		pedir(get("/traslados/problemas"), escenario.admin())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].traslado.id", contains((int) sinUnidad)))
				.andExpect(jsonPath("$[0].problema").value("SIN_UNIDAD"));
		assertThat(estado(programado)).isEqualTo("PROGRAMADO");
	}

	@Test
	@DisplayName("PB-27 · CP-27-03 · la central asigna a mano una candidata, y queda quién la eligió")
	void asignarAMano() throws Exception {
		Paramedico sinPosicion = escenario.paramedicoEnTurno("II");
		long traslado = escenario.pedirTraslado(escenario.ciudadano(), Escenario.pedidoDeTraslado());
		planificador.barrer();
		assertThat(estado(traslado)).isEqualTo("BUSCANDO_UNIDAD");

		pedir(get("/traslados/" + traslado + "/unidades"), escenario.admin())
				.andExpect(jsonPath("$[*].ambulanciaId", hasItem(sinPosicion.ambulanciaId().intValue())));
		asignar(traslado, sinPosicion.ambulanciaId())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.traslado.estado").value("ASIGNADO"))
				.andExpect(jsonPath("$.estadoAtencion").value("EN_CAMINO"))
				.andExpect(jsonPath("$.placa").isNotEmpty());

		Long asignadoPor = jdbc.queryForObject("select asignado_por_id from atencion where traslado_id = ?",
				Long.class, traslado);
		assertThat(asignadoPor).isEqualTo(jdbc.queryForObject("select id from usuario where rol = 'ADMIN'",
				Long.class));
		asignar(traslado, escenario.paramedicoEnTurno("II").ambulanciaId())
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("TRANSICION_INVALIDA"));
	}

	@Test
	@DisplayName("PB-27 · CP-27-03 · a mano tampoco se asigna una unidad que no alcanza, ni una ocupada o sin nadie de turno")
	void asignacionRechazada() throws Exception {
		Paramedico chica = escenario.paramedicoEnTurno("IA");
		Paramedico ocupada = escenario.paramedicoEnTurno("III");
		escenario.tomar(ocupada, escenario.alertar(escenario.ciudadano()).incidenteId());
		long sinTurno = escenario.ambulancia("III");
		long traslado = escenario.pedirTraslado(escenario.ciudadano(), con("movilidad", "CAMILLA"));

		asignar(traslado, chica.ambulanciaId())
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("UNIDAD_INSUFICIENTE"));
		asignar(traslado, ocupada.ambulanciaId())
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("AMBULANCIA_NO_DISPONIBLE"));
		asignar(traslado, sinTurno)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("AMBULANCIA_NO_DISPONIBLE"));
		pedir(post("/traslados/" + traslado + "/asignar"), escenario.admin(), Json.objeto("ambulanciaId", null))
				.andExpect(status().isBadRequest());
		assertThat(estado(traslado)).isEqualTo("BUSCANDO_UNIDAD");
	}

	@Test
	@DisplayName("PB-27 · CP-27-04 · si se pasa la última hora de salida sin unidad, queda no cubierto y se avisa a la familia")
	void noCubierto() throws Exception {
		Ciudadano ciudadano = escenario.ciudadano();
		escenario.recibirAvisos(ciudadano, "push-ciudadano");
		long traslado = escenario.pedirTraslado(ciudadano, Escenario.pedidoDeTraslado());
		pasarElTiempo(traslado, Duration.ofMinutes(61));

		planificador.barrer();

		assertThat(estado(traslado)).isEqualTo("NO_CUBIERTO");
		assertThat(publicaciones.avisosAlCiudadano("push-ciudadano").getLast().datos())
				.containsEntry("tipo", "NO_CUBIERTO");
		pedir(get("/traslados/problemas"), escenario.admin())
				.andExpect(jsonPath("$[0].traslado.id").value(traslado))
				.andExpect(jsonPath("$[0].problema").value("NO_CUBIERTO"));
	}

	@Test
	@DisplayName("PB-27 · CP-27-04 · cuando la central habló con la familia lo marca, y sale de la bandeja")
	void familiaAvisada() throws Exception {
		Ciudadano ciudadano = escenario.ciudadano();
		long noCubierto = escenario.pedirTraslado(ciudadano, Escenario.pedidoDeTraslado());
		pasarElTiempo(noCubierto, Duration.ofMinutes(61));
		planificador.barrer();
		long programado = escenario.pedirTraslado(ciudadano, conCita(manana()));

		pedir(post("/traslados/" + noCubierto + "/familia-avisada"), escenario.admin())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.horaFamiliaAvisada").isNotEmpty());

		pedir(get("/traslados/problemas"), escenario.admin()).andExpect(jsonPath("$.length()").value(0));
		pedir(post("/traslados/" + programado + "/familia-avisada"), escenario.admin())
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("TRANSICION_INVALIDA"));
	}

	@Test
	@DisplayName("PB-27 · CP-27-03 · a una unidad atrasada la central le saca el traslado, y vuelve a buscarse otra")
	void devolverABusqueda() throws Exception {
		Paramedico atrasada = escenario.unidadParaTraslados("II", 0.002);
		escenario.recibirAvisos(atrasada, "push-atrasada");
		long traslado = escenario.pedirTraslado(escenario.ciudadano(), Escenario.pedidoDeTraslado());
		planificador.barrer();
		jdbc.update("update traslado set hora_recogida_hasta = now() - interval '1 minute' where id = ?", traslado);
		pedir(get("/traslados/problemas"), escenario.admin())
				.andExpect(jsonPath("$[0].problema").value("UNIDAD_ATRASADA"));

		pedir(post("/traslados/" + traslado + "/devolver"), escenario.admin())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.traslado.estado").value("BUSCANDO_UNIDAD"))
				.andExpect(jsonPath("$.horaDevolucion").isNotEmpty());

		assertThat(jdbc.queryForObject("select motivo_cancelacion from atencion where traslado_id = ?", String.class,
				traslado)).isEqualTo("REASIGNADA");
		estadoDeLaAmbulancia(atrasada.ambulanciaId(), "DISPONIBLE");
		assertThat(publicaciones.avisosDeTrasladoA("push-atrasada", traslado))
				.containsExactly("ASIGNADO", "RETIRADO_REASIGNADA");
		planificador.barrer();
		assertThat(estado(traslado)).isEqualTo("BUSCANDO_UNIDAD");
	}

	@Test
	@DisplayName("PB-27 · CP-27-03 · no se le saca el traslado a una unidad que ya llegó")
	void noSeDevuelveSiLlego() throws Exception {
		Paramedico unidad = escenario.unidadParaTraslados("II", 0.002);
		long traslado = escenario.pedirTraslado(escenario.ciudadano(), Escenario.pedidoDeTraslado());
		planificador.barrer();
		long atencion = jdbc.queryForObject("select id from atencion where traslado_id = ?", Long.class, traslado);
		escenario.marcar(unidad, atencion, "llegada");

		pedir(post("/traslados/" + traslado + "/devolver"), escenario.admin())
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("TRANSICION_INVALIDA"));
	}

	@Test
	@DisplayName("PB-27 · CP-27-02 · la unidad que se libera se lleva el traslado que estaba esperando")
	void unidadQueSeLibera() throws Exception {
		Paramedico unidad = escenario.unidadParaTraslados("II", 0.002);
		long atencion = escenario.tomar(unidad, escenario.alertar(escenario.ciudadano()).incidenteId());
		long traslado = escenario.pedirTraslado(escenario.ciudadano(), Escenario.pedidoDeTraslado());
		planificador.barrer();
		assertThat(estado(traslado)).isEqualTo("BUSCANDO_UNIDAD");

		escenario.marcar(unidad, atencion, "llegada");
		pedir(post("/atenciones/" + atencion + "/sin-traslado"), unidad.token(), Json.objeto("latitud",
				Escenario.LATITUD, "longitud", Escenario.LONGITUD, "motivo", "ATENDIDO_EN_EL_LUGAR"))
				.andExpect(status().isOk());
		escenario.liberar(unidad, atencion);

		assertThat(estado(traslado)).isEqualTo("ASIGNADO");
		assertThat(jdbc.queryForObject("select ambulancia_id from atencion where traslado_id = ?", Long.class,
				traslado)).isEqualTo(unidad.ambulanciaId());
	}

	@Test
	@DisplayName("PB-27 · CP-27-05 · a las 19:00 se recuerda una vez a la familia el traslado de mañana")
	void recordatorio() throws Exception {
		Ciudadano ciudadano = escenario.ciudadano();
		escenario.recibirAvisos(ciudadano, "push-ciudadano");
		ZonedDateTime hoy = ZonedDateTime.now(LA_PAZ).truncatedTo(ChronoUnit.DAYS);
		long traslado = escenario.pedirTraslado(ciudadano, conCita(hoy.plusDays(1).withHour(10).toInstant()));
		jdbc.update("update traslado set fecha_hora_creacion = ? where id = ?",
				Timestamp.from(hoy.withHour(8).toInstant()), traslado);

		assertThat(asignador.recordarLosDeManana(hoy.withHour(18).toInstant())).isZero();
		assertThat(asignador.recordarLosDeManana(hoy.withHour(20).toInstant())).isEqualTo(1);
		assertThat(asignador.recordarLosDeManana(hoy.withHour(21).toInstant())).isZero();

		assertThat(publicaciones.avisosAlCiudadano("push-ciudadano"))
				.extracting(aviso -> aviso.datos().get("tipo"))
				.containsExactly("RECORDATORIO");
		assertThat(jdbc.queryForObject("select hora_recordatorio is not null from traslado where id = ?",
				Boolean.class, traslado)).isTrue();
	}

	@Test
	@DisplayName("PB-27 · CP-27-06 · la agenda y la asignación son de la central")
	void soloLaCentral() throws Exception {
		Paramedico paramedico = escenario.paramedicoEnTurno("II");
		long traslado = escenario.pedirTraslado(escenario.ciudadano(), Escenario.pedidoDeTraslado());

		pedir(get("/traslados"), paramedico.token()).andExpect(status().isForbidden());
		pedir(get("/traslados/problemas"), escenario.ciudadano().token()).andExpect(status().isForbidden());
		pedir(post("/traslados/" + traslado + "/asignar"), paramedico.token(),
				Json.objeto("ambulanciaId", paramedico.ambulanciaId()))
				.andExpect(status().isForbidden());
	}

	private static Instant manana() {
		return Instant.now().plus(Duration.ofDays(1)).truncatedTo(ChronoUnit.SECONDS);
	}

	private static Map<String, Object> conCita(Instant cita) {
		return con("horaCita", cita);
	}

	private static Map<String, Object> con(String campo, Object valor) {
		Map<String, Object> pedido = Escenario.pedidoDeTraslado();
		pedido.put(campo, valor);
		return pedido;
	}

	/** Como si hubiera pasado ese tiempo desde que se pidió: todas sus horas se corren hacia atrás juntas. */
	private void pasarElTiempo(long traslado, Duration tiempo) {
		jdbc.update("""
				update traslado set hora_salida_estimada = hora_salida_estimada - make_interval(secs => ?),
				  hora_limite_salida = hora_limite_salida - make_interval(secs => ?),
				  hora_recogida_desde = hora_recogida_desde - make_interval(secs => ?),
				  hora_recogida_hasta = hora_recogida_hasta - make_interval(secs => ?),
				  fecha_hora_creacion = fecha_hora_creacion - make_interval(secs => ?)
				where id = ?
				""", tiempo.toSeconds(), tiempo.toSeconds(), tiempo.toSeconds(), tiempo.toSeconds(),
				tiempo.toSeconds(), traslado);
	}

	/** El día de la agenda en que figura: el de su salida, en la hora de la empresa. */
	private LocalDate diaDeSalida(long traslado) {
		return jdbc.queryForObject("select hora_salida_estimada from traslado where id = ?", Timestamp.class,
				traslado).toInstant().atZone(LA_PAZ).toLocalDate();
	}

	private ResultActions asignar(long traslado, long ambulancia) throws Exception {
		return pedir(post("/traslados/" + traslado + "/asignar"), escenario.admin(),
				Json.objeto("ambulanciaId", ambulancia));
	}

	private String estado(long traslado) {
		return jdbc.queryForObject("select estado from traslado where id = ?", String.class, traslado);
	}

	private void estadoDeLaAmbulancia(long ambulancia, String estado) throws Exception {
		pedir(get("/ambulancias"), escenario.admin())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.id == " + ambulancia + ")].estado", hasItem(estado)));
	}

}
