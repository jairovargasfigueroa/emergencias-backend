package com.uem.ambulancias.emergencias;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import com.uem.ambulancias.soporte.Escenario;
import com.uem.ambulancias.soporte.Escenario.Paramedico;
import com.uem.ambulancias.soporte.Json;
import com.uem.ambulancias.soporte.PruebaIT;

@Tag("PB-09")
class CentroSaludIT extends PruebaIT {

	@Test
	@DisplayName("PB-09 · CP-09-01 · el catálogo muestra solo los centros activos, por nombre y con su ubicación")
	void catalogoActivo() throws Exception {
		escenario.centroDeSalud("Hospital Japonés", true);
		escenario.centroDeSalud("Clínica Foianini", true);
		escenario.centroDeSalud("Posta cerrada del Plan 3000", false);

		pedir(get("/centros-salud"), escenario.paramedicoActivado().token())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].nombre").value("Clínica Foianini"))
				.andExpect(jsonPath("$[1].nombre").value("Hospital Japonés"))
				.andExpect(jsonPath("$[0].latitud").value(Escenario.LATITUD))
				.andExpect(jsonPath("$[0].longitud").value(Escenario.LONGITUD + 0.01))
				.andExpect(jsonPath("$[0].direccion").isNotEmpty());
	}

	@Test
	@DisplayName("PB-09 · CP-09-01 · lo consultan el paramédico, el ciudadano y la central; sin sesión, nadie")
	void quienConsulta() throws Exception {
		escenario.centroDeSalud("Hospital Japonés", true);

		pedir(get("/centros-salud"), escenario.ciudadano().token()).andExpect(status().isOk());
		pedir(get("/centros-salud"), escenario.admin()).andExpect(status().isOk());
		pedir(get("/centros-salud"), null).andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("PB-09 · CP-09-02 · la entrega queda vinculada al centro del catálogo que se eligió")
	void entregaEnUnCentro() throws Exception {
		long centro = escenario.centroDeSalud("Hospital Japonés", true);
		Paramedico paramedico = escenario.paramedicoEnTurno();
		long incidente = escenario.alertar(escenario.ciudadano()).incidenteId();
		long atencion = escenario.tomar(paramedico, incidente);
		escenario.hastaElHospital(paramedico, atencion);

		entregar(paramedico, atencion, Json.objeto("latitud", Escenario.LATITUD, "longitud", Escenario.LONGITUD,
				"centroSaludId", centro))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("PACIENTE_ENTREGADO"))
				.andExpect(jsonPath("$.centroSaludId").value(centro));

		pedir(get("/incidentes/" + incidente), escenario.admin())
				.andExpect(jsonPath("$.atenciones[0].centroSalud.id").value(centro))
				.andExpect(jsonPath("$.atenciones[0].centroSalud.nombre").value("Hospital Japonés"));
	}

	@Test
	@DisplayName("PB-09 · CP-09-03 · un destino que no está en el catálogo se registra con una descripción libre")
	void destinoLibre() throws Exception {
		Paramedico paramedico = escenario.paramedicoEnTurno();
		long incidente = escenario.alertar(escenario.ciudadano()).incidenteId();
		long atencion = escenario.tomar(paramedico, incidente);
		escenario.hastaElHospital(paramedico, atencion);

		entregar(paramedico, atencion, Json.objeto("latitud", Escenario.LATITUD, "longitud", Escenario.LONGITUD,
				"destinoDescripcion", "  Consultorio particular del Dr. Suárez, calle Junín  "))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.centroSaludId").doesNotExist())
				.andExpect(jsonPath("$.destinoDescripcion").value("Consultorio particular del Dr. Suárez, calle Junín"));

		pedir(get("/incidentes/" + incidente), escenario.admin())
				.andExpect(jsonPath("$.atenciones[0].centroSalud").doesNotExist())
				.andExpect(jsonPath("$.atenciones[0].destinoDescripcion")
						.value("Consultorio particular del Dr. Suárez, calle Junín"));
	}

	@Test
	@DisplayName("PB-09 · CP-09-03 · el catálogo nunca bloquea la entrega: sin centro ni descripción se entrega igual")
	void entregaSinDestino() throws Exception {
		Paramedico paramedico = escenario.paramedicoEnTurno();
		long atencion = escenario.tomar(paramedico, escenario.alertar(escenario.ciudadano()).incidenteId());
		escenario.hastaElHospital(paramedico, atencion);

		entregar(paramedico, atencion, Json.objeto("latitud", Escenario.LATITUD, "longitud", Escenario.LONGITUD))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("PACIENTE_ENTREGADO"));
	}

	@Test
	@DisplayName("PB-09 · CP-09-04 · un centro dado de baja o que no existe no se puede elegir, y la entrega no avanza")
	void centroNoDisponible() throws Exception {
		long deBaja = escenario.centroDeSalud("Posta cerrada del Plan 3000", false);
		Paramedico paramedico = escenario.paramedicoEnTurno();
		long atencion = escenario.tomar(paramedico, escenario.alertar(escenario.ciudadano()).incidenteId());
		escenario.hastaElHospital(paramedico, atencion);

		entregar(paramedico, atencion, Json.objeto("latitud", Escenario.LATITUD, "longitud", Escenario.LONGITUD,
				"centroSaludId", deBaja))
				.andExpect(status().isNotFound());
		entregar(paramedico, atencion, Json.objeto("latitud", Escenario.LATITUD, "longitud", Escenario.LONGITUD,
				"centroSaludId", 999_999))
				.andExpect(status().isNotFound());
		pedir(get("/paramedicos/actual/atencion"), paramedico.token())
				.andExpect(jsonPath("$.estado").value("EN_HOSPITAL"));
	}

	private ResultActions entregar(Paramedico paramedico, long atencion, String cuerpo) throws Exception {
		return pedir(post("/atenciones/" + atencion + "/entrega"), paramedico.token(), cuerpo);
	}

}
