package com.uem.ambulancias.emergencias.service;

import java.util.List;

import com.uem.ambulancias.comun.error.CodigoError;
import com.uem.ambulancias.comun.error.ConflictoException;
import com.uem.ambulancias.comun.error.NoEncontradoException;
import com.uem.ambulancias.emergencias.domain.Atencion;
import com.uem.ambulancias.emergencias.domain.Incidente;
import com.uem.ambulancias.emergencias.repository.AtencionRepository;
import com.uem.ambulancias.emergencias.repository.IncidenteRepository;
import com.uem.ambulancias.flota.domain.Turno;
import com.uem.ambulancias.flota.repository.TurnoRepository;
import com.uem.ambulancias.usuarios.domain.RolUsuario;
import com.uem.ambulancias.usuarios.domain.Usuario;
import com.uem.ambulancias.usuarios.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lo que hace la central con una emergencia, como el despachador de una central real. En el día a día las unidades
 * toman los incidentes solas; esto es para cuando nadie lo toma, o hace falta una unidad más.
 */
@Service
@RequiredArgsConstructor
public class DespachoDeIncidentes {

	private final IncidenteService incidenteService;
	private final IncidenteRepository incidentes;
	private final AtencionRepository atenciones;
	private final TurnoRepository turnos;
	private final UsuarioRepository usuarios;
	private final CandidatasCercanas candidatasCercanas;
	private final ApplicationEventPublisher eventos;

	/** Las unidades que se pueden mandar, de la más cercana al lugar a la más lejana. */
	@Transactional(readOnly = true)
	public List<UnidadCandidata> candidatas(Long incidenteId) {
		Incidente incidente = incidentes.findById(incidenteId)
				.orElseThrow(() -> new NoEncontradoException("No existe el incidente " + incidenteId + "."));
		return candidatasCercanas.alrededorDe(incidente.getUbicacion(), unidad -> true,
				atenciones.buscarAmbulanciasQueEstuvieron(incidenteId));
	}

	/**
	 * Manda la unidad al incidente. Responde por la atención quien entró primero al turno de esa unidad, igual que
	 * en un traslado, y la tripulación se entera por push.
	 */
	@Transactional
	public Atencion despachar(Long incidenteId, Long ambulanciaId, Long administradorId) {
		Usuario administrador = usuarios.findByIdAndRol(administradorId, RolUsuario.ADMIN)
				.orElseThrow(() -> new NoEncontradoException("No existe el administrador " + administradorId + "."));
		Usuario responsable = turnos.buscarAbiertosPorAmbulancias(List.of(ambulanciaId)).stream()
				.findFirst()
				.map(Turno::getParamedico)
				.orElseThrow(() -> new ConflictoException(CodigoError.AMBULANCIA_NO_DISPONIBLE,
						"Esa unidad no tiene a nadie de turno."));
		Atencion atencion = incidenteService.despachar(incidenteId, ambulanciaId, responsable, administrador);
		eventos.publishEvent(new IncidenteDespachado(incidenteId, ambulanciaId));
		return atencion;
	}

}
