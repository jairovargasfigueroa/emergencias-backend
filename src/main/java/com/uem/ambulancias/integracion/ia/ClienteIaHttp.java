package com.uem.ambulancias.integracion.ia;

import java.net.http.HttpClient;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;

import com.uem.ambulancias.evidencias.service.AnalisisRecibido;
import com.uem.ambulancias.evidencias.service.FalloDelServicioIa;
import com.uem.ambulancias.evidencias.service.FalloDelServicioIa.Desenlace;
import com.uem.ambulancias.evidencias.service.PedidoDeAnalisis;
import com.uem.ambulancias.evidencias.service.PedidoDeResumen;
import com.uem.ambulancias.evidencias.service.Procedencia;
import com.uem.ambulancias.evidencias.service.ResumenRecibido;
import com.uem.ambulancias.evidencias.service.ServicioDeAnalisis;
import com.uem.ambulancias.integracion.ia.ContratoIa.AnalysisRequest;
import com.uem.ambulancias.integracion.ia.ContratoIa.AnalysisResponse;
import com.uem.ambulancias.integracion.ia.ContratoIa.ErrorResponse;
import com.uem.ambulancias.integracion.ia.ContratoIa.Provenance;
import com.uem.ambulancias.integracion.ia.ContratoIa.SummaryRequest;
import com.uem.ambulancias.integracion.ia.ContratoIa.SummaryResponse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Llama al servicio de análisis por HTTP. Las llamadas son síncronas y largas: el timeout de lectura es mayor que el
 * peor caso del servicio. Cada error se traduce, según la tabla del servicio, en reintentar, firmar de nuevo, dar por
 * fallido o pedir revisión.
 */
@Slf4j
public class ClienteIaHttp implements ServicioDeAnalisis {

	/** El archivo o el pedido no sirven: reintentar daría lo mismo. */
	private static final Set<String> DEFINITIVOS = Set.of("invalid_request", "unsupported_media_type",
			"evidence_empty", "evidence_too_large", "mime_mismatch", "checksum_mismatch", "duration_exceeded",
			"unreadable_media", "object_not_found", "download_failed", "invalid_download_url", "unknown_alert",
			"duplicate_source", "nothing_to_summarize", "too_many_sources");

	private static final Set<String> PASAJEROS = Set.of("download_unavailable", "provider_unavailable",
			"invalid_model_output", "internal_error");

	private static final Set<String> DE_CONFIGURACION = Set.of("unauthorized", "provider_rejected",
			"provider_misconfigured", "service_not_configured");

	private static final String URL_VENCIDA = "download_forbidden";

	private final RestClient rest;

	public ClienteIaHttp(IaProperties config) {
		// Sin redirecciones: el servicio no las usa, y seguir una mandaría el token a otro lado. HTTP/1.1 porque
		// uvicorn no habla HTTP/2: el cliente de Java pediría pasar a h2c y el servicio deja un aviso por cada pedido.
		HttpClient http = HttpClient.newBuilder()
				.version(HttpClient.Version.HTTP_1_1)
				.connectTimeout(config.conexion())
				.followRedirects(HttpClient.Redirect.NEVER)
				.build();
		JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(http);
		fabrica.setReadTimeout(config.lectura());
		this.rest = RestClient.builder()
				.baseUrl(config.url())
				.requestFactory(fabrica)
				.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.token())
				.build();
	}

	@Override
	public AnalisisRecibido analizar(PedidoDeAnalisis pedido) {
		AnalysisRequest cuerpo = new AnalysisRequest(pedido.trabajo().toString(), pedido.evidenciaId(),
				pedido.alertaId(), pedido.incidenteId(), pedido.urlLectura(), pedido.mimeType(), pedido.sha256());
		AnalysisResponse respuesta = llamar("/v1/analyses", cuerpo, AnalysisResponse.class);
		if (respuesta.analysis() == null || !respuesta.analysis().isObject()) {
			throw new FalloDelServicioIa("respuesta_incompleta", Desenlace.REINTENTAR);
		}
		return new AnalisisRecibido(respuesta.modality(), respuesta.schemaVersion(), respuesta.analysis().toString(),
				procedencia(respuesta.provenance()));
	}

	@Override
	public ResumenRecibido resumir(PedidoDeResumen pedido) {
		SummaryRequest cuerpo = new SummaryRequest(
				pedido.incidenteId(),
				pedido.alertas().stream()
						.map(alerta -> new ContratoIa.Alert(alerta.alertaId(), texto(alerta.emitidaEn()),
								alerta.descripcion(), alerta.cantidadAfectados(), alerta.emisorEsPaciente()))
						.toList(),
				pedido.evidencias().stream()
						.map(evidencia -> new ContratoIa.Evidence(evidencia.evidenciaId(), evidencia.alertaId(),
								evidencia.modalidad(), texto(evidencia.recibidaEn()), evidencia.analisisJson()))
						.toList());
		SummaryResponse respuesta = llamar("/v1/summaries", cuerpo, SummaryResponse.class);
		if (respuesta.summary() == null || !respuesta.summary().isObject()) {
			throw new FalloDelServicioIa("respuesta_incompleta", Desenlace.REINTENTAR);
		}
		return new ResumenRecibido(respuesta.summary().toString(), lista(respuesta.usedEvidenceIds()),
				lista(respuesta.usedAlertIds()), procedencia(respuesta.provenance()));
	}

	private <T> T llamar(String ruta, Object cuerpo, Class<T> tipo) {
		try {
			return rest.post()
					.uri(ruta)
					.contentType(MediaType.APPLICATION_JSON)
					.body(cuerpo)
					.exchange((peticion, respuesta) -> {
						if (respuesta.getStatusCode().is2xxSuccessful()) {
							T leido = respuesta.bodyTo(tipo);
							if (leido == null) {
								throw new FalloDelServicioIa("respuesta_vacia", Desenlace.REINTENTAR);
							}
							return leido;
						}
						throw fallo(respuesta.getStatusCode(), leerError(respuesta));
					});
		} catch (FalloDelServicioIa e) {
			throw e;
		} catch (RestClientException e) {
			// No respondió, se cortó o mandó algo ilegible: puede ser pasajero.
			throw new FalloDelServicioIa("sin_respuesta", Desenlace.REINTENTAR, e);
		}
	}

	private static ErrorResponse leerError(RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse respuesta) {
		try {
			return respuesta.bodyTo(ErrorResponse.class);
		} catch (RuntimeException e) {
			return null;
		}
	}

	/**
	 * La tabla de errores del servicio. Un código que no figura se resuelve con su {@code retryable}, y si ni eso
	 * llegó, con el estado HTTP.
	 */
	static FalloDelServicioIa fallo(HttpStatusCode estado, ErrorResponse error) {
		String codigo = error != null && error.errorCode() != null ? error.errorCode() : "http_" + estado.value();
		return new FalloDelServicioIa(codigo, desenlace(estado, codigo, error == null ? null : error.retryable()));
	}

	static Desenlace desenlace(HttpStatusCode estado, String codigo, Boolean reintentable) {
		if (URL_VENCIDA.equals(codigo)) {
			return Desenlace.FIRMAR_DE_NUEVO;
		}
		if (DE_CONFIGURACION.contains(codigo)) {
			return Desenlace.REVISION;
		}
		if (DEFINITIVOS.contains(codigo)) {
			return Desenlace.DEFINITIVO;
		}
		if (PASAJEROS.contains(codigo)) {
			return Desenlace.REINTENTAR;
		}
		log.warn("El servicio de análisis respondió un código que no está en su contrato: {} ({}).", codigo, estado);
		if (reintentable != null) {
			return reintentable ? Desenlace.REINTENTAR : Desenlace.DEFINITIVO;
		}
		// Una ruta que no existe o un token rechazado son configuración, no la evidencia.
		if (estado.value() == HttpStatus.UNAUTHORIZED.value() || estado.value() == HttpStatus.FORBIDDEN.value()
				|| estado.value() == HttpStatus.NOT_FOUND.value()) {
			return Desenlace.REVISION;
		}
		return estado.is5xxServerError() || estado.value() == HttpStatus.TOO_MANY_REQUESTS.value()
				? Desenlace.REINTENTAR
				: Desenlace.DEFINITIVO;
	}

	private static Procedencia procedencia(Provenance procedencia) {
		if (procedencia == null) {
			return new Procedencia(null, null, null, null, null);
		}
		return new Procedencia(procedencia.provider(), procedencia.model(), procedencia.promptVersion(),
				instante(procedencia.generatedAt()), procedencia.method());
	}

	/** La fecha del servicio puede venir con {@code Z} o con desfase ({@code +00:00}). Si no se entiende, queda vacía. */
	private static Instant instante(String texto) {
		if (texto == null) {
			return null;
		}
		try {
			return OffsetDateTime.parse(texto).toInstant();
		} catch (DateTimeParseException e) {
			return null;
		}
	}

	private static String texto(Instant instante) {
		return instante == null ? null : instante.toString();
	}

	private static List<Long> lista(List<Long> ids) {
		return ids == null ? List.of() : ids;
	}

}
