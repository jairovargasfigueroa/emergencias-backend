package com.uem.ambulancias.emergencias.dto;

import java.time.Instant;

import com.uem.ambulancias.emergencias.domain.Atencion;
import com.uem.ambulancias.emergencias.domain.EstadoAtencion;
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
		/**
		 * En qué va la unidad que lo tiene: en camino, en la puerta, con el paciente a bordo. Nulo mientras no
		 * salió nadie. Al ciudadano le dice hasta cuándo puede cancelar: hasta que la unidad llega.
		 */
		EstadoAtencion estadoUnidad,
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
		/** El centro del catálogo, si el destino es uno: la app del paramédico lo trae ya elegido al entregar. */
		Long centroSaludDestinoId,
		String centroSaludDestino,
		String destinoDetalle,

		Instant fechaHoraCreacion) {

	/** Sin atención: para cuando todavía no salió nadie, o quien lo pide no necesita saber en qué va la unidad. */
	public static TrasladoResponse de(Traslado traslado) {
		return de(traslado, null);
	}

	/** {@code atencion} es la que lo tiene ahora, o nula si no hay ninguna. */
	public static TrasladoResponse de(Traslado traslado, Atencion atencion) {
		return new TrasladoResponse(
				traslado.getId(),
				traslado.getEstado(),
				atencion == null ? null : atencion.getEstado(),
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
				traslado.getCentroSaludDestino() == null ? null : traslado.getCentroSaludDestino().getId(),
				traslado.getCentroSaludDestino() == null ? null : traslado.getCentroSaludDestino().getNombre(),
				traslado.getDestinoDetalle(),
				traslado.getFechaHoraCreacion());
	}

}
