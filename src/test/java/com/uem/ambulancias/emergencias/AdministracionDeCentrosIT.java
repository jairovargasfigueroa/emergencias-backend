package com.uem.ambulancias.emergencias;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.uem.ambulancias.soporte.Escenario;
import com.uem.ambulancias.soporte.Escenario.Paramedico;
import com.uem.ambulancias.soporte.Json;
import com.uem.ambulancias.soporte.PruebaIT;

@Tag("PB-24")
class AdministracionDeCentrosIT extends PruebaIT {

	private static final double LATITUD = -17.7833;
	private static final double LONGITUD = -63.1821;

	private String centro(String nombre, String direccion) {
		return Json.objeto("nombre", nombre, "direccion", direccion, "latitud", LATITUD, "longitud", LONGITUD);
	}

	@Test
	@DisplayName("PB-24 · CP-24-01 · la central registra un centro y ya se ofrece como destino")
	void registra() throws Exception {
		String admin = escenario.admin();

		pedir(post("/centros-salud"), admin, centro("  Hospital de Niños  ", "  Av. Irala  "))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.nombre").value("Hospital de Niños"))
				.andExpect(jsonPath("$.direccion").value("Av. Irala"))
				.andExpect(jsonPath("$.latitud").value(LATITUD))
				.andExpect(jsonPath("$.longitud").value(LONGITUD))
				.andExpect(jsonPath("$.activo").value(true));

		pedir(get("/centros-salud"), escenario.paramedicoActivado().token())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].nombre").value("Hospital de Niños"));
	}

	@Test
	@DisplayName("PB-24 · CP-24-01 · sin nombre o con coordenadas fuera de rango no se registra")
	void validaLosDatos() throws Exception {
		String admin = escenario.admin();

		pedir(post("/centros-salud"), admin, centro("   ", null))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value("VALIDACION"));
		pedir(post("/centros-salud"), admin,
				Json.objeto("nombre", "Posta", "latitud", -95, "longitud", LONGITUD))
				.andExpect(status().isBadRequest());
		pedir(post("/centros-salud"), admin, Json.objeto("nombre", "Posta", "latitud", LATITUD))
				.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("PB-24 · CP-24-02 · la central corrige el nombre, la dirección y la ubicación")
	void corrige() throws Exception {
		long id = escenario.centroDeSalud("Hospital Japones", true);

		pedir(put("/centros-salud/" + id), escenario.admin(), Json.objeto("nombre", "Hospital Japonés",
				"direccion", "", "latitud", -17.75, "longitud", -63.16))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nombre").value("Hospital Japonés"))
				.andExpect(jsonPath("$.direccion").doesNotExist())
				.andExpect(jsonPath("$.latitud").value(-17.75))
				.andExpect(jsonPath("$.longitud").value(-63.16));

		pedir(put("/centros-salud/999999"), escenario.admin(), centro("Otro", null))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"));
	}

	@Test
	@DisplayName("PB-24 · CP-24-03 · un centro dado de baja deja de ofrecerse, pero la central lo sigue viendo")
	void daDeBaja() throws Exception {
		String admin = escenario.admin();
		long cerrado = escenario.centroDeSalud("Posta del Plan 3000", true);
		escenario.centroDeSalud("Hospital Japonés", true);

		pedir(post("/centros-salud/" + cerrado + "/desactivar"), admin)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.activo").value(false));

		pedir(get("/centros-salud"), escenario.ciudadano().token())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].nombre").value("Hospital Japonés"));
		pedir(get("/centros-salud/todos"), admin)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].nombre").value("Hospital Japonés"))
				.andExpect(jsonPath("$[1].nombre").value("Posta del Plan 3000"))
				.andExpect(jsonPath("$[1].activo").value(false));
	}

	@Test
	@DisplayName("PB-24 · CP-24-03 · un centro dado de baja ya no se puede elegir para entregar")
	void dadoDeBajaNoSeElige() throws Exception {
		long centro = escenario.centroDeSalud("Posta del Plan 3000", true);
		pedir(post("/centros-salud/" + centro + "/desactivar"), escenario.admin()).andExpect(status().isOk());
		Paramedico paramedico = escenario.paramedicoEnTurno();
		long incidente = escenario.alertar(escenario.ciudadano()).incidenteId();
		long atencion = escenario.tomar(paramedico, incidente);
		escenario.hastaElHospital(paramedico, atencion);

		pedir(post("/atenciones/" + atencion + "/entrega"), paramedico.token(), Json.objeto(
				"latitud", Escenario.LATITUD, "longitud", Escenario.LONGITUD, "centroSaludId", centro))
				.andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("PB-24 · CP-24-04 · la baja no borra el historial: la atención sigue apuntando al centro")
	void bajaConservaElHistorial() throws Exception {
		String admin = escenario.admin();
		long centro = escenario.centroDeSalud("Hospital Japonés", true);
		Paramedico paramedico = escenario.paramedicoEnTurno();
		long incidente = escenario.alertar(escenario.ciudadano()).incidenteId();
		long atencion = escenario.tomar(paramedico, incidente);
		escenario.hastaElHospital(paramedico, atencion);
		pedir(post("/atenciones/" + atencion + "/entrega"), paramedico.token(), Json.objeto(
				"latitud", Escenario.LATITUD, "longitud", Escenario.LONGITUD, "centroSaludId", centro))
				.andExpect(status().isOk());

		pedir(post("/centros-salud/" + centro + "/desactivar"), admin).andExpect(status().isOk());

		pedir(get("/incidentes/" + incidente), admin)
				.andExpect(jsonPath("$.atenciones[0].centroSalud.id").value(centro))
				.andExpect(jsonPath("$.atenciones[0].centroSalud.nombre").value("Hospital Japonés"));
	}

	@Test
	@DisplayName("PB-24 · CP-24-05 · un centro dado de baja se vuelve a ofrecer al reactivarlo")
	void reactiva() throws Exception {
		String admin = escenario.admin();
		long centro = escenario.centroDeSalud("Posta del Plan 3000", false);

		pedir(post("/centros-salud/" + centro + "/activar"), admin)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.activo").value(true));
		pedir(get("/centros-salud"), escenario.ciudadano().token())
				.andExpect(jsonPath("$.length()").value(1));
	}

	@Test
	@DisplayName("PB-24 · CP-24-06 · solo la central administra el catálogo")
	void soloLaCentral() throws Exception {
		long centro = escenario.centroDeSalud("Hospital Japonés", true);
		String ciudadano = escenario.ciudadano().token();
		String paramedico = escenario.paramedicoActivado().token();

		pedir(post("/centros-salud"), ciudadano, centro("Hospital de Niños", null)).andExpect(status().isForbidden());
		pedir(put("/centros-salud/" + centro), paramedico, centro("Otro nombre", null))
				.andExpect(status().isForbidden());
		pedir(post("/centros-salud/" + centro + "/desactivar"), paramedico).andExpect(status().isForbidden());
		pedir(get("/centros-salud/todos"), ciudadano).andExpect(status().isForbidden());
		pedir(post("/centros-salud"), null, centro("Hospital de Niños", null)).andExpect(status().isUnauthorized());
	}
}
