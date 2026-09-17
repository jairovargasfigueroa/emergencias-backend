package com.uem.ambulancias.flota.domain;

import java.time.Instant;

import com.uem.ambulancias.comun.error.CodigoError;
import com.uem.ambulancias.comun.error.ConflictoException;
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

	/** ME-1 M0: una ambulancia nace DISPONIBLE y activa. */
	public static Ambulancia registrar(String placa, String tipoUnidad) {
		Ambulancia ambulancia = new Ambulancia();
		ambulancia.placa = placa;
		ambulancia.tipoUnidad = tipoUnidad;
		ambulancia.estado = EstadoAmbulancia.DISPONIBLE;
		ambulancia.activa = true;
		return ambulancia;
	}

	/** ME-1 M4. Regla de integridad: con una atención activa no puede salir de servicio. */
	public void marcarFueraDeServicio() {
		if (estado == EstadoAmbulancia.EN_ATENCION) {
			throw enAtencion();
		}
		cambiarEstado(EstadoAmbulancia.DISPONIBLE, EstadoAmbulancia.FUERA_DE_SERVICIO);
	}

	/** ME-1 M5. */
	public void reactivar() {
		cambiarEstado(EstadoAmbulancia.FUERA_DE_SERVICIO, EstadoAmbulancia.DISPONIBLE);
	}

	/** Baja lógica. Se rechaza mientras tenga una atención activa. */
	public void desactivar() {
		if (estado == EstadoAmbulancia.EN_ATENCION) {
			throw enAtencion();
		}
		activa = false;
	}

	/** Aplica una transición de ME-1: solo es válida desde el estado indicado. */
	private void cambiarEstado(EstadoAmbulancia desde, EstadoAmbulancia hasta) {
		if (estado != desde) {
			throw new ConflictoException(CodigoError.TRANSICION_INVALIDA,
					"La ambulancia " + placa + " no puede pasar de " + estado + " a " + hasta + ".");
		}
		estado = hasta;
	}

	private ConflictoException enAtencion() {
		return new ConflictoException(CodigoError.AMBULANCIA_EN_ATENCION,
				"La ambulancia " + placa + " tiene una atención en curso.");
	}

}
