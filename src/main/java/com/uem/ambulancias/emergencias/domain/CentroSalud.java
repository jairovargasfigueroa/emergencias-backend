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
 * Catálogo de clínicas, hospitales y postas donde se entrega al paciente. Baja lógica con {@code activo}: un centro
 * dado de baja deja de ofrecerse, pero las atenciones y los traslados que ya lo usaron siguen apuntándolo.
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


	public static CentroSalud registrar(String nombre, Point ubicacion, String direccion) {
		CentroSalud centro = new CentroSalud();
		centro.nombre = nombre;
		centro.ubicacion = ubicacion;
		centro.direccion = direccion;
		centro.activo = true;
		return centro;
	}

	/**
	 * Corrige los datos del centro. Las atenciones y los traslados guardan el centro por referencia y copian su
	 * punto al registrarse, así que una corrección no cambia dónde se entregó a nadie.
	 */
	public void corregir(String nombre, Point ubicacion, String direccion) {
		this.nombre = nombre;
		this.ubicacion = ubicacion;
		this.direccion = direccion;
	}

	public void desactivar() {
		this.activo = false;
	}

	public void activar() {
		this.activo = true;
	}

}
