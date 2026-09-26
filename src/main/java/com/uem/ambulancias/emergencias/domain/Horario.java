package com.uem.ambulancias.emergencias.domain;

import java.time.Instant;

/**
 * Los momentos de un traslado, calculados al pedirlo y congelados: si mañana se ajusta la velocidad con la que se
 * estima el viaje, los traslados ya pedidos no se mueven solos.
 *
 * <p>La salida y la recogida no son lo mismo y la diferencia importa: entre que la unidad arranca y llega a la
 * puerta pasan los minutos de acercamiento. A la familia se le promete la recogida, que es lo que le sirve.
 */
public record Horario(ModoHorario modo, Instant horaCita, Instant salidaEstimada, Instant limiteSalida,
		Instant recogidaDesde, Instant recogidaHasta) {

	public Horario {
		if ((modo == ModoHorario.PROGRAMADO) != (horaCita != null)) {
			throw new IllegalArgumentException("Un traslado programado lleva hora de cita y uno inmediato no.");
		}
		if (limiteSalida.isBefore(salidaEstimada)) {
			throw new IllegalArgumentException("El límite de salida no puede ser anterior a la salida estimada.");
		}
	}

}
