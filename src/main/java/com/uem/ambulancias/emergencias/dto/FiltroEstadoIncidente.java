package com.uem.ambulancias.emergencias.dto;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import com.uem.ambulancias.emergencias.domain.EstadoIncidente;

/**
 * Filtro de la consulta de incidentes del panel: ABIERTOS son ACTIVO y EN_ATENCION; CERRADOS, los cuatro estados
 * finales de ME-1.
 */
public enum FiltroEstadoIncidente {

	ABIERTOS,
	CERRADOS,
	TODOS;

	public Set<EstadoIncidente> estados() {
		return switch (this) {
			case ABIERTOS -> EstadoIncidente.ABIERTOS;
			case CERRADOS -> Arrays.stream(EstadoIncidente.values())
					.filter(estado -> !estado.isAbierto())
					.collect(Collectors.toUnmodifiableSet());
			case TODOS -> Set.of(EstadoIncidente.values());
		};
	}

}
