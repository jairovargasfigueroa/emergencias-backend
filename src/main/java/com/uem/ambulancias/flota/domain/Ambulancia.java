package com.uem.ambulancias.flota.domain;

import java.time.Instant;

import com.uem.ambulancias.comun.geo.Geo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Point;

/**
 * Unidad de la flota. Baja lógica con {@code activa}. La posición en vivo no se guarda aquí:
 * {@code ultimaPosicion} es la última foto persistida para la consulta de cercanía.
 */
@Entity
@Table(name = "ambulancia", uniqueConstraints = @UniqueConstraint(name = "uk_ambulancia_placa", columnNames = "placa"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Ambulancia {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String placa;

	@Column(nullable = false)
	private String tipoUnidad;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private EstadoAmbulancia estado;

	@Column(nullable = false)
	private boolean activa;

	@Column(columnDefinition = Geo.COLUMNA_PUNTO)
	private Point ultimaPosicion;

	private Instant ultimaPosicionEn;

}
