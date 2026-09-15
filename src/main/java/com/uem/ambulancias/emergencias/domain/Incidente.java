package com.uem.ambulancias.emergencias.domain;

import java.time.Instant;

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
 * El suceso real: agrupa una o más alertas y es a lo que acuden las unidades.
 * {@code cerradoPor} es {@code null} en los cierres automáticos.
 */
@Entity
@Table(name = "incidente")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Incidente {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, columnDefinition = Geo.COLUMNA_PUNTO)
	private Point ubicacion;

	@Column(nullable = false)
	private Instant fechaHoraCreacion;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private EstadoIncidente estado;

	@Enumerated(EnumType.STRING)
	private MotivoCierreIncidente motivoCierre;

	private Integer cantidadAfectados;

	private Instant fechaHoraCierre;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "cerrado_por_id")
	private Usuario cerradoPor;

	/** ME-1 I0: nace ACTIVO con la ubicación efectiva y los afectados de su primera alerta. */
	public static Incidente crear(Point ubicacion, Integer cantidadAfectados, Instant fechaHoraCreacion) {
		Incidente incidente = new Incidente();
		incidente.ubicacion = ubicacion;
		incidente.cantidadAfectados = cantidadAfectados;
		incidente.fechaHoraCreacion = fechaHoraCreacion;
		incidente.estado = EstadoIncidente.ACTIVO;
		return incidente;
	}

	/**
	 * Afectados consolidados: el máximo de lo reportado, nunca la suma, porque cada emisor estima la escena
	 * completa. Una cantidad {@code null} no cambia nada.
	 */
	public void consolidarAfectados(Integer cantidad) {
		if (cantidad == null) {
			return;
		}
		if (cantidadAfectados == null || cantidad > cantidadAfectados) {
			cantidadAfectados = cantidad;
		}
	}

}
