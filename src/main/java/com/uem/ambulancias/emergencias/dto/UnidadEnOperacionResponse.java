package com.uem.ambulancias.emergencias.dto;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import com.uem.ambulancias.flota.domain.Ambulancia;
import com.uem.ambulancias.flota.domain.EstadoAmbulancia;
import com.uem.ambulancias.flota.domain.TipoUnidad;
import com.uem.ambulancias.flota.domain.Turno;

import org.locationtech.jts.geom.Point;

/**
 * Una unidad en el centro de control, con todo lo que hay que saber de ella de un vistazo: quién va adentro, qué
 * está haciendo y dónde estaba la última vez que se supo.
 */
public record UnidadEnOperacionResponse(

		Long ambulanciaId,
		String placa,
		TipoUnidad tipoUnidad,
		EstadoAmbulancia estado,
		boolean activa,

		/** Quiénes tienen turno abierto en esta unidad. Vacía si no hay nadie adentro. */
		List<Tripulante> tripulacion,

		/** Desde cuándo hay tripulación: el más viejo de los turnos abiertos. {@code null} si no hay ninguno. */
		Instant turnoDesde,

		/** {@code null} cuando la unidad no está atendiendo nada. */
		AtencionEnCursoResponse atencion,

		/**
		 * La última posición guardada en la base, no la de Firebase. Es solo la pintada inicial del mapa: en
		 * cuanto llega la primera posición en vivo, el panel la reemplaza.
		 */
		Posicion ultimaPosicion) {

	/** Un paramédico de turno. Lleva el teléfono porque el despacho a veces necesita llamarlo. */
	public record Tripulante(Long id, String nombreCompleto, String telefono) {
	}

	/** Dónde estaba la unidad y cuándo se guardó ese dato. */
	public record Posicion(double latitud, double longitud, Instant en) {
	}

	/**
	 * {@code turnosAbiertos} son los de esta unidad, con su paramédico traído; {@code atencion} es {@code null} si
	 * no está atendiendo.
	 */
	public static UnidadEnOperacionResponse de(Ambulancia ambulancia, List<Turno> turnosAbiertos,
			AtencionEnCursoResponse atencion) {
		return new UnidadEnOperacionResponse(
				ambulancia.getId(),
				ambulancia.getPlaca(),
				ambulancia.getTipoUnidad(),
				ambulancia.getEstado(),
				ambulancia.isActiva(),
				turnosAbiertos.stream()
						.map(turno -> new Tripulante(turno.getParamedico().getId(),
								turno.getParamedico().getNombreCompleto(), turno.getParamedico().getTelefono()))
						.toList(),
				turnosAbiertos.stream().map(Turno::getInicio).min(Comparator.naturalOrder()).orElse(null),
				atencion,
				posicion(ambulancia));
	}

	private static Posicion posicion(Ambulancia ambulancia) {
		Point punto = ambulancia.getUltimaPosicion();
		return punto == null ? null
				: new Posicion(punto.getY(), punto.getX(), ambulancia.getUltimaPosicionEn());
	}

}
