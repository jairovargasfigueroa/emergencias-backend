package com.uem.ambulancias.emergencias.service;

import java.util.List;

import com.uem.ambulancias.emergencias.domain.EstadoAtencion;
import com.uem.ambulancias.emergencias.domain.Incidente;
import com.uem.ambulancias.emergencias.repository.AlertaRepository;
import com.uem.ambulancias.emergencias.repository.AtencionRepository;
import com.uem.ambulancias.emergencias.repository.IncidenteRepository;
import com.uem.ambulancias.flota.domain.Ambulancia;
import com.uem.ambulancias.flota.repository.AmbulanciaRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * SEC-A: difunde los incidentes después del commit. Lo ven todas las unidades disponibles y activas; si no hay
 * ninguna, se publica igual.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DifusionDeIncidentes {

	private final IncidenteRepository incidentes;
	private final AlertaRepository alertas;
	private final AtencionRepository atenciones;
	private final AmbulanciaRepository ambulancias;
	private final PublicadorTiempoReal publicador;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
	public void difundir(IncidenteActualizado evento) {
		try {
			incidentes.findById(evento.incidenteId()).ifPresent(this::publicar);
		} catch (RuntimeException e) {
			// El cambio ya está confirmado: un fallo al publicar no debe convertirse en un error de la petición.
			log.error("No se pudo difundir el incidente {}.", evento.incidenteId(), e);
		}
	}

	private void publicar(Incidente incidente) {
		if (!incidente.getEstado().isAbierto()) {
			publicador.retirarIncidente(incidente.getId());
			return;
		}

		Point ubicacion = incidente.getUbicacion();
		List<Ambulancia> disponiblesPorCercania =
				ambulancias.buscarDisponiblesPorCercania(ubicacion.getY(), ubicacion.getX());
		if (disponiblesPorCercania.isEmpty()) {
			log.warn("Incidente {} sin unidades disponibles: se publica igual.", incidente.getId());
		}

		publicador.publicarIncidenteAbierto(new IncidentePublicado(
				incidente.getId(),
				ubicacion.getY(),
				ubicacion.getX(),
				incidente.getEstado(),
				incidente.getFechaHoraCreacion(),
				incidente.getCantidadAfectados(),
				alertas.buscarDescripciones(incidente.getId()),
				atenciones.countByIncidenteIdAndEstadoIn(incidente.getId(), EstadoAtencion.ACTIVOS)));
	}

}
