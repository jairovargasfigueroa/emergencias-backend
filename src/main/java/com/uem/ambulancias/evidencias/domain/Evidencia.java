package com.uem.ambulancias.evidencias.domain;

import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

import com.uem.ambulancias.comun.error.CodigoError;
import com.uem.ambulancias.comun.error.ConflictoException;
import com.uem.ambulancias.emergencias.domain.Alerta;
import com.uem.ambulancias.emergencias.domain.EstadoAlerta;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Una foto, un audio o un video que el ciudadano adjunta a su alerta. El archivo no pasa por el servidor: la app lo
 * sube directo al almacén con una URL firmada, y acá queda lo que se firmó para poder comprobar después que lo subido
 * es exactamente eso.
 */
@Entity
@Table(name = "evidencia")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Evidencia {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "alerta_id", nullable = false)
	private Alerta alerta;

	@Column(nullable = false)
	private String mimeType;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Modalidad modalidad;

	@Column(nullable = false)
	private long tamanoBytes;

	/** SHA-256 de los bytes, en hexadecimal y minúsculas, tal como lo calculó la app. */
	@Column(nullable = false, length = 64)
	private String sha256;

	/**
	 * Dónde queda el archivo: {@code alertas/{alertaId}/{evidenciaId}.{ext}}. Lleva el id de la evidencia, así que se
	 * fija apenas se guarda, en la misma transacción.
	 */
	@Column(unique = true)
	private String claveObjeto;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private EstadoEvidencia estado;

	@Column(nullable = false)
	private Instant registradaEn;

	private Instant subidaEn;

	/** Cuándo terminó el análisis, bien o mal. */
	private Instant procesadaEn;

	/**
	 * Evidencia recién pedida: nace PENDIENTE_SUBIDA. Solo para una alerta en pie de un incidente abierto, en un
	 * formato que se pueda analizar y sin pasarse del límite de su modalidad.
	 */
	public static Evidencia registrar(Alerta alerta, FormatoEvidencia formato, long tamanoBytes, String sha256,
			long limiteBytes, Instant ahora) {
		exigirAlertaAbierta(alerta);
		if (tamanoBytes > limiteBytes) {
			throw new ConflictoException(CodigoError.EVIDENCIA_DEMASIADO_GRANDE,
					"El archivo pesa más de lo que se acepta para " + formato.modalidad().name().toLowerCase(Locale.ROOT)
							+ " (" + limiteBytes / (1024 * 1024) + " MiB).");
		}
		Evidencia evidencia = new Evidencia();
		evidencia.alerta = alerta;
		evidencia.mimeType = formato.mimeType();
		evidencia.modalidad = formato.modalidad();
		evidencia.tamanoBytes = tamanoBytes;
		evidencia.sha256 = sha256.toLowerCase(Locale.ROOT);
		evidencia.estado = EstadoEvidencia.PENDIENTE_SUBIDA;
		evidencia.registradaEn = ahora;
		return evidencia;
	}

	/** Una alerta cancelada o de un incidente ya cerrado no recibe archivos nuevos. */
	public static void exigirAlertaAbierta(Alerta alerta) {
		boolean alertaEnPie = alerta.getEstado() != EstadoAlerta.CANCELADA
				&& alerta.getEstado() != EstadoAlerta.DESCARTADA;
		if (!alertaEnPie || !alerta.getIncidente().getEstado().isAbierto()) {
			throw new ConflictoException(CodigoError.ALERTA_CERRADA,
					"La alerta " + alerta.getId() + " ya no admite evidencias.");
		}
	}

	/** Fija la clave del archivo en el almacén. Necesita el id, por eso va después de guardar. */
	public void asignarClaveObjeto() {
		if (id == null) {
			throw new IllegalStateException("La evidencia todavía no tiene id.");
		}
		String extension = FormatoEvidencia.deMime(mimeType).orElseThrow().extension();
		claveObjeto = "alertas/" + alerta.getId() + "/" + id + "." + extension;
	}

	/** El mismo SHA-256 en base64, que es como lo pide y lo devuelve el almacén. */
	public String getSha256Base64() {
		return Base64.getEncoder().encodeToString(HexFormat.of().parseHex(sha256));
	}

	public boolean isPendienteDeSubida() {
		return estado == EstadoEvidencia.PENDIENTE_SUBIDA;
	}

	/** El archivo ya está en el almacén y coincide con lo firmado. */
	public void confirmarSubida(Instant ahora) {
		pasarA(EstadoEvidencia.SUBIDA, EstadoEvidencia.PENDIENTE_SUBIDA);
		subidaEn = ahora;
	}

	public void marcarAnalizada(Instant ahora) {
		pasarA(EstadoEvidencia.ANALIZADA, EstadoEvidencia.SUBIDA);
		procesadaEn = ahora;
	}

	/** El análisis no se pudo hacer y no se va a reintentar. El archivo sigue ahí para el personal. */
	public void marcarFallida(Instant ahora) {
		pasarA(EstadoEvidencia.FALLIDA, EstadoEvidencia.SUBIDA);
		procesadaEn = ahora;
	}

	private void pasarA(EstadoEvidencia nuevo, EstadoEvidencia desde) {
		if (estado != desde) {
			throw new ConflictoException(CodigoError.TRANSICION_INVALIDA,
					"La evidencia " + id + " no puede pasar de " + estado + " a " + nuevo + ".");
		}
		estado = nuevo;
	}

}
