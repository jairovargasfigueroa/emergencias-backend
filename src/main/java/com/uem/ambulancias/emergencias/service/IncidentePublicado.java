package com.uem.ambulancias.emergencias.service;

import java.time.Instant;
import java.util.List;

import com.uem.ambulancias.emergencias.domain.EstadoIncidente;

/**
 * Lo que ven las unidades de un incidente abierto antes de tomarlo: ubicación, tiempo desde la creación,
 * afectados, descripciones de sus alertas y cuántas unidades ya acuden.
 */
public record IncidentePublicado(
		Long id,
		double latitud,
		double longitud,
		EstadoIncidente estado,
		Instant fechaHoraCreacion,
		Integer cantidadAfectados,
		List<String> descripciones,
		long unidadesAcudiendo) {
}
