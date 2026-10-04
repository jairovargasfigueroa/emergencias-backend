package com.uem.ambulancias.evidencias.domain;

import java.util.Objects;
import java.util.Set;

/**
 * Lo que cambia la preparación de la tripulación, sacado de una versión del resumen: la gravedad, el tipo de suceso,
 * cuántas personas hay y qué peligros siguen en pie. Una versión nueva solo se avisa por push si cambia algo de esto;
 * un cambio de redacción no, porque un aviso que suena por todo deja de escucharse.
 *
 * @param peligros los de {@code hazards}, que es la misma lista de textos en las versiones 1 y 2 del resumen
 */
public record DatosClaveDelResumen(String gravedad, String tipoSuceso, Integer personasMinimo, Integer personasMaximo,
		Set<String> peligros) {

	public DatosClaveDelResumen {
		peligros = Set.copyOf(peligros);
	}

	/** Si esta versión cambia algo que la tripulación tiene que saber respecto de la anterior. */
	public boolean cambioImportanteRespectoDe(DatosClaveDelResumen anterior) {
		return !Objects.equals(gravedad, anterior.gravedad) || !Objects.equals(tipoSuceso, anterior.tipoSuceso)
				|| !Objects.equals(personasMinimo, anterior.personasMinimo)
				|| !Objects.equals(personasMaximo, anterior.personasMaximo) || !peligros.equals(anterior.peligros);
	}

}
