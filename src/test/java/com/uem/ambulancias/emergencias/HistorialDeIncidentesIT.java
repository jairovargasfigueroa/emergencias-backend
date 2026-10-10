package com.uem.ambulancias.emergencias;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

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

@Tag("PB-21")
class HistorialDeIncidentesIT extends PruebaIT {

	@Test
	@DisplayName("PB-21 · CP-21-01 · se filtran los abiertos, los finalizados o todos, del más reciente al más antiguo")
	void filtros() throws Exception {
		long activo = incidenteEn(0.01);
		long enAtencion = incidenteEn(0.02);
		escenario.tomar(escenario.paramedicoEnTurno(), enAtencion);
		long cancelado = incidenteRetirado(0.03);
		long falsaAlarma = incidenteEn(0.04);
		pedir(post("/incidentes/" + falsaAlarma + "/cierre"), escenario.admin(),
				Json.objeto("motivo", "FALSA_ALARMA_VERIFICADA"))
				.andExpect(status().isNoContent());

		listar("ABIERTOS")
				.andExpect(jsonPath("$.totalElementos").value(2))
				.andExpect(jsonPath("$.contenido[*].id", containsInAnyOrder((int) activo, (int) enAtencion)));
		listar("CERRADOS")
				.andExpect(jsonPath("$.totalElementos").value(2))
				.andExpect(jsonPath("$.contenido[*].estado", containsInAnyOrder("CANCELADO", "FALSA_ALARMA")));
		pedir(get("/incidentes"), escenario.admin())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElementos").value(4))
				.andExpect(jsonPath("$.contenido[0].id").value(falsaAlarma))
				.andExpect(jsonPath("$.contenido[1].id").value(cancelado))
				.andExpect(jsonPath("$.contenido[3].id").value(activo));
	}

	@Test
	@DisplayName("PB-21 · CP-21-01 · el resumen de cada incidente trae cuántos avisaron, qué unidades fueron y cuántas siguen")
	void resumenDeCadaUno() throws Exception {
		Alerta alerta = escenario.alertar(escenario.ciudadano());
		escenario.alertar(escenario.ciudadano());
		Paramedico primera = escenario.paramedicoEnTurno();
		Paramedico segunda = escenario.paramedicoEnTurno();
		long atencion = escenario.tomar(primera, alerta.incidenteId());
		pedir(post("/atenciones/" + atencion + "/cancelar"), primera.token(), Json.objeto("motivo", "DESVIADA"))
				.andExpect(status().isOk());
		escenario.tomar(segunda, alerta.incidenteId());

		listar("TODOS")
				.andExpect(jsonPath("$.contenido[0].cantidadAlertas").value(2))
				.andExpect(jsonPath("$.contenido[0].unidades.length()").value(2))
				.andExpect(jsonPath("$.contenido[0].unidadesAcudiendo").value(1))
				.andExpect(jsonPath("$.contenido[0].latitud").value(Escenario.LATITUD));
	}

	@Test
	@DisplayName("PB-21 · CP-21-01 · se recorre por páginas; el tamaño tiene tope y una página negativa es la primera")
	void paginas() throws Exception {
		for (int i = 1; i <= 5; i++) {
			incidenteEn(i * 0.01);
		}

		pedir(get("/incidentes").param("tamano", "2"), escenario.admin())
				.andExpect(jsonPath("$.contenido.length()").value(2))
				.andExpect(jsonPath("$.pagina").value(0))
				.andExpect(jsonPath("$.totalElementos").value(5))
				.andExpect(jsonPath("$.totalPaginas").value(3));
		pedir(get("/incidentes").param("tamano", "2").param("pagina", "2"), escenario.admin())
				.andExpect(jsonPath("$.contenido.length()").value(1));
		pedir(get("/incidentes").param("tamano", "500"), escenario.admin())
				.andExpect(jsonPath("$.tamano").value(100));
		pedir(get("/incidentes").param("pagina", "-3"), escenario.admin())
				.andExpect(jsonPath("$.pagina").value(0));
		pedir(get("/incidentes").param("estado", "PERDIDOS"), escenario.admin()).andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("PB-21 · CP-21-02 · el detalle trae las alertas en orden de emisión y las atenciones en orden de toma, con quién y cuándo")
	void detalle() throws Exception {
		Ciudadano primero = escenario.ciudadano();
		Ciudadano segundo = escenario.ciudadano();
		Alerta alerta = escenario.alertar(primero);
		escenario.alertar(segundo);
		Paramedico unidadUno = escenario.paramedicoEnTurno();
		Paramedico unidadDos = escenario.paramedicoEnTurno();
		long atencionUno = escenario.tomar(unidadUno, alerta.incidenteId());
		escenario.marcar(unidadUno, atencionUno, "llegada");
		pedir(post("/incidentes/" + alerta.incidenteId() + "/sumarse"), unidadDos.token())
				.andExpect(status().isCreated());

		String respuesta = cuerpo(pedir(get("/incidentes/" + alerta.incidenteId()), escenario.admin())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.alertas[0].emisor.id").value(primero.id()))
				.andExpect(jsonPath("$.alertas[1].emisor.id").value(segundo.id()))
				.andExpect(jsonPath("$.alertas[0].emisor.telefono").isNotEmpty())
				.andExpect(jsonPath("$.atenciones[0].ambulanciaId").value(unidadUno.ambulanciaId()))
				.andExpect(jsonPath("$.atenciones[0].paramedicoResponsable.id").value(unidadUno.id()))
				.andExpect(jsonPath("$.atenciones[0].paramedicoResponsable.telefono").value(unidadUno.telefono()))
				.andExpect(jsonPath("$.atenciones[0].estado").value("EN_EL_LUGAR"))
				.andExpect(jsonPath("$.atenciones[0].horaLlegada").isNotEmpty())
				.andExpect(jsonPath("$.atenciones[1].ambulanciaId").value(unidadDos.ambulanciaId()))
				.andExpect(jsonPath("$.atenciones[1].estado").value("EN_CAMINO"))
				.andExpect(jsonPath("$.atenciones[1].horaLlegada").doesNotExist()));
		Instant primeraAlerta = Instant.parse(Json.texto(respuesta, "$.alertas[0].fechaHora"));
		Instant segundaAlerta = Instant.parse(Json.texto(respuesta, "$.alertas[1].fechaHora"));
		Instant primeraToma = Instant.parse(Json.texto(respuesta, "$.atenciones[0].horaToma"));
		Instant segundaToma = Instant.parse(Json.texto(respuesta, "$.atenciones[1].horaToma"));
		assertThat(primeraAlerta).isBeforeOrEqualTo(segundaAlerta);
		assertThat(primeraToma).isBeforeOrEqualTo(segundaToma);
	}

	@Test
	@DisplayName("PB-21 · CP-21-03 · un cierre manual muestra motivo, hora y quién lo cerró; uno que se cerró solo, no tiene responsable")
	void cierres() throws Exception {
		long manual = incidenteEn(0.01);
		pedir(post("/incidentes/" + manual + "/cierre"), escenario.admin(),
				Json.objeto("motivo", "ATENDIDO_EXTERNAMENTE"))
				.andExpect(status().isNoContent());
		long solo = incidenteRetirado(0.02);

		pedir(get("/incidentes/" + manual), escenario.admin())
				.andExpect(jsonPath("$.motivoCierre").value("ATENDIDO_EXTERNAMENTE"))
				.andExpect(jsonPath("$.fechaHoraCierre").isNotEmpty())
				.andExpect(jsonPath("$.cerradoPor").value(startsWith("Administrador")));
		pedir(get("/incidentes/" + solo), escenario.admin())
				.andExpect(jsonPath("$.estado").value("CANCELADO"))
				.andExpect(jsonPath("$.fechaHoraCierre").isNotEmpty())
				.andExpect(jsonPath("$.cerradoPor").doesNotExist());
	}

	@Test
	@DisplayName("PB-21 · CP-21-04 · consultar no cambia nada, y el historial no se puede editar ni borrar")
	void soloLectura() throws Exception {
		Alerta alerta = escenario.alertar(escenario.ciudadano());
		escenario.tomar(escenario.paramedicoEnTurno(), alerta.incidenteId());
		String detalle = cuerpo(pedir(get("/incidentes/" + alerta.incidenteId()), escenario.admin()));
		String lista = cuerpo(listar("TODOS"));
		int publicadas = publicaciones.incidentesAbiertos(alerta.incidenteId()).size();

		for (int i = 0; i < 3; i++) {
			pedir(get("/incidentes/" + alerta.incidenteId()), escenario.admin()).andExpect(status().isOk());
			listar("TODOS");
		}

		assertThat(cuerpo(pedir(get("/incidentes/" + alerta.incidenteId()), escenario.admin()))).isEqualTo(detalle);
		assertThat(cuerpo(listar("TODOS"))).isEqualTo(lista);
		assertThat(publicaciones.incidentesAbiertos(alerta.incidenteId())).hasSize(publicadas);
		pedir(put("/incidentes/" + alerta.incidenteId()), escenario.admin(), Json.objeto("estado", "ATENDIDO"))
				.andExpect(status().isMethodNotAllowed());
		pedir(delete("/incidentes/" + alerta.incidenteId()), escenario.admin())
				.andExpect(status().isMethodNotAllowed());
	}

	@Test
	@DisplayName("PB-21 · CP-21-04 · el historial es de la central: ni el paramédico ni el ciudadano lo consultan")
	void soloLaCentral() throws Exception {
		long incidente = incidenteEn(0.01);

		pedir(get("/incidentes"), escenario.paramedicoActivado().token()).andExpect(status().isForbidden());
		pedir(get("/incidentes/" + incidente), escenario.ciudadano().token()).andExpect(status().isForbidden());
		pedir(get("/incidentes/" + incidente), null).andExpect(status().isUnauthorized());
	}

	/** Un incidente nuevo, al norte del centro: cada desplazamiento distinto queda fuera del radio de los demás. */
	private long incidenteEn(double alNorte) {
		return escenario.alertar(escenario.ciudadano(), Escenario.LATITUD + alNorte, Escenario.LONGITUD).incidenteId();
	}

	/** Un incidente que se cerró solo porque quien pidió retiró su pedido. */
	private long incidenteRetirado(double alNorte) throws Exception {
		Ciudadano ciudadano = escenario.ciudadano();
		Alerta alerta = escenario.alertar(ciudadano, Escenario.LATITUD + alNorte, Escenario.LONGITUD);
		pedir(post("/alertas/" + alerta.alertaId() + "/cancelacion"), ciudadano.token(),
				Json.objeto("motivo", "ERROR"))
				.andExpect(status().isOk());
		return alerta.incidenteId();
	}

	private ResultActions listar(String estado) throws Exception {
		return pedir(get("/incidentes").param("estado", estado), escenario.admin()).andExpect(status().isOk());
	}

}
