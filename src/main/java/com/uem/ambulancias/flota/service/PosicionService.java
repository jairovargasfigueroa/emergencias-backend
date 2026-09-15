package com.uem.ambulancias.flota.service;

import java.time.Instant;

import com.uem.ambulancias.comun.error.CodigoError;
import com.uem.ambulancias.comun.error.ConflictoException;
import com.uem.ambulancias.comun.geo.Geo;
import com.uem.ambulancias.flota.domain.Asignacion;
import com.uem.ambulancias.flota.repository.AmbulanciaRepository;
import com.uem.ambulancias.flota.repository.AsignacionRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PosicionService {

	private final ServicioParamedicoService servicioParamedico;
	private final AsignacionRepository asignaciones;
	private final AmbulanciaRepository ambulancias;
	private final PosicionProperties propiedades;
	private final ApplicationEventPublisher eventos;

	/**
	 * PB-03 R4 y R5: la posición del paramédico en servicio es la de su ambulancia. Se publica en tiempo real y se
	 * guarda como última posición si la anterior tiene más de {@code sga.posicion.persistir-cada-seg}.
	 */
	@Transactional
	public void registrar(Long paramedicoId, double latitud, double longitud) {
		servicioParamedico.buscarParamedicoActivo(paramedicoId);
		Asignacion vigente = asignaciones.buscarVigentePorParamedico(paramedicoId)
				.filter(asignacion -> asignacion.getAmbulancia().isActiva())
				.orElseThrow(() -> new ConflictoException(CodigoError.SIN_SERVICIO,
						"El paramédico no tiene una ambulancia activa asignada."));

		Long ambulanciaId = vigente.getAmbulancia().getId();
		Instant ahora = Instant.now();
		ambulancias.guardarUltimaPosicion(ambulanciaId, Geo.punto(latitud, longitud), ahora,
				ahora.minusSeconds(propiedades.persistirCadaSeg()));
		eventos.publishEvent(new PosicionActualizada(ambulanciaId, latitud, longitud, ahora));
	}

}
