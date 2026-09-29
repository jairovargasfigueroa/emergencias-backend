package com.uem.ambulancias.emergencias.service;

import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import com.uem.ambulancias.comun.geo.Geo;
import com.uem.ambulancias.flota.domain.Ambulancia;
import com.uem.ambulancias.flota.repository.AmbulanciaRepository;

import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Component;

/**
 * Las unidades que el administrador puede mandar a mano, de la más cercana a la más lejana. Es lo mismo para un
 * traslado que para una emergencia: cambian qué unidades sirven y cuál es el punto.
 */
@Component
@RequiredArgsConstructor
public class CandidatasCercanas {

	private final AmbulanciaRepository ambulancias;
	private final TrasladoProperties config;

	/**
	 * Las disponibles y activas que cumplen {@code sirve}. Van también las que el sistema no elegiría solo —sin
	 * posición reciente, o que ya estuvieron en el caso—, marcadas: quien manda a mano puede saber algo que el
	 * sistema no.
	 */
	public List<UnidadCandidata> alrededorDe(Point punto, Predicate<Ambulancia> sirve, Collection<Long> yaEstuvieron) {
		Set<Long> estuvieron = Set.copyOf(yaEstuvieron);
		Instant posicionDesde = Instant.now().minus(config.posicionVigente());
		return ambulancias.findAllByOrderByPlacaAsc().stream()
				.filter(Ambulancia::puedeAtender)
				.filter(sirve)
				.map(unidad -> new UnidadCandidata(unidad,
						unidad.getUltimaPosicion() == null ? null : Geo.metrosEntre(unidad.getUltimaPosicion(), punto),
						unidad.getUltimaPosicionEn() != null && !unidad.getUltimaPosicionEn().isBefore(posicionDesde),
						estuvieron.contains(unidad.getId())))
				.sorted(Comparator.comparing(UnidadCandidata::distanciaMetros,
						Comparator.nullsLast(Comparator.naturalOrder())))
				.toList();
	}

}
