package com.uem.ambulancias.soporte;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Base64;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.uem.ambulancias.seguridad.service.TokenService;
import com.uem.ambulancias.usuarios.domain.Usuario;
import com.uem.ambulancias.usuarios.repository.UsuarioRepository;

/**
 * Arma los datos de cada prueba por la misma API que usan las apps y el panel, así lo que se prueba parte de un
 * estado real: un paramédico activado con su PIN y en turno, un ciudadano verificado, una alerta. Solo el
 * administrador y los centros de salud se crean directo en la base, porque no hay ninguna API para crearlos.
 */
public class Escenario {

	/** Un PIN que pasa las reglas: no es de los fáciles ni está en ningún teléfono de prueba. */
	public static final String PIN = "583920";

	/** Centro de Santa Cruz de la Sierra. */
	public static final double LATITUD = -17.78329;
	public static final double LONGITUD = -63.18210;

	/** Un grado de latitud son unos 110,6 km: este desplazamiento al norte son unos 100 m. */
	public static final double CIEN_METROS = 0.000904;

	private static final AtomicInteger SECUENCIA = new AtomicInteger(1);

	private final MockMvc mvc;
	private final UsuarioRepository usuarios;
	private final TokenService tokens;
	private final JdbcTemplate jdbc;
	private final AlmacenEnMemoria almacen;
	private String tokenAdmin;

	public Escenario(MockMvc mvc, UsuarioRepository usuarios, TokenService tokens, JdbcTemplate jdbc,
			AlmacenEnMemoria almacen) {
		this.mvc = mvc;
		this.usuarios = usuarios;
		this.tokens = tokens;
		this.jdbc = jdbc;
		this.almacen = almacen;
	}

	/** La sesión del panel de un administrador; se crea la primera vez que se pide. */
	public String admin() {
		if (tokenAdmin == null) {
			int n = SECUENCIA.getAndIncrement();
			Usuario admin = usuarios.save(Usuario.registrarAdmin("Administrador " + n, "6" + String.format("%07d", n),
					"admin" + n + "@prueba.bo", "{noop}no-se-usa"));
			tokenAdmin = tokens.paraPanel(admin);
		}
		return tokenAdmin;
	}

	public long ambulancia() {
		String placa = "PRB" + SECUENCIA.getAndIncrement();
		String respuesta = enviar(post("/ambulancias"), admin(), Json.objeto("placa", placa, "tipoUnidad", "II"), 201);
		return Json.numero(respuesta, "$.id");
	}

	/** Un paramédico registrado por la central, sin activar todavía. */
	public long paramedicoRegistrado(String telefono) {
		String respuesta = enviar(post("/paramedicos"), admin(),
				Json.objeto("nombreCompleto", "Paramédico " + telefono, "telefono", telefono), 201);
		return Json.numero(respuesta, "$.id");
	}

	/**
	 * Genera el código y espera al segundo siguiente, como pasa en la vida real mientras la persona lo escribe. Los
	 * tokens guardan su hora al segundo y el cierre de sesiones que hace el código la guarda con milésimas: activar en
	 * el mismo segundo deja un token que el servidor toma por anterior al cierre (ver el resumen de las pruebas).
	 */
	public String codigoDeActivacion(long paramedicoId) {
		String codigo = Json.texto(enviar(post("/paramedicos/" + paramedicoId + "/activacion"), admin(), null, 201),
				"$.codigo");
		esperarAlSiguienteSegundo();
		return codigo;
	}

	public static void esperarAlSiguienteSegundo() {
		try {
			Thread.sleep(1005 - System.currentTimeMillis() % 1000);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	/** Registrado, con su código usado: ya creó su PIN y tiene el teléfono vinculado. */
	public Paramedico paramedicoActivado() {
		String telefono = nuevoTelefono();
		long id = paramedicoRegistrado(telefono);
		String codigo = codigoDeActivacion(id);
		String respuesta = enviar(post("/auth/paramedico/activacion"), null,
				Json.objeto("telefono", telefono, "codigo", codigo, "pin", PIN), 200);
		return new Paramedico(id, telefono, Json.texto(respuesta, "$.token"),
				Json.texto(respuesta, "$.claveDispositivo"), null);
	}

	/** Activado, asignado a una ambulancia nueva y con el turno abierto: su unidad queda DISPONIBLE. */
	public Paramedico paramedicoEnTurno() {
		Paramedico paramedico = paramedicoActivado();
		long ambulanciaId = ambulancia();
		asignar(paramedico.id(), ambulanciaId);
		iniciarTurno(paramedico);
		return paramedico.conAmbulancia(ambulanciaId);
	}

	public void asignar(long paramedicoId, long ambulanciaId) {
		enviar(post("/asignaciones"), admin(),
				Json.objeto("paramedicoId", paramedicoId, "ambulanciaId", ambulanciaId), 201);
	}

	public void iniciarTurno(Paramedico paramedico) {
		enviar(post("/paramedicos/actual/turno/inicio"), paramedico.token(),
				Json.objeto("pin", PIN, "claveDispositivo", paramedico.claveDispositivo()), 201);
	}

	public void reportarPosicion(Paramedico paramedico, double latitud, double longitud) {
		enviar(post("/posiciones"), paramedico.token(), Json.objeto("latitud", latitud, "longitud", longitud), 204);
	}

	/** El teléfono del paramédico queda registrado para recibir los push, con ese token de FCM. */
	public void recibirAvisos(Paramedico paramedico, String tokenPush) {
		enviar(post("/paramedicos/actual/dispositivo"), paramedico.token(), Json.objeto("tokenPush", tokenPush), 204);
	}

	/** Un ciudadano que verificó su número por SMS y entró por primera vez. */
	public Ciudadano ciudadano() {
		String telefono = "+5916" + String.format("%07d", SECUENCIA.getAndIncrement());
		String respuesta = enviar(post("/auth/ciudadano"), null,
				Json.objeto("idToken", VerificadorDeTelefonoFalso.tokenDe(telefono), "nombreCompleto",
						"Ciudadano " + telefono, "aceptaPrivacidad", true),
				201);
		return new Ciudadano(Json.numero(respuesta, "$.ciudadano.id"), telefono, Json.texto(respuesta, "$.token"));
	}

	/** El teléfono del ciudadano queda registrado para recibir los push de su pedido, con ese token de FCM. */
	public void recibirAvisos(Ciudadano ciudadano, String tokenPush) {
		enviar(post("/ciudadanos/actual/dispositivo"), ciudadano.token(), Json.objeto("tokenPush", tokenPush), 204);
	}

	public Alerta alertar(Ciudadano ciudadano, double latitud, double longitud) {
		String respuesta = enviar(post("/alertas"), ciudadano.token(),
				Json.objeto("latitud", latitud, "longitud", longitud, "origenUbicacion", "GPS"), 201);
		return new Alerta(Json.numero(respuesta, "$.alertaId"), Json.numero(respuesta, "$.incidenteId"));
	}

	public Alerta alertar(Ciudadano ciudadano) {
		return alertar(ciudadano, LATITUD, LONGITUD);
	}

	/** Toma el incidente con la unidad en turno del paramédico. Devuelve el id de la atención. */
	public long tomar(Paramedico paramedico, long incidenteId) {
		return Json.numero(enviar(post("/incidentes/" + incidenteId + "/tomar"), paramedico.token(), null, 201), "$.id");
	}

	/**
	 * Marca un hito de la atención en el lugar del incidente: {@code llegada}, {@code recogida} u {@code hospital}.
	 */
	public void marcar(Paramedico paramedico, long atencionId, String hito) {
		enviar(post("/atenciones/" + atencionId + "/" + hito), paramedico.token(),
				Json.objeto("latitud", LATITUD, "longitud", LONGITUD), 200);
	}

	/** Llega, sube al paciente y llega al hospital: la atención queda lista para la entrega. */
	public void hastaElHospital(Paramedico paramedico, long atencionId) {
		marcar(paramedico, atencionId, "llegada");
		marcar(paramedico, atencionId, "recogida");
		marcar(paramedico, atencionId, "hospital");
	}

	public void liberar(Paramedico paramedico, long atencionId) {
		enviar(post("/atenciones/" + atencionId + "/liberacion"), paramedico.token(), null, 200);
	}

	/**
	 * Un centro de salud del catálogo, cargado directo en la base: no hay API para crearlos y en producción también
	 * se cargan así.
	 */
	public long centroDeSalud(String nombre, boolean activo) {
		return jdbc.queryForObject("""
				insert into centro_salud (nombre, direccion, ubicacion, activo)
				values (?, ?, ST_SetSRID(ST_MakePoint(?, ?), 4326), ?) returning id
				""", Long.class, nombre, "Av. Cañoto " + SECUENCIA.getAndIncrement(), LONGITUD + 0.01, LATITUD, activo);
	}

	/** Teléfonos de 8 dígitos que empiezan con 7, como los de Bolivia, y que nunca contienen el PIN de prueba. */
	/**
	 * El ciudadano anuncia un archivo para su alerta y recibe dónde subirlo. El contenido es inventado: alcanza con
	 * un SHA-256 distinto por archivo.
	 */
	public Evidencia anunciarEvidencia(Ciudadano ciudadano, long alertaId, String mimeType, long tamanoBytes) {
		String sha256 = String.format("%064x", SECUENCIA.getAndIncrement());
		String respuesta = enviar(post("/alertas/" + alertaId + "/evidencias"), ciudadano.token(),
				Json.objeto("mimeType", mimeType, "tamanoBytes", tamanoBytes, "sha256", sha256), 201);
		return new Evidencia(Json.numero(respuesta, "$.evidenciaId"), Json.texto(respuesta, "$.urlSubida"),
				tamanoBytes, sha256);
	}

	/** Sube al almacén el archivo anunciado, tal cual se anunció. */
	public void subir(Evidencia evidencia) {
		almacen.subir(evidencia.urlSubida(), evidencia.tamanoBytes(), evidencia.sha256Base64());
	}

	/** Anuncia, sube y confirma: el archivo queda recibido y esperando su análisis. */
	public Evidencia evidenciaSubida(Ciudadano ciudadano, long alertaId, String mimeType) {
		Evidencia evidencia = anunciarEvidencia(ciudadano, alertaId, mimeType, 250_000);
		subir(evidencia);
		enviar(post("/evidencias/" + evidencia.id() + "/confirmacion"), ciudadano.token(), null, 202);
		return evidencia;
	}

	public static String nuevoTelefono() {
		return "7" + String.format("%07d", SECUENCIA.getAndIncrement());
	}

	private String enviar(MockHttpServletRequestBuilder peticion, String token, String cuerpo, int estadoEsperado) {
		if (token != null) {
			peticion.header("Authorization", "Bearer " + token);
		}
		if (cuerpo != null) {
			peticion.contentType(MediaType.APPLICATION_JSON).content(cuerpo);
		}
		try {
			return Json.utf8(mvc.perform(peticion)
					.andExpect(status().is(estadoEsperado))
					.andReturn()
					.getResponse()
					.getContentAsByteArray());
		} catch (Exception e) {
			throw new IllegalStateException("No se pudo preparar el escenario: " + e.getMessage(), e);
		}
	}

	public record Paramedico(long id, String telefono, String token, String claveDispositivo, Long ambulanciaId) {

		Paramedico conAmbulancia(long ambulanciaId) {
			return new Paramedico(id, telefono, token, claveDispositivo, ambulanciaId);
		}

	}

	public record Ciudadano(long id, String telefono, String token) {
	}

	public record Alerta(long alertaId, long incidenteId) {
	}

	public record Evidencia(long id, String urlSubida, long tamanoBytes, String sha256) {

		/** El mismo SHA-256 en base64, que es como lo devuelve el almacén. */
		public String sha256Base64() {
			return Base64.getEncoder().encodeToString(HexFormat.of().parseHex(sha256));
		}

	}

}
