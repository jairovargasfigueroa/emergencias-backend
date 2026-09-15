package com.uem.ambulancias.emergencias.domain;

import com.uem.ambulancias.comun.geo.Geo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Point;

/**
 * Catálogo de clínicas, hospitales y postas donde se entrega al paciente. Baja lógica con {@code activo}.
 */
@Entity
@Table(name = "centro_salud")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CentroSalud {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String nombre;

	@Column(nullable = false, columnDefinition = Geo.COLUMNA_PUNTO)
	private Point ubicacion;

	private String direccion;

	@Column(nullable = false)
	private boolean activo;

}
