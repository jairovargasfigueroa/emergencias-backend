package com.uem.ambulancias.emergencias.service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.uem.ambulancias.emergencias.domain.Atencion;
import com.uem.ambulancias.emergencias.domain.Incidente;
import com.uem.ambulancias.emergencias.repository.AlertaRepository;
import com.uem.ambulancias.emergencias.repository.AtencionRepository;
import com.uem.ambulancias.emergencias.repository.IncidenteRepository;
import com.uem.ambulancias.flota.domain.Ambulancia;
import com.uem.ambulancias.flota.domain.Turno;
import com.uem.ambulancias.flota.repository.AmbulanciaRepository;
import com.uem.ambulancias.flota.repository.TurnoRepository;

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
 * ninguna, se publica igual. Un incidente que espera unidad (recién creado o que se quedó sin ninguna) además llega
 * por push a los paramédicos de turno, del más cercano al más lejano.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DifusionDeIncidentes {

	private final IncidenteRepository incidentes;
	private final AlertaRepository alertas;
	private final AtencionRepository atenciones;
	private final AmbulanciaRepository ambulancias;
	private final TurnoRepository turnos;
	private final PublicadorDeIncidentes publicador;
	private final NotificadorPush notificador;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
	public void difundir(IncidenteActualizado evento) {
		try {
			incidentes.findById(evento.incidenteId()).ifPresent(incidente -> publicar(incidente, evento.avisar()));
		} catch (RuntimeException e) {
			// El cambio ya está confirmado: un fallo al publicar no debe convertirse en un error de la petición.
			log.error("No se pudo difundir el incidente {}.", evento.incidenteId(), e);
		}
	}

	private void publicar(Incidente incidente, boolean avisar) {
		boolean abierto = incidente.getEstado().isAbierto();
		List<Atencion> activas = abierto ? atenciones.buscarActivasPorIncidente(incidente.getId()) : List.of();
		publicador.publicarSeguimiento(seguimiento(incidente, activas));

		if (!abierto) {
			publicador.retirarIncidente(incidente.getId());
			return;
		}

		Point ubicacion = incidente.getUbicacion();
		List<Ambulancia> disponiblesPorCercania =
				ambulancias.buscarDisponiblesPorCercania(ubicacion.getY(), ubicacion.getX());
		if (disponiblesPorCercania.isEmpty()) {
			log.warn("Incidente {} sin unidades disponibles: se publica igual.", incidente.getId());
		}

		IncidentePublicado publicado = new IncidentePublicado(
				incidente.getId(),
				ubicacion.getY(),
				ubicacion.getX(),
				incidente.getEstado(),
				incidente.getFechaHoraCreacion(),
				incidente.getCantidadAfectados(),
				alertas.buscarDescripciones(incidente.getId()),
				activas.size());
		publicador.publicarIncidenteAbierto(publicado);

		if (avisar) {
			notificador.notificarNuevoIncidente(tokensPorCercania(disponiblesPorCercania), publicado);
		}
	}

	private static SeguimientoPublicado seguimiento(Incidente incidente, List<Atencion> activas) {
		List<SeguimientoPublicado.Unidad> unidades = activas.stream()
				.map(atencion -> {
					Ambulancia ambulancia = atencion.getAmbulancia();
					Point posicion = ambulancia.getUltimaPosicion();
					return new SeguimientoPublicado.Unidad(
							ambulancia.getId(),
							ambulancia.getPlaca(),
							atencion.getEstado(),
							posicion == null ? null : posicion.getY(),
							posicion == null ? null : posicion.getX(),
							ambulancia.getUltimaPosicionEn());
				})
				.toList();
		return new SeguimientoPublicado(incidente.getId(), incidente.getEstado(), unidades);
	}

	/**
	 * Tokens push de quienes están de turno en esas ambulancias, respetando el orden por cercanía. Se mira el turno y
	 * no la asignación: con el compañero trabajando la unidad figura disponible, pero el que está en su casa no trabaja.
	 */
	private List<String> tokensPorCercania(List<Ambulancia> disponiblesPorCercania) {
		if (disponiblesPorCercania.isEmpty()) {
			return List.of();
		}
		Map<Long, Integer> posicionPorAmbulancia = new HashMap<>();
		for (int i = 0; i < disponiblesPorCercania.size(); i++) {
			posicionPorAmbulancia.put(disponiblesPorCercania.get(i).getId(), i);
		}
		return turnos.buscarAbiertosConAvisoPorAmbulancias(posicionPorAmbulancia.keySet()).stream()
				.sorted(Comparator.comparing((Turno turno) -> posicionPorAmbulancia.get(turno.getAmbulancia().getId())))
				.map(turno -> turno.getParamedico().getTokenPush())
				.distinct()
				.toList();
	}

}
