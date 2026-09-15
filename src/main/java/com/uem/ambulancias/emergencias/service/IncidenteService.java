package com.uem.ambulancias.emergencias.service;

import com.uem.ambulancias.emergencias.domain.Alerta;
import com.uem.ambulancias.emergencias.domain.Incidente;
import com.uem.ambulancias.emergencias.repository.AlertaRepository;
import com.uem.ambulancias.emergencias.repository.BloqueoDeAgrupacion;
import com.uem.ambulancias.emergencias.repository.IncidenteRepository;

import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Point;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IncidenteService {

	private final IncidenteRepository incidentes;
	private final AlertaRepository alertas;
	private final BloqueoDeAgrupacion bloqueoDeAgrupacion;
	private final AgrupacionProperties agrupacion;
	private final ApplicationEventPublisher eventos;

	/**
	 * SEC-A.1. Vincula la alerta al incidente abierto más cercano dentro del radio y la ventana, o crea uno nuevo
	 * con su ubicación efectiva. Todo ocurre con la agrupación serializada, desde la búsqueda hasta guardar la
	 * alerta. La difusión se dispara después del commit.
	 */
	@Transactional
	public Incidente agruparAlerta(Alerta alerta) {
		bloqueoDeAgrupacion.adquirir();

		Point ubicacion = alerta.getUbicacionEfectiva();
		Incidente incidente = incidentes
				.buscarActivoCercano(ubicacion, agrupacion.radioM(), agrupacion.ventanaMin())
				.orElse(null);
		boolean nuevo = incidente == null;
		if (nuevo) {
			incidente = Incidente.crear((Point) ubicacion.copy(), alerta.getCantidadAfectados(), alerta.getFechaHora());
		} else {
			incidente.consolidarAfectados(alerta.getCantidadAfectados());
		}
		incidentes.save(incidente);

		alerta.vincular(incidente);
		alertas.save(alerta);

		eventos.publishEvent(new IncidenteActualizado(incidente.getId(), nuevo));
		return incidente;
	}

}
