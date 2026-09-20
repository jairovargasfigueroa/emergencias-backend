package com.uem.ambulancias.emergencias.domain;

import java.time.Instant;

import com.uem.ambulancias.comun.error.CodigoError;
import com.uem.ambulancias.comun.error.ConflictoException;
import com.uem.ambulancias.comun.geo.Geo;
import com.uem.ambulancias.usuarios.domain.Usuario;

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
import org.locationtech.jts.geom.Point;

/**
 * El aviso que emite una persona. Varias alertas pueden pertenecer al mismo incidente.
 */
@Entity
@Table(name = "alerta")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Alerta {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, columnDefinition = Geo.COLUMNA_PUNTO)
	private Point ubicacionOriginal;

	@Column(columnDefinition = Geo.COLUMNA_PUNTO)
	private Point ubicacionAjustada;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private OrigenUbicacion origenUbicacion;

	@Column(nullable = false)
	private Instant fechaHora;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private EstadoAlerta estado;

	@Enumerated(EnumType.STRING)
	private MotivoCancelacionAlerta motivoCancelacion;

	private Instant horaCancelacion;

	/**
	 * Si quien avisó es la persona que necesitaba la ambulancia. Se pregunta al retirar el pedido, que es donde
	 * importa: el despacho real no le da el mismo peso a que cancele el propio paciente o un tercero que pasaba.
	 */
	private Boolean emisorEsPaciente;

	private Integer cantidadAfectados;

	@Column(columnDefinition = "text")
	private String descripcion;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "emisor_id", nullable = false)
	private Usuario emisor;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "incidente_id", nullable = false)
	private Incidente incidente;

	/** Alerta recién emitida: nace RECIBIDA y se vincula a un incidente en la misma operación. */
	public static Alerta emitir(Usuario emisor, Point ubicacion, OrigenUbicacion origenUbicacion,
			Integer cantidadAfectados, String descripcion, Instant fechaHora) {
		Alerta alerta = new Alerta();
		alerta.emisor = emisor;
		alerta.ubicacionOriginal = ubicacion;
		alerta.origenUbicacion = origenUbicacion;
		alerta.cantidadAfectados = cantidadAfectados;
		alerta.descripcion = descripcion;
		alerta.fechaHora = fechaHora;
		alerta.estado = EstadoAlerta.RECIBIDA;
		return alerta;
	}

	/** Ubicación efectiva: la ajustada si existe; si no, la original. Es la que se usa para agrupar. */
	public Point getUbicacionEfectiva() {
		return ubicacionAjustada != null ? ubicacionAjustada : ubicacionOriginal;
	}

	/** Asocia la alerta al incidente y la deja VINCULADA. */
	public void vincular(Incidente incidente) {
		this.incidente = incidente;
		this.estado = EstadoAlerta.VINCULADA;
	}

	/**
	 * El ciudadano retira su pedido. Retirar un pedido no es cerrar la emergencia: si quedan otras alertas vivas o ya
	 * hay una unidad en camino, el incidente sigue y lo resuelve quien corresponde.
	 */
	public void cancelar(MotivoCancelacionAlerta motivo, Boolean emisorEsPaciente) {
		if (motivo == null) {
			throw new IllegalArgumentException("El motivo de cancelación es obligatorio.");
		}
		if (estado == EstadoAlerta.CANCELADA) {
			throw new ConflictoException(CodigoError.TRANSICION_INVALIDA, "La alerta " + id + " ya estaba cancelada.");
		}
		estado = EstadoAlerta.CANCELADA;
		motivoCancelacion = motivo;
		horaCancelacion = Instant.now();
		this.emisorEsPaciente = emisorEsPaciente;
	}

	/**
	 * Datos opcionales que la app pregunta después de emitir, porque no bloquean la emisión (PB-02 R3). Solo se
	 * aplican los campos que llegan: uno {@code null} deja el valor anterior sin tocar.
	 */
	public void completarDetalles(Integer cantidadAfectados, String descripcion) {
		if (cantidadAfectados != null) {
			this.cantidadAfectados = cantidadAfectados;
		}
		if (descripcion != null) {
			this.descripcion = descripcion;
		}
	}

}
