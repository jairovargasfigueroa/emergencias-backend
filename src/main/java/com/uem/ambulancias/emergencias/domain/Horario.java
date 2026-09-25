package com.uem.ambulancias.emergencias.domain;

import java.time.Instant;

/**
 * Los tres momentos de un traslado. Se calculan al pedirlo y quedan congelados: si mañana se ajusta la velocidad
 * con la que se estima el viaje, los traslados ya pedidos no se mueven solos.
 */
public record Horario(ModoHorario modo, Instant horaCita, Instant salidaEstimada, Instant limiteSalida) {

	public Horario {
		if ((modo == ModoHorario.PROGRAMADO) != (horaCita != null)) {
			throw new IllegalArgumentException("Un traslado programado lleva hora de cita y uno inmediato no.");
		}
		if (limiteSalida.isBefore(salidaEstimada)) {
			throw new IllegalArgumentException("El límite de salida no puede ser anterior a la salida estimada.");
		}
	}

}
