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

	/** ME-1 M0: una ambulancia nace activa y sin turno, porque todavía no hay nadie que la opere. */
	public static Ambulancia registrar(String placa, String tipoUnidad) {
		Ambulancia ambulancia = new Ambulancia();
		ambulancia.placa = placa;
		ambulancia.tipoUnidad = tipoUnidad;
		// Nace sin tripulación: recién cuenta como disponible cuando alguien entre en turno con ella.
		ambulancia.estado = EstadoAmbulancia.SIN_TURNO;
		ambulancia.activa = true;
		return ambulancia;
	}

	/**
	 * ME-1 M4, por el administrador. Regla de integridad: con una atención activa no puede salir de servicio. Se
	 * acepta también sin nadie de turno: una unidad se rompe igual estando en la base.
	 */
	public void marcarFueraDeServicio() {
		if (estado == EstadoAmbulancia.EN_ATENCION) {
			throw enAtencion();
		}
		if (estado != EstadoAmbulancia.DISPONIBLE && estado != EstadoAmbulancia.SIN_TURNO) {
			throw transicionInvalida(EstadoAmbulancia.FUERA_DE_SERVICIO);
		}
		estado = EstadoAmbulancia.FUERA_DE_SERVICIO;
	}

	/**
	 * ME-1 M5, por el paramédico o el administrador. Que se arregle la avería no la pone a trabajar: vuelve a estar
	 * disponible solo si hay alguien de turno adentro.
	 */
	public void reactivar(boolean hayTurnoAbierto) {
		EstadoAmbulancia destino = hayTurnoAbierto ? EstadoAmbulancia.DISPONIBLE : EstadoAmbulancia.SIN_TURNO;
		exigirEstado(EstadoAmbulancia.FUERA_DE_SERVICIO, destino);
		estado = destino;
	}

	/** Baja lógica. Se rechaza mientras tenga una atención activa. */
	public void desactivar() {
		if (estado == EstadoAmbulancia.EN_ATENCION) {
			throw enAtencion();
		}
		activa = false;
	}

	/** Puede tomar o sumarse a un incidente: está activa y DISPONIBLE (PB-04 R5). */
	public boolean puedeAtender() {
		return activa && estado == EstadoAmbulancia.DISPONIBLE;
	}

	/**
	 * Transición de ME-1 que dispara una atención: M1 al tomar o sumarse, M2 al entregar o cancelar, M3 al cancelar
	 * por avería. Si no es válida desde el estado actual, se rechaza y nada cambia.
	 */
	public void cambiarEstado(EstadoAmbulancia nuevo) {
		if (!estado.puedePasarA(nuevo)) {
			throw transicionInvalida(nuevo);
		}
		if (nuevo == EstadoAmbulancia.EN_ATENCION && !activa) {
			throw new ConflictoException(CodigoError.AMBULANCIA_NO_DISPONIBLE,
					"La ambulancia " + placa + " está desactivada.");
		}
		estado = nuevo;
	}

	private void exigirEstado(EstadoAmbulancia requerido, EstadoAmbulancia destino) {
		if (estado != requerido) {
			throw transicionInvalida(destino);
		}
	}

	private ConflictoException transicionInvalida(EstadoAmbulancia destino) {
		return new ConflictoException(CodigoError.TRANSICION_INVALIDA,
				"La ambulancia " + placa + " no puede pasar de " + estado + " a " + destino + ".");
	}

	private ConflictoException enAtencion() {
		return new ConflictoException(CodigoError.AMBULANCIA_EN_ATENCION,
				"La ambulancia " + placa + " tiene una atención en curso.");
	}

}
