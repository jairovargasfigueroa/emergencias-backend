package com.uem.ambulancias.emergencias.domain;

import java.time.Instant;

import com.uem.ambulancias.comun.geo.Geo;
import com.uem.ambulancias.flota.domain.Ambulancia;

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
 * Lo que una unidad hace en un incidente. Cada hito congela su hora y su ubicación; el paciente se
 * registra aquí porque cada unidad recoge al suyo.
 */
@Entity
@Table(name = "atencion")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Atencion {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Instant horaToma;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private EstadoAtencion estado;

	private Instant horaLlegada;

	@Column(columnDefinition = Geo.COLUMNA_PUNTO)
	private Point ubicacionLlegada;

	private Instant horaRecogida;

	@Column(columnDefinition = Geo.COLUMNA_PUNTO)
	private Point ubicacionRecogida;

	private Instant horaEntrega;

	@Column(columnDefinition = Geo.COLUMNA_PUNTO)
	private Point ubicacionEntrega;

	@Enumerated(EnumType.STRING)
	private MotivoCancelacionAtencion motivoCancelacion;

	private Instant horaCancelacion;

	private String nombrePaciente;

	private String documentoPaciente;

	@Column(columnDefinition = "text")
	private String destinoDescripcion;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "incidente_id", nullable = false)
	private Incidente incidente;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "ambulancia_id", nullable = false)
	private Ambulancia ambulancia;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "centro_salud_id")
	private CentroSalud centroSalud;

	/** ME-1 A0: la atención nace EN_CAMINO cuando la unidad toma o se suma al incidente. */
	public static Atencion iniciar(Incidente incidente, Ambulancia ambulancia, Instant horaToma) {
		Atencion atencion = new Atencion();
		atencion.incidente = incidente;
		atencion.ambulancia = ambulancia;
		atencion.horaToma = horaToma;
		atencion.estado = EstadoAtencion.EN_CAMINO;
		return atencion;
	}

}
