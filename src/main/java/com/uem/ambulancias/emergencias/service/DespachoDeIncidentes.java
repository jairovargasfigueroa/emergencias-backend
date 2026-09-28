package com.uem.ambulancias.emergencias.service;

import java.util.List;

import com.uem.ambulancias.comun.error.CodigoError;
import com.uem.ambulancias.comun.error.ConflictoException;
import com.uem.ambulancias.comun.error.NoEncontradoException;
import com.uem.ambulancias.emergencias.domain.Atencion;
import com.uem.ambulancias.emergencias.domain.EstadoIncidente;
import com.uem.ambulancias.emergencias.domain.Incidente;
import com.uem.ambulancias.emergencias.domain.MotivoCierreIncidente;
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

	/**
	 * La central cierra una emergencia que no se va a atender: falsa alarma, ya la atendieron por otro medio, no hay
	 * cobertura u otro motivo. Solo sin unidades trabajándola: si hay alguna, el caso lo cierra ella con lo que
	 * encuentre. Queda registrado quién lo cerró, y el incidente sale del mapa de las unidades.
	 */
	@Transactional
	public void cerrar(Long incidenteId, Long administradorId, MotivoCierreIncidente motivo) {
		Usuario administrador = usuarios.findByIdAndRol(administradorId, RolUsuario.ADMIN)
				.orElseThrow(() -> new NoEncontradoException("No existe el administrador " + administradorId + "."));
		Incidente incidente = incidentes.buscarParaActualizar(incidenteId)
				.orElseThrow(() -> new NoEncontradoException("No existe el incidente " + incidenteId + "."));
		if (!incidente.getEstado().isAbierto()) {
			throw new ConflictoException(CodigoError.INCIDENTE_CERRADO, "El incidente ya está cerrado.");
		}
		if (atenciones.existeActivaPorIncidente(incidenteId)) {
			throw new ConflictoException(CodigoError.TRANSICION_INVALIDA,
					"Hay unidades trabajando en este incidente: lo cierran ellas, o cierra primero sus atenciones.");
		}
		incidente.cerrar(estadoPara(motivo), motivo, administrador);
		incidentes.save(incidente);
		eventos.publishEvent(new IncidenteActualizado(incidenteId, false));
	}

	/** Cómo queda el incidente según por qué lo cerró la central. */
	private static EstadoIncidente estadoPara(MotivoCierreIncidente motivo) {
		return switch (motivo) {
			case FALSA_ALARMA_VERIFICADA -> EstadoIncidente.FALSA_ALARMA;
			case ATENDIDO_EXTERNAMENTE -> EstadoIncidente.ATENDIDO_EXTERNAMENTE;
			case SIN_COBERTURA, OTRO -> EstadoIncidente.CANCELADO;
		};
	}

}
