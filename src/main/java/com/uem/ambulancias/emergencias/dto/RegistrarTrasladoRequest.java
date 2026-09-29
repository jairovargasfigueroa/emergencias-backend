package com.uem.ambulancias.emergencias.dto;

import java.time.Instant;

import com.uem.ambulancias.emergencias.domain.Movilidad;
import com.uem.ambulancias.flota.domain.TipoUnidad;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Pedido de traslado. Lo que el paciente necesita son datos de este viaje y no de la persona, porque pueden
 * cambiar de un traslado al otro.
 *
 * <p>{@code pasajeroId} nulo significa que viaja quien pide. {@code horaCita} nula significa "para ahora".
 * {@code tipoUnidad} nulo acepta la unidad que el sistema sugiere; si viene, no puede ser menor que esa.
 */
public record RegistrarTrasladoRequest(

		Long pasajeroId,

		@NotNull(message = "Hay que indicar cómo se moviliza el paciente.")
		Movilidad movilidad,

		boolean oxigeno,

		boolean equipo,

		boolean aislamiento,

		@Positive(message = "El peso aproximado tiene que ser mayor que cero.")
		Integer pesoAproximado,

		@PositiveOrZero(message = "Los acompañantes no pueden ser negativos.")
		int acompanantes,

		@Size(max = 2000, message = "Las observaciones son demasiado largas.")
		String observaciones,

		TipoUnidad tipoUnidad,

		@NotNull(message = "La latitud del origen es obligatoria.")
		@DecimalMin(value = "-90", message = "La latitud debe estar entre -90 y 90.")
		@DecimalMax(value = "90", message = "La latitud debe estar entre -90 y 90.")
		Double origenLatitud,

		@NotNull(message = "La longitud del origen es obligatoria.")
		@DecimalMin(value = "-180", message = "La longitud debe estar entre -180 y 180.")
		@DecimalMax(value = "180", message = "La longitud debe estar entre -180 y 180.")
		Double origenLongitud,

		@Size(max = 255, message = "La referencia es demasiado larga.")
		String origenReferencia,

		@Size(max = 255, message = "El nombre del contacto es demasiado largo.")
		String contactoNombre,

		@Size(max = 30, message = "El teléfono del contacto es demasiado largo.")
		String contactoTelefono,

		Long centroSaludDestinoId,

		@DecimalMin(value = "-90", message = "La latitud debe estar entre -90 y 90.")
		@DecimalMax(value = "90", message = "La latitud debe estar entre -90 y 90.")
		Double destinoLatitud,

		@DecimalMin(value = "-180", message = "La longitud debe estar entre -180 y 180.")
		@DecimalMax(value = "180", message = "La longitud debe estar entre -180 y 180.")
		Double destinoLongitud,

		@Size(max = 255, message = "El detalle del destino es demasiado largo.")
		String destinoDetalle,

		Instant horaCita) {
}
