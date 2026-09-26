package com.uem.ambulancias.emergencias.dto;

import java.time.Instant;

import com.uem.ambulancias.emergencias.domain.EstadoTraslado;
import com.uem.ambulancias.emergencias.domain.ModoHorario;
import com.uem.ambulancias.emergencias.domain.Movilidad;
import com.uem.ambulancias.emergencias.domain.Traslado;
import com.uem.ambulancias.flota.domain.TipoUnidad;

/**
 * Un traslado como lo ven el ciudadano y el panel. {@code tipoUnidad} es el que hay que buscar de verdad; el
 * pedido se mantiene aparte para saber cuándo hubo que corregirlo.
 */
public record TrasladoResponse(

		Long id,
		EstadoTraslado estado,
		ModoHorario modoHorario,
		Instant horaCita,
		Instant horaSalidaEstimada,
		Instant horaLimiteSalida,
		/** La ventana que se le promete a la familia: cuándo pasa la unidad por el origen. */
		Instant horaRecogidaDesde,
		Instant horaRecogidaHasta,

		String pasajero,
		Movilidad movilidad,
		boolean oxigeno,
		boolean equipo,
		boolean aislamiento,
		Integer pesoAproximado,
		int acompanantes,
		String observaciones,

		TipoUnidad tipoUnidad,
		TipoUnidad tipoUnidadPedido,

		UbicacionResponse origen,
		String origenReferencia,
		String contactoNombre,
		String contactoTelefono,

		UbicacionResponse destino,
		String centroSaludDestino,
		String destinoDetalle,

		Instant fechaHoraCreacion) {

	public static TrasladoResponse de(Traslado traslado) {
		return new TrasladoResponse(
				traslado.getId(),
				traslado.getEstado(),
				traslado.getModoHorario(),
				traslado.getHoraCita(),
				traslado.getHoraSalidaEstimada(),
				traslado.getHoraLimiteSalida(),
				traslado.getHoraRecogidaDesde(),
				traslado.getHoraRecogidaHasta(),
				traslado.getPasajero().getNombreCompleto(),
				traslado.getMovilidad(),
				traslado.isRequiereOxigeno(),
				traslado.isRequiereEquipo(),
				traslado.isRequiereAislamiento(),
				traslado.getPesoAproximado(),
				traslado.getAcompanantes(),
				traslado.getObservaciones(),
				traslado.tipoUnidadEfectivo(),
				traslado.getTipoUnidadPedido(),
				UbicacionResponse.de(traslado.getOrigen()),
				traslado.getOrigenReferencia(),
				traslado.getContactoNombre(),
				traslado.getContactoTelefono(),
				UbicacionResponse.de(traslado.getDestino()),
				traslado.getCentroSaludDestino() == null ? null : traslado.getCentroSaludDestino().getNombre(),
				traslado.getDestinoDetalle(),
				traslado.getFechaHoraCreacion());
	}

}
