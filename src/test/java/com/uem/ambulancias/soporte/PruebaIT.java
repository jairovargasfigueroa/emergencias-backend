package com.uem.ambulancias.soporte;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.uem.ambulancias.seguridad.service.TokenService;
import com.uem.ambulancias.usuarios.repository.UsuarioRepository;

/**
 * Base de las pruebas de integración ({@code ...IT}): la app entera, con PostGIS en Docker y Firebase, S3 y la IA
 * reemplazados.
 * Todas comparten el mismo contexto y la misma base, que se vacía antes de cada prueba: ninguna depende de otra.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({ PostgisDePrueba.class, DoblesDePrueba.class })
public abstract class PruebaIT {

	@Autowired
	protected MockMvc mvc;

	@Autowired
	protected PublicacionesAnotadas publicaciones;

	@Autowired
	protected JdbcTemplate jdbc;

	@Autowired
	protected AlmacenEnMemoria almacen;

	@Autowired
	protected ServicioIaFalso ia;

	@Autowired
	private UsuarioRepository usuarios;

	@Autowired
	private TokenService tokens;

	protected Escenario escenario;

	@BeforeEach
	void empezarDeCero() {
		List<String> tablas = jdbc.queryForList(
				"select tablename from pg_tables where schemaname = 'public' and tablename <> 'spatial_ref_sys'",
				String.class);
		if (!tablas.isEmpty()) {
			jdbc.execute("truncate table " + String.join(", ", tablas) + " restart identity cascade");
		}
		publicaciones.olvidar();
		almacen.olvidar();
		ia.olvidar();
		escenario = new Escenario(mvc, usuarios, tokens, jdbc);
	}

	/** Hace la petición con la sesión de ese token (o sin sesión, si es null) y el cuerpo dado (o ninguno). */
	protected ResultActions pedir(MockHttpServletRequestBuilder peticion, String token, String cuerpo) throws Exception {
		if (token != null) {
			peticion.header("Authorization", "Bearer " + token);
		}
		if (cuerpo != null) {
			peticion.contentType(MediaType.APPLICATION_JSON).content(cuerpo);
		}
		return mvc.perform(peticion);
	}

	protected ResultActions pedir(MockHttpServletRequestBuilder peticion, String token) throws Exception {
		return pedir(peticion, token, null);
	}

	/** El cuerpo de la respuesta como texto, para leerlo con {@link Json}. */
	protected static String cuerpo(ResultActions resultado) {
		return Json.utf8(resultado.andReturn().getResponse().getContentAsByteArray());
	}

}
