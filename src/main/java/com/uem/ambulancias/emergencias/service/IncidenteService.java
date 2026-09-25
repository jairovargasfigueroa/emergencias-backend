package com.uem.ambulancias.emergencias.service;

import java.time.Instant;
import java.util.List;

import com.uem.ambulancias.comun.error.CodigoError;
import com.uem.ambulancias.comun.error.ConflictoException;
import com.uem.ambulancias.comun.error.NoEncontradoException;
import com.uem.ambulancias.emergencias.domain.Alerta;
import com.uem.ambulancias.emergencias.domain.Atencion;
import com.uem.ambulancias.emergencias.domain.EstadoIncidente;
import com.uem.ambulancias.emergencias.domain.Incidente;
import com.uem.ambulancias.emergencias.domain.MotivoCancelacionAlerta;
import com.uem.ambulancias.emergencias.exception.IncidenteYaTomadoException;
import com.uem.ambulancias.emergencias.repository.AlertaRepository;
import com.uem.ambulancias.emergencias.repository.AtencionRepository;
import com.uem.ambulancias.emergencias.repository.BloqueoDeAgrupacion;
import com.uem.ambulancias.emergencias.repository.IncidenteRepository;
import com.uem.ambulancias.flota.domain.Ambulancia;
import com.uem.ambulancias.flota.domain.EstadoAmbulancia;
import com.uem.ambulancias.flota.repository.AmbulanciaRepository;
import com.uem.ambulancias.usuarios.domain.Usuario;

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
	private final AtencionRepository atenciones;
	private final AmbulanciaRepository ambulancias;
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

		// Un incidente recién creado todavía no tiene unidad en camino: hay que avisar a las disponibles.
		eventos.publishEvent(new IncidenteActualizado(incidente.getId(), nuevo));
		return incidente;
	}

	/**
	 * Completa los datos opcionales de la alerta después de emitirla, mientras el ciudadano espera. Solo se aplican
	 * los campos que llegan (PB-02 R3) y el consolidado del incidente solo sube, por {@code consolidarAfectados}
	 * (PB-02 R6). Se hace con acceso exclusivo al incidente y se difunde después del commit, para que al paramédico
	 * se le actualice el dato sin refrescar.
	 */
	@Transactional
	public Alerta completarDetalles(Long alertaId, Long ciudadanoId, Integer cantidadAfectados, String descripcion) {
		Long incidenteId = alertas.buscarIncidenteId(alertaId)
				.orElseThrow(() -> new NoEncontradoException("No existe la alerta " + alertaId + "."));
		Incidente incidente = incidentes.buscarParaActualizar(incidenteId)
				.orElseThrow(() -> new NoEncontradoException("No existe el incidente " + incidenteId + "."));
		Alerta alerta = alertas.findById(alertaId)
				.orElseThrow(() -> new NoEncontradoException("No existe la alerta " + alertaId + "."));

		if (!alerta.getEmisor().getId().equals(ciudadanoId)) {
			throw new ConflictoException(CodigoError.ALERTA_AJENA,
					"La alerta " + alertaId + " no es del ciudadano " + ciudadanoId + ".");
		}
		if (!incidente.getEstado().isAbierto()) {
			throw new ConflictoException(CodigoError.DETALLES_NO_EDITABLES,
					"El incidente " + incidenteId + " ya está cerrado.");
		}
		if (atenciones.existeLlegadaPorIncidente(incidenteId)) {
			throw new ConflictoException(CodigoError.DETALLES_NO_EDITABLES,
					"Una unidad ya llegó al lugar del incidente " + incidenteId + ".");
		}

		alerta.completarDetalles(cantidadAfectados, descripcion);
		alertas.save(alerta);

		incidente.consolidarAfectados(cantidadAfectados);
		incidentes.save(incidente);

		eventos.publishEvent(new IncidenteActualizado(incidenteId, false));
		return alerta;
	}

	/**
	 * El ciudadano retira su pedido. Se puede hasta que una unidad llegue al lugar: desde ahí, lo que pasa lo decide
	 * quien está parado allí.
	 *
	 * <p>Retirar un pedido no es cerrar la emergencia. Si quedan otras alertas vivas, el incidente sigue. Si no queda
	 * ninguna pero ya hay una unidad en camino, tampoco se cierra: se le avisa y el paramédico decide si sigue o se
	 * vuelve, como en el despacho real. Solo se cierra cuando nadie más pidió y nadie salió todavía.
	 */
	@Transactional
	public Alerta cancelarAlerta(Long alertaId, Long ciudadanoId, MotivoCancelacionAlerta motivo,
			Boolean emisorEsPaciente) {
		Long incidenteId = alertas.buscarIncidenteId(alertaId)
				.orElseThrow(() -> new NoEncontradoException("No existe la alerta " + alertaId + "."));
		Incidente incidente = incidentes.buscarParaActualizar(incidenteId)
				.orElseThrow(() -> new NoEncontradoException("No existe el incidente " + incidenteId + "."));
		Alerta alerta = alertas.findById(alertaId)
				.orElseThrow(() -> new NoEncontradoException("No existe la alerta " + alertaId + "."));

		if (!alerta.getEmisor().getId().equals(ciudadanoId)) {
			throw new ConflictoException(CodigoError.ALERTA_AJENA,
					"La alerta " + alertaId + " no es del ciudadano " + ciudadanoId + ".");
		}
		if (!incidente.getEstado().isAbierto()) {
			throw new ConflictoException(CodigoError.TRANSICION_INVALIDA,
					"El incidente " + incidenteId + " ya está cerrado.");
		}
		if (atenciones.existeLlegadaPorIncidente(incidenteId)) {
			throw new ConflictoException(CodigoError.TRANSICION_INVALIDA,
					"Una unidad ya llegó al lugar del incidente " + incidenteId + ".");
		}

		alerta.cancelar(motivo, emisorEsPaciente);
		alertas.save(alerta);

		boolean quedanPedidos = alertas.existeAlgunaVigente(incidenteId);
		boolean hayUnidadEnCamino = atenciones.existeActivaPorIncidente(incidenteId);
		if (!quedanPedidos && !hayUnidadEnCamino) {
			incidente.cambiarEstado(EstadoIncidente.CANCELADO);
			incidentes.save(incidente);
		}

		eventos.publishEvent(new IncidenteActualizado(incidenteId, false));
		return alerta;
	}

	/**
	 * SEC-B.1. La primera unidad toma el incidente. Si ya hay una atención activa, lanza
	 * {@link IncidenteYaTomadoException} y no crea nada.
	 */
	@Transactional
	public Atencion tomar(Long idIncidente, Long idAmbulancia, Usuario paramedico) {
		return crearAtencion(idIncidente, idAmbulancia, paramedico, true);
	}

	/** SEC-B.1. Mismo camino que {@link #tomar} sin verificar si ya hay atenciones activas. */
	@Transactional
	public Atencion sumarse(Long idIncidente, Long idAmbulancia, Usuario paramedico) {
		return crearAtencion(idIncidente, idAmbulancia, paramedico, false);
	}

	/** Ambulancias que acuden al incidente (atenciones activas), para el contexto del 409. */
	@Transactional(readOnly = true)
	public List<Ambulancia> unidadesAcudiendo(Long idIncidente) {
		return atenciones.buscarActivasPorIncidente(idIncidente).stream().map(Atencion::getAmbulancia).toList();
	}

	/**
	 * Acceso exclusivo: se bloquea primero el incidente y después la ambulancia, siempre en ese orden. Las validaciones
	 * ocurren antes de crear la atención; si alguna falla, no se crea nada. Cascada de ME-1 en la misma transacción:
	 * A0, I1 (si el incidente estaba ACTIVO) y M1.
	 */
	private Atencion crearAtencion(Long idIncidente, Long idAmbulancia, Usuario paramedico, boolean esToma) {
		Incidente incidente = incidentes.buscarParaActualizar(idIncidente)
				.orElseThrow(() -> new NoEncontradoException("No existe el incidente " + idIncidente + "."));
		Ambulancia ambulancia = ambulancias.buscarParaActualizar(idAmbulancia)
				.orElseThrow(() -> new NoEncontradoException("No existe la ambulancia " + idAmbulancia + "."));

		if (!incidente.getEstado().isAbierto()) {
			throw new ConflictoException(CodigoError.INCIDENTE_CERRADO, "El incidente " + idIncidente + " ya está cerrado.");
		}
		if (!ambulancia.puedeAtender()) {
			throw new ConflictoException(CodigoError.AMBULANCIA_NO_DISPONIBLE,
					"La ambulancia " + ambulancia.getPlaca() + " no está disponible.");
		}
		if (esToma && atenciones.existeActivaPorIncidente(idIncidente)) {
			throw new IncidenteYaTomadoException(incidente);
		}

		Atencion atencion = atenciones.save(Atencion.iniciar(incidente, ambulancia, paramedico, Instant.now()));
		if (incidente.getEstado() == EstadoIncidente.ACTIVO) {
			incidente.cambiarEstado(EstadoIncidente.EN_ATENCION);
			incidentes.save(incidente);
		}
		ambulancias.actualizarEstado(idAmbulancia, EstadoAmbulancia.EN_ATENCION);

		eventos.publishEvent(new IncidenteActualizado(idIncidente, false));
		return atencion;
	}

}
