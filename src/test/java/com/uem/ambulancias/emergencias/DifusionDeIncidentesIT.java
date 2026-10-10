package com.uem.ambulancias.emergencias;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.uem.ambulancias.emergencias.domain.EstadoIncidente;
import com.uem.ambulancias.emergencias.service.IncidentePublicado;
import com.uem.ambulancias.soporte.Escenario;
import com.uem.ambulancias.soporte.Escenario.Alerta;
import com.uem.ambulancias.soporte.Escenario.Ciudadano;
import com.uem.ambulancias.soporte.Escenario.Paramedico;
import com.uem.ambulancias.soporte.Json;
import com.uem.ambulancias.soporte.PruebaIT;

/**
 * Lo que ve el paramédico llega publicado en tiempo real (el nodo de incidentes abiertos) y por push. En las pruebas
 * esas publicaciones quedan anotadas en vez de ir a Firebase.
 */
@Tag("PB-06")
class DifusionDeIncidentesIT extends PruebaIT {

	@Test
	@DisplayName("PB-06 · CP-06-01 · el incidente nuevo se publica con su ubicación, afectados y lo que contó quien avisó")
	void publicaElIncidente() throws Exception {
		String respuesta = cuerpo(pedir(post("/alertas"), escenario.ciudadano().token(),
				Json.objeto("latitud", Escenario.LATITUD, "longitud", Escenario.LONGITUD, "origenUbicacion", "GPS",
						"cantidadAfectados", 2, "descripcion", "Señora desmayada en la parada del micro"))
				.andExpect(status().isCreated()));

		IncidentePublicado publicado = ultimoPublicado(Json.numero(respuesta, "$.incidenteId"));
		assertThat(publicado.estado()).isEqualTo(EstadoIncidente.ACTIVO);
		assertThat(publicado.latitud()).isEqualTo(Escenario.LATITUD);
		assertThat(publicado.longitud()).isEqualTo(Escenario.LONGITUD);
		assertThat(publicado.cantidadAfectados()).isEqualTo(2);
		assertThat(publicado.descripciones()).containsExactly("Señora desmayada en la parada del micro");
		assertThat(publicado.unidadesAcudiendo()).isZero();
	}

	@Test
	@DisplayName("PB-06 · CP-06-02 · el push llega a quienes están de turno en una unidad libre, del más cercano al más lejano")
	void avisoPorCercania() {
		enTurnoEn(Escenario.LATITUD + 0.03, "push-lejos");
		escenario.recibirAvisos(escenario.paramedicoEnTurno(), "push-sin-posicion");
		enTurnoEn(Escenario.LATITUD + 3 * Escenario.CIEN_METROS, "push-cerca");
		enTurnoEn(Escenario.LATITUD + 0.009, "push-medio");

		Paramedico fueraDeTurno = escenario.paramedicoActivado();
		escenario.asignar(fueraDeTurno.id(), escenario.ambulancia());
		escenario.recibirAvisos(fueraDeTurno, "push-fuera-de-turno");
		Paramedico ocupado = enTurnoEn(Escenario.LATITUD + Escenario.CIEN_METROS, "push-ocupado");
		escenario.tomar(ocupado, escenario.alertar(escenario.ciudadano(), Escenario.LATITUD + 0.2,
				Escenario.LONGITUD).incidenteId());
		enTurnoEn(Escenario.LATITUD, null);

		escenario.alertar(escenario.ciudadano());

		assertThat(publicaciones.ultimoAvisoDeIncidenteNuevo())
				.containsExactly("push-cerca", "push-medio", "push-lejos", "push-sin-posicion");
	}

	@Test
	@DisplayName("PB-06 · CP-06-03 · otra alerta del mismo incidente actualiza lo publicado sin volver a mandar el push")
	void otraAlertaActualiza() throws Exception {
		enTurnoEn(Escenario.LATITUD, "push-unidad");
		Alerta primera = escenario.alertar(escenario.ciudadano());
		int avisos = publicaciones.cantidadDeAvisosDeIncidenteNuevo();

		pedir(post("/alertas"), escenario.ciudadano().token(), Json.objeto("latitud", Escenario.LATITUD,
				"longitud", Escenario.LONGITUD, "origenUbicacion", "GPS", "cantidadAfectados", 3, "descripcion",
				"Son tres, uno sangra"))
				.andExpect(status().isCreated());

		IncidentePublicado publicado = ultimoPublicado(primera.incidenteId());
		assertThat(publicado.cantidadAfectados()).isEqualTo(3);
		assertThat(publicado.descripciones()).containsExactly("Son tres, uno sangra");
		assertThat(publicaciones.cantidadDeAvisosDeIncidenteNuevo()).isEqualTo(avisos);
	}

	@Test
	@DisplayName("PB-06 · CP-06-04 · tomado, sigue a la vista de las demás unidades con cuántas acuden, para poder sumarse")
	void tomadoSigueALaVista() {
		Alerta alerta = escenario.alertar(escenario.ciudadano());
		int avisos = publicaciones.cantidadDeAvisosDeIncidenteNuevo();

		escenario.tomar(escenario.paramedicoEnTurno(), alerta.incidenteId());

		IncidentePublicado publicado = ultimoPublicado(alerta.incidenteId());
		assertThat(publicado.estado()).isEqualTo(EstadoIncidente.EN_ATENCION);
		assertThat(publicado.unidadesAcudiendo()).isEqualTo(1);
		assertThat(publicaciones.fueRetirado(alerta.incidenteId())).isFalse();
		assertThat(publicaciones.cantidadDeAvisosDeIncidenteNuevo()).isEqualTo(avisos);
	}

	@Test
	@DisplayName("PB-06 · CP-06-05 · cuando el incidente se cierra, desaparece de la lista de las unidades")
	void cerradoSeRetira() throws Exception {
		Ciudadano ciudadano = escenario.ciudadano();
		Alerta alerta = escenario.alertar(ciudadano);
		assertThat(publicaciones.fueRetirado(alerta.incidenteId())).isFalse();

		pedir(post("/alertas/" + alerta.alertaId() + "/cancelacion"), ciudadano.token(),
				Json.objeto("motivo", "FALSA_ALARMA"))
				.andExpect(status().isOk());

		assertThat(publicaciones.fueRetirado(alerta.incidenteId())).isTrue();
	}

	@Test
	@DisplayName("PB-06 · CP-06-06 · sin ninguna unidad libre, el incidente se publica igual y queda esperando")
	void sinUnidades() {
		Alerta alerta = escenario.alertar(escenario.ciudadano());

		assertThat(ultimoPublicado(alerta.incidenteId()).estado()).isEqualTo(EstadoIncidente.ACTIVO);
		assertThat(publicaciones.ultimoAvisoDeIncidenteNuevo()).isEmpty();
	}

	@Test
	@DisplayName("PB-06 · CP-06-07 · la central ve los incidentes abiertos, del más reciente al más antiguo")
	void listaDeLaCentral() throws Exception {
		Ciudadano ciudadano = escenario.ciudadano();
		Alerta cerrado = escenario.alertar(ciudadano, Escenario.LATITUD + 0.05, Escenario.LONGITUD);
		pedir(post("/alertas/" + cerrado.alertaId() + "/cancelacion"), ciudadano.token(),
				Json.objeto("motivo", "ERROR"))
				.andExpect(status().isOk());
		Alerta primero = escenario.alertar(escenario.ciudadano(), Escenario.LATITUD + 0.1, Escenario.LONGITUD);
		Alerta segundo = escenario.alertar(escenario.ciudadano());

		pedir(get("/incidentes").param("estado", "ABIERTOS"), escenario.admin())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElementos").value(2))
				.andExpect(jsonPath("$.contenido[0].id").value(segundo.incidenteId()))
				.andExpect(jsonPath("$.contenido[1].id").value(primero.incidenteId()));
		pedir(get("/incidentes").param("estado", "CERRADOS"), escenario.admin())
				.andExpect(jsonPath("$.totalElementos").value(1))
				.andExpect(jsonPath("$.contenido[0].estado").value("CANCELADO"));
		pedir(get("/incidentes"), escenario.paramedicoActivado().token()).andExpect(status().isForbidden());
	}

	/** En turno, con su unidad en ese punto y, si se da un token, con el teléfono registrado para los push. */
	private Paramedico enTurnoEn(double latitud, String tokenPush) {
		Paramedico paramedico = escenario.paramedicoEnTurno();
		escenario.reportarPosicion(paramedico, latitud, Escenario.LONGITUD);
		if (tokenPush != null) {
			escenario.recibirAvisos(paramedico, tokenPush);
		}
		return paramedico;
	}

	private IncidentePublicado ultimoPublicado(long incidenteId) {
		List<IncidentePublicado> publicados = publicaciones.incidentesAbiertos(incidenteId);
		assertThat(publicados).as("publicaciones del incidente %d", incidenteId).isNotEmpty();
		return publicados.getLast();
	}

}
