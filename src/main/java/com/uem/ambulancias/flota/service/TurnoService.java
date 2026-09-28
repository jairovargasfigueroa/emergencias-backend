package com.uem.ambulancias.flota.service;

import java.time.Instant;
import java.util.Optional;

import com.uem.ambulancias.comun.error.CodigoError;
import com.uem.ambulancias.comun.error.ConflictoException;
import com.uem.ambulancias.comun.error.NoEncontradoException;
import com.uem.ambulancias.emergencias.repository.AtencionRepository;
import com.uem.ambulancias.flota.domain.Ambulancia;
import com.uem.ambulancias.flota.domain.EstadoAmbulancia;
import com.uem.ambulancias.flota.domain.Turno;
import com.uem.ambulancias.flota.repository.AmbulanciaRepository;
import com.uem.ambulancias.flota.repository.TurnoRepository;
import com.uem.ambulancias.usuarios.domain.Usuario;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Entrar y salir de turno. El turno es de la persona; la ambulancia solo hereda: queda disponible mientras haya
 * alguien adentro y cae a SIN_TURNO cuando sale el último. Nadie la cambia a mano, que es donde estos datos se
 * vuelven mentira.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TurnoService {

	private final TurnoRepository turnos;
	private final AmbulanciaRepository ambulancias;
	private final AtencionRepository atenciones;
	private final ServicioParamedicoService servicioParamedico;
	private final ApplicationEventPublisher eventos;

	/** El turno abierto del paramédico, si está trabajando ahora. */
	public Optional<Turno> turnoAbierto(Long paramedicoId) {
		return turnos.buscarAbiertoPorParamedico(paramedicoId);
	}

	/** Cuántos tienen turno abierto en esa unidad. */
	public long tripulantesEnTurno(Long ambulanciaId) {
		return turnos.contarAbiertosPorAmbulancia(ambulanciaId);
	}

	/**
	 * La ambulancia con la que el paramédico trabaja ahora: la de su turno abierto. Para acudir a un incidente o marcar
	 * un hito hay que estar de turno. Estar asignado no alcanza: con el compañero trabajando la unidad figura
	 * disponible, pero el que está en su casa no está trabajando.
	 */
	public Long ambulanciaEnTurno(Long paramedicoId) {
		return turnos.buscarAbiertoPorParamedico(paramedicoId)
				.map(turno -> turno.getAmbulancia().getId())
				.orElseThrow(() -> new ConflictoException(CodigoError.SIN_TURNO, "No estás de turno."));
	}

	/**
	 * Entra a trabajar con la ambulancia de su asignación vigente. Si la unidad está averiada no pasa a disponible: la
	 * avería manda sobre el turno, y él lo ve en pantalla.
	 */
	@Transactional
	public Turno iniciar(Long paramedicoId) {
		Usuario paramedico = servicioParamedico.buscarParamedicoActivo(paramedicoId);
		if (turnos.buscarAbiertoPorParamedico(paramedicoId).isPresent()) {
			throw new ConflictoException(CodigoError.TRANSICION_INVALIDA, "Ya tienes un turno abierto.");
		}
		Long ambulanciaId = servicioParamedico.ambulanciaAsignada(paramedicoId);
		Ambulancia ambulancia = ambulancias.buscarParaActualizar(ambulanciaId)
				.orElseThrow(() -> new NoEncontradoException("No existe la ambulancia " + ambulanciaId + "."));
		if (!ambulancia.isActiva()) {
			throw new ConflictoException(CodigoError.SIN_SERVICIO, "Tu ambulancia está dada de baja.");
		}

		Turno turno = turnos.save(Turno.iniciar(paramedico, ambulancia, Instant.now()));
		if (ambulancia.getEstado() == EstadoAmbulancia.SIN_TURNO) {
			ambulancia.cambiarEstado(EstadoAmbulancia.DISPONIBLE);
		}
		return turno;
	}

	/**
	 * Sale de turno. No se puede con una atención en curso: es la regla del despacho real, nadie se va dejando un caso
	 * abierto. La unidad cae a SIN_TURNO solo si era el último adentro y no está ocupada ni averiada.
	 */
	@Transactional
	public Turno terminar(Long paramedicoId) {
		Turno turno = turnos.buscarAbiertoPorParamedico(paramedicoId)
				.orElseThrow(() -> new ConflictoException(CodigoError.TRANSICION_INVALIDA, "No tienes un turno abierto."));
		if (atenciones.buscarQueOcupaAmbulancia(turno.getAmbulancia().getId()).isPresent()) {
			throw new ConflictoException(CodigoError.TRANSICION_INVALIDA,
					"Tienes una atención en curso: termínala antes de salir de turno.");
		}
		return cerrar(turno);
	}

	/**
	 * El administrador le cierra el turno a alguien que se fue sin cerrarlo, como hace el despachador de una central.
	 * Sin esto su unidad sigue figurando con gente adentro y a él le siguen llegando avisos. Valen las mismas reglas
	 * que cuando lo cierra él: con una atención en curso, primero hay que cerrar la atención.
	 */
	@Transactional
	public Turno terminarDesdeLaCentral(Long paramedicoId) {
		Turno turno = turnos.buscarAbiertoPorParamedico(paramedicoId)
				.orElseThrow(() -> new ConflictoException(CodigoError.TRANSICION_INVALIDA,
						"Ese paramédico no tiene un turno abierto."));
		if (atenciones.buscarQueOcupaAmbulancia(turno.getAmbulancia().getId()).isPresent()) {
			throw new ConflictoException(CodigoError.TRANSICION_INVALIDA,
					"Su unidad tiene una atención en curso: ciérrala antes de cerrarle el turno.");
		}
		return cerrar(turno);
	}

	private Turno cerrar(Turno turno) {
		Long ambulanciaId = turno.getAmbulancia().getId();
		turno.terminar(Instant.now());
		if (turnos.contarAbiertosPorAmbulancia(ambulanciaId) == 0) {
			Ambulancia ambulancia = ambulancias.buscarParaActualizar(ambulanciaId)
					.orElseThrow(() -> new NoEncontradoException("No existe la ambulancia " + ambulanciaId + "."));
			// Averiada se queda averiada: que se vaya la tripulación no la arregla.
			if (ambulancia.getEstado() == EstadoAmbulancia.DISPONIBLE) {
				ambulancia.cambiarEstado(EstadoAmbulancia.SIN_TURNO);
			}
			// Se avisa aunque la unidad quede averiada: de cualquier modo ya no hay quién reporte su posición.
			eventos.publishEvent(new UnidadSinTripulacion(ambulanciaId));
		}
		return turno;
	}

}
