package com.uem.ambulancias.emergencias.service;

import java.time.Instant;
import java.util.List;

import com.uem.ambulancias.emergencias.domain.EstadoAtencion;
import com.uem.ambulancias.emergencias.domain.EstadoIncidente;

/**
 * Lo que ve el ciudadano de su incidente: el estado y solo las unidades con atención activa, que son las únicas que
 * exponen su posición (PB-06 R2).
 */
public record SeguimientoPublicado(Long incidenteId, EstadoIncidente estado, List<Unidad> unidades) {

	/** Posición: la última guardada de la ambulancia, si tiene; la posición en vivo llega aparte. */
	public record Unidad(
			Long ambulanciaId,
			String placa,
			EstadoAtencion estado,
			Double latitud,
			Double longitud,
			Instant posicionEn) {
	}

}
