package com.uem.ambulancias.emergencias;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.uem.ambulancias.emergencias.domain.EstadoAtencion;
import com.uem.ambulancias.emergencias.domain.EstadoIncidente;
import com.uem.ambulancias.emergencias.service.AvisoParaCiudadano;
import com.uem.ambulancias.emergencias.service.SeguimientoPublicado;
import com.uem.ambulancias.flota.service.PosicionActualizada;
import com.uem.ambulancias.soporte.Escenario;
import com.uem.ambulancias.soporte.Escenario.Alerta;
import com.uem.ambulancias.soporte.Escenario.Ciudadano;
import com.uem.ambulancias.soporte.Escenario.Paramedico;
import com.uem.ambulancias.soporte.Json;
import com.uem.ambulancias.soporte.PruebaIT;

/**
 * Lo que ve el ciudadano mientras espera: el seguimiento publicado de su incidente, la posición en vivo de la unidad
 * y los push de los momentos que importan. En las pruebas todo eso queda anotado en vez de ir a Firebase.
 */
@Tag("PB-08")
class SeguimientoIT extends PruebaIT {

	@Test
	@DisplayName("PB-08 · CP-08-01 · apenas pide ayuda ve su pedido recibido, todavía sin unidad")
	void pedidoRecibido() {
		Alerta alerta = escenario.alertar(escenario.ciudadano());

		SeguimientoPublicado seguimiento = publicaciones.ultimoSeguimiento(alerta.incidenteId());
		assertThat(seguimiento.estado()).isEqualTo(EstadoIncidente.ACTIVO);
		assertThat(seguimiento.unidades()).isEmpty();
	}

	@Test
	@DisplayName("PB-08 · CP-08-02 · cuando una unidad lo toma ve cuál es y dónde está, y le llega el push de que va en camino")
	void unidadEnCamino() {
		Ciudadano ciudadano = escenario.ciudadano();
		escenario.recibirAvisos(ciudadano, "push-ciudadano");
		Alerta alerta = escenario.alertar(ciudadano);
		Paramedico paramedico = escenario.paramedicoEnTurno();
		escenario.reportarPosicion(paramedico, Escenario.LATITUD + 0.01, Escenario.LONGITUD);

		escenario.tomar(paramedico, alerta.incidenteId());

		SeguimientoPublicado seguimiento = publicaciones.ultimoSeguimiento(alerta.incidenteId());
		assertThat(seguimiento.estado()).isEqualTo(EstadoIncidente.EN_ATENCION);
		assertThat(seguimiento.unidades()).singleElement().satisfies(unidad -> {
			assertThat(unidad.ambulanciaId()).isEqualTo(paramedico.ambulanciaId());
			assertThat(unidad.placa()).isNotBlank();
			assertThat(unidad.estado()).isEqualTo(EstadoAtencion.EN_CAMINO);
			assertThat(unidad.latitud()).isEqualTo(Escenario.LATITUD + 0.01);
			assertThat(unidad.posicionEn()).isNotNull();
		});
		AvisoParaCiudadano aviso = ultimoAviso("push-ciudadano");
		assertThat(aviso.datos()).containsEntry("tipo", "UNIDAD_EN_CAMINO")
				.containsEntry("incidenteId", String.valueOf(alerta.incidenteId()))
				.containsEntry("alertaId", String.valueOf(alerta.alertaId()));
	}

	@Test
	@DisplayName("PB-08 · CP-08-03 · ve moverse a la unidad solo mientras va a su incidente")
	void posicionEnVivo() throws Exception {
		Alerta alerta = escenario.alertar(escenario.ciudadano());
		Paramedico paramedico = escenario.paramedicoEnTurno();
		escenario.reportarPosicion(paramedico, Escenario.LATITUD + 0.02, Escenario.LONGITUD);
		assertThat(publicaciones.cantidadDePosicionesEnSeguimiento()).isZero();

		long atencion = escenario.tomar(paramedico, alerta.incidenteId());
		escenario.reportarPosicion(paramedico, Escenario.LATITUD + 0.01, Escenario.LONGITUD);

		List<PosicionActualizada> posiciones = publicaciones.posicionesEnSeguimiento(alerta.incidenteId());
		assertThat(posiciones).singleElement().satisfies(posicion -> {
			assertThat(posicion.ambulanciaId()).isEqualTo(paramedico.ambulanciaId());
			assertThat(posicion.latitud()).isEqualTo(Escenario.LATITUD + 0.01);
		});

		cancelarAtencion(paramedico, atencion, "DESVIADA");
		escenario.reportarPosicion(paramedico, Escenario.LATITUD + 0.03, Escenario.LONGITUD);

		assertThat(publicaciones.posicionesEnSeguimiento(alerta.incidenteId())).hasSize(1);
	}

	@Test
	@DisplayName("PB-08 · CP-08-04 · cuando llega la primera unidad lo ve y le llega un solo push, aunque lleguen dos")
	void unidadLlego() throws Exception {
		Ciudadano ciudadano = escenario.ciudadano();
		escenario.recibirAvisos(ciudadano, "push-ciudadano");
		Alerta alerta = escenario.alertar(ciudadano);
		Paramedico primero = escenario.paramedicoEnTurno();
		Paramedico segundo = escenario.paramedicoEnTurno();
		long atencionPrimero = escenario.tomar(primero, alerta.incidenteId());
		long atencionSegundo = Json.numero(cuerpo(pedir(post("/incidentes/" + alerta.incidenteId() + "/sumarse"),
				segundo.token()).andExpect(status().isCreated())), "$.id");

		llegar(primero, atencionPrimero);
		llegar(segundo, atencionSegundo);

		assertThat(publicaciones.ultimoSeguimiento(alerta.incidenteId()).unidades())
				.extracting(SeguimientoPublicado.Unidad::estado)
				.containsOnly(EstadoAtencion.EN_EL_LUGAR);
		assertThat(publicaciones.avisosAlCiudadano("push-ciudadano"))
				.extracting(aviso -> aviso.datos().get("tipo"))
				.containsExactly("UNIDAD_EN_CAMINO", "UNIDAD_LLEGO");
	}

	@Test
	@DisplayName("PB-08 · CP-08-05 · si la unidad no puede llegar, sabe que se busca otra y el incidente vuelve a avisarse a las unidades")
	void buscandoOtraUnidad() throws Exception {
		Ciudadano ciudadano = escenario.ciudadano();
		escenario.recibirAvisos(ciudadano, "push-ciudadano");
		Alerta alerta = escenario.alertar(ciudadano);
		Paramedico paramedico = escenario.paramedicoEnTurno();
		long atencion = escenario.tomar(paramedico, alerta.incidenteId());
		int avisosAUnidades = publicaciones.cantidadDeAvisosDeIncidenteNuevo();

		cancelarAtencion(paramedico, atencion, "AVERIA");

		SeguimientoPublicado seguimiento = publicaciones.ultimoSeguimiento(alerta.incidenteId());
		assertThat(seguimiento.estado()).isEqualTo(EstadoIncidente.ACTIVO);
		assertThat(seguimiento.unidades()).isEmpty();
		assertThat(ultimoAviso("push-ciudadano").datos()).containsEntry("tipo", "BUSCANDO_OTRA_UNIDAD");
		assertThat(publicaciones.cantidadDeAvisosDeIncidenteNuevo()).isEqualTo(avisosAUnidades + 1);
	}

	@Test
	@DisplayName("PB-08 · CP-08-06 · quien retiró su pedido deja de recibir avisos; los demás que pidieron, no")
	void sinAvisosTrasRetirar() throws Exception {
		Ciudadano retira = escenario.ciudadano();
		Ciudadano espera = escenario.ciudadano();
		escenario.recibirAvisos(retira, "push-retira");
		escenario.recibirAvisos(espera, "push-espera");
		Alerta alerta = escenario.alertar(retira);
		escenario.alertar(espera);
		pedir(post("/alertas/" + alerta.alertaId() + "/cancelacion"), retira.token(), Json.objeto("motivo", "ERROR"))
				.andExpect(status().isOk());

		escenario.tomar(escenario.paramedicoEnTurno(), alerta.incidenteId());

		assertThat(publicaciones.avisosAlCiudadano("push-retira")).isEmpty();
		assertThat(ultimoAviso("push-espera").datos()).containsEntry("tipo", "UNIDAD_EN_CAMINO");
	}

	@Test
	@DisplayName("PB-08 · CP-08-07 · cuando el incidente se cierra, el seguimiento muestra el cierre y ya ninguna unidad")
	void incidenteCerrado() throws Exception {
		Ciudadano ciudadano = escenario.ciudadano();
		Alerta alerta = escenario.alertar(ciudadano);

		pedir(post("/alertas/" + alerta.alertaId() + "/cancelacion"), ciudadano.token(),
				Json.objeto("motivo", "YA_FUE_ATENDIDO"))
				.andExpect(status().isOk());

		SeguimientoPublicado seguimiento = publicaciones.ultimoSeguimiento(alerta.incidenteId());
		assertThat(seguimiento.estado()).isEqualTo(EstadoIncidente.CANCELADO);
		assertThat(seguimiento.unidades()).isEmpty();
	}

	private void llegar(Paramedico paramedico, long atencion) throws Exception {
		pedir(post("/atenciones/" + atencion + "/llegada"), paramedico.token(),
				Json.objeto("latitud", Escenario.LATITUD, "longitud", Escenario.LONGITUD))
				.andExpect(status().isOk());
	}

	private void cancelarAtencion(Paramedico paramedico, long atencion, String motivo) throws Exception {
		pedir(post("/atenciones/" + atencion + "/cancelar"), paramedico.token(), Json.objeto("motivo", motivo))
				.andExpect(status().isOk());
	}

	private AvisoParaCiudadano ultimoAviso(String tokenPush) {
		List<AvisoParaCiudadano> avisos = publicaciones.avisosAlCiudadano(tokenPush);
		assertThat(avisos).as("push al teléfono %s", tokenPush).isNotEmpty();
		return avisos.getLast();
	}

}
