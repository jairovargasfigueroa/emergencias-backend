package com.uem.ambulancias.emergencias.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.uem.ambulancias.comun.error.CodigoError;
import com.uem.ambulancias.comun.error.ConflictoException;
import com.uem.ambulancias.comun.error.NoEncontradoException;
import com.uem.ambulancias.emergencias.domain.Atencion;
import com.uem.ambulancias.emergencias.domain.CentroSalud;
import com.uem.ambulancias.emergencias.domain.EstadoAtencion;
import com.uem.ambulancias.emergencias.domain.EstadoIncidente;
import com.uem.ambulancias.emergencias.domain.EstadoTraslado;
import com.uem.ambulancias.emergencias.domain.Incidente;
import com.uem.ambulancias.emergencias.domain.MotivoCancelacionAtencion;
import com.uem.ambulancias.emergencias.domain.MotivoCierreIncidente;
import com.uem.ambulancias.emergencias.domain.MotivoSinTraslado;
import com.uem.ambulancias.emergencias.domain.Movilidad;
import com.uem.ambulancias.emergencias.domain.Traslado;
import com.uem.ambulancias.emergencias.repository.AlertaRepository;
import com.uem.ambulancias.emergencias.repository.AtencionRepository;
import com.uem.ambulancias.emergencias.repository.CentroSaludRepository;
import com.uem.ambulancias.emergencias.repository.IncidenteRepository;
import com.uem.ambulancias.flota.domain.EstadoAmbulancia;
import com.uem.ambulancias.flota.repository.AmbulanciaRepository;
import com.uem.ambulancias.flota.service.ServicioParamedicoService;

import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Point;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * PB-05: hitos, cancelación y datos del paciente de la atención, con sus cascadas de ME-1.
 */
@Service
@RequiredArgsConstructor
public class AtencionService {

	private final AtencionRepository atenciones;
	private final AlertaRepository alertas;
	private final IncidenteRepository incidentes;
	private final AmbulanciaRepository ambulancias;
	private final CentroSaludRepository centrosSalud;
	private final ServicioParamedicoService servicioParamedico;
	private final SelectorDeUnidad selector;
	private final ApplicationEventPublisher eventos;

	/**
	 * Si todos los que pidieron la ambulancia para este incidente ya retiraron su pedido. No cancela nada por sí
	 * solo: es lo que la unidad en camino necesita saber para decidir si sigue o se vuelve.
	 */
	@Transactional(readOnly = true)
	public boolean emisoresCancelaron(Long incidenteId) {
		return !alertas.existeAlgunaVigente(incidenteId);
	}

	/**
	 * La atención que tiene ocupada a la ambulancia del paramédico, si tiene una. Incluye la que ya entregó al
	 * paciente y todavía no se liberó: la unidad sigue tomada y esa pantalla es la que ofrece liberarse.
	 */
	@Transactional(readOnly = true)
	public Optional<Atencion> atencionActiva(Long paramedicoId) {
		return atenciones.buscarQueOcupaAmbulancia(servicioParamedico.ambulanciaAsignada(paramedicoId));
	}

	/** Los traslados que hizo este paramédico: lo que ve en su historial. */
	@Transactional(readOnly = true)
	public List<Atencion> trasladosDe(Long paramedicoId) {
		return atenciones.buscarTrasladosDeParamedico(paramedicoId);
	}

	/** ME-1 A1. */
	@Transactional
	public Atencion marcarLlegada(Long atencionId, Long paramedicoId, Point ubicacion) {
		return aplicar(atencionId, paramedicoId, true, (atencion, incidente) -> {
			atencion.marcarHito(EstadoAtencion.EN_EL_LUGAR, ubicacion);
			return false;
		});
	}

	/** ME-1 A2. Si llegan datos del paciente, se guardan en la atención; los que no llegan se conservan. */
	@Transactional
	public Atencion marcarRecogida(Long atencionId, Long paramedicoId, Point ubicacion, String nombrePaciente,
			String documentoPaciente) {
		return aplicar(atencionId, paramedicoId, true, (atencion, incidente) -> {
			atencion.marcarHito(EstadoAtencion.PACIENTE_RECOGIDO, ubicacion);
			if (nombrePaciente != null || documentoPaciente != null) {
				atencion.actualizarPaciente(
						nombrePaciente != null ? nombrePaciente : atencion.getNombrePaciente(),
						documentoPaciente != null ? documentoPaciente : atencion.getDocumentoPaciente());
			}
			return false;
		});
	}

	/** Llegada al centro de salud con el paciente a bordo. No cambia nada de la unidad: sigue ocupada. */
	@Transactional
	public Atencion marcarLlegadaAlHospital(Long atencionId, Long paramedicoId, Point ubicacion) {
		return aplicar(atencionId, paramedicoId, true, (atencion, incidente) -> {
			atencion.marcarHito(EstadoAtencion.EN_HOSPITAL, ubicacion);
			return false;
		});
	}

	/**
	 * La unidad fue y no trasladó a nadie: lo atendió en el lugar, el paciente se negó, no había nadie, ya se lo
	 * habían llevado o falleció. Es un desenlace normal, no una cancelación, y el motivo decide cómo cierra el
	 * incidente. La unidad sigue ocupada hasta que se libere.
	 */
	@Transactional
	public Atencion cerrarSinTraslado(Long atencionId, Long paramedicoId, Point ubicacion, MotivoSinTraslado motivo) {
		return aplicar(atencionId, paramedicoId, true, (atencion, incidente) -> {
			atencion.cerrarSinTraslado(motivo, ubicacion);
			return evaluarSiHayIncidente(incidente);
		});
	}

	/** La unidad termina de entregar, limpia y queda libre (M2). Recién acá puede recibir otra emergencia. */
	@Transactional
	public Atencion liberar(Long atencionId, Long paramedicoId) {
		Atencion atencion = aplicar(atencionId, paramedicoId, false, (a, incidente) -> {
			a.liberar();
			ambulancias.actualizarEstado(a.getAmbulancia().getId(), EstadoAmbulancia.DISPONIBLE);
			return false;
		});
		// Puede haber un traslado esperando justo esta unidad: mejor enterarse ahora que en el próximo barrido.
		eventos.publishEvent(new UnidadLiberada(atencion.getAmbulancia().getId()));
		return atencion;
	}

	/**
	 * Solo en traslados. La unidad llegó y el paciente no estaba listo: queda la marca con su hora y la atención
	 * sigue donde está. Si espera o se retira lo decide el paramédico después, y eso ya son otros botones.
	 */
	@Transactional
	public Atencion marcarPacienteNoListo(Long atencionId, Long paramedicoId) {
		return aplicar(atencionId, paramedicoId, false, (atencion, incidente) -> {
			exigirQueSeaTraslado(atencion);
			atencion.marcarPacienteNoListo(Instant.now());
			return false;
		});
	}

	/**
	 * Solo en traslados. El paciente no está como decía la ficha y esta unidad no lo puede llevar. El paramédico
	 * corrige lo que ve, el sistema vuelve a derivar el tipo que hace falta, y el pedido regresa a la cola con el
	 * requerimiento arreglado. Sin esto volvería a pedir el mismo tipo que acaba de fallar.
	 */
	@Transactional
	public Atencion cerrarPorUnidadQueNoCorresponde(Long atencionId, Long paramedicoId, Point ubicacion,
			Movilidad movilidad, boolean oxigeno, boolean equipo) {
		return aplicar(atencionId, paramedicoId, false, (atencion, incidente) -> {
			Traslado traslado = exigirQueSeaTraslado(atencion);
			traslado.corregirTipoUnidad(selector.sugerirPara(movilidad, oxigeno, equipo));
			atencion.cerrarSinTraslado(MotivoSinTraslado.UNIDAD_NO_CORRESPONDE, ubicacion);
			return false;
		});
	}

	private Traslado exigirQueSeaTraslado(Atencion atencion) {
		Traslado traslado = atencion.getTraslado();
		if (traslado == null) {
			throw new ConflictoException(CodigoError.VALIDACION, "Esta acción es solo de los traslados.");
		}
		return traslado;
	}

	/**
	 * ME-1 A3. La unidad NO queda libre acá: entregar al paciente termina el caso para el incidente, pero la
	 * ambulancia sigue ocupada hasta que el paramédico se libera.
	 */
	@Transactional
	public Atencion entregar(Long atencionId, Long paramedicoId, Point ubicacion, Long centroSaludId,
			String destinoDescripcion) {
		CentroSalud centroSalud = centroSaludId == null ? null : centrosSalud.findByIdAndActivoTrue(centroSaludId)
				.orElseThrow(() -> new NoEncontradoException("No existe el centro de salud " + centroSaludId + "."));
		return aplicar(atencionId, paramedicoId, true, (atencion, incidente) -> {
			atencion.entregar(ubicacion, centroSalud, destinoDescripcion);
			return evaluarSiHayIncidente(incidente);
		});
	}

	/** ME-1 A4. Cascada: FUERA_DE_SERVICIO si es por avería (M3), si no DISPONIBLE (M2); luego se evalúa el incidente. */
	@Transactional
	public Atencion cancelar(Long atencionId, Long paramedicoId, MotivoCancelacionAtencion motivo) {
		return aplicar(atencionId, paramedicoId, true, (atencion, incidente) -> {
			atencion.cancelar(motivo);
			ambulancias.actualizarEstado(atencion.getAmbulancia().getId(),
					motivo == MotivoCancelacionAtencion.AVERIA ? EstadoAmbulancia.FUERA_DE_SERVICIO : EstadoAmbulancia.DISPONIBLE);
			return evaluarSiHayIncidente(incidente);
		});
	}

	/** PB-05 R3: edición de los datos del paciente mientras la atención esté activa. */
	@Transactional
	public Atencion actualizarPaciente(Long atencionId, Long paramedicoId, String nombrePaciente,
			String documentoPaciente) {
		return aplicar(atencionId, paramedicoId, false, (atencion, incidente) -> {
			atencion.actualizarPaciente(nombrePaciente, documentoPaciente);
			return false;
		});
	}

	/**
	 * PB-05 R10: el cambio y su cascada se aplican con acceso exclusivo al incidente, así dos unidades del mismo
	 * incidente que entregan o cancelan a la vez no evalúan un conteo desactualizado. Solo se permite sobre la atención
	 * de la ambulancia del paramédico. La publicación ocurre después del commit, con un solo evento por operación.
	 */
	private Atencion aplicar(Long atencionId, Long paramedicoId, boolean difundir, CambioDeAtencion cambio) {
		Atencion atencion = atenciones.findById(atencionId)
				.orElseThrow(() -> new NoEncontradoException("No existe la atención " + atencionId + "."));

		// Una atención de traslado no cuelga de ningún incidente: no hay nada que bloquear ni que difundir.
		Long incidenteId = atenciones.buscarIncidenteId(atencionId).orElse(null);
		Incidente incidente = incidenteId == null ? null : incidentes.buscarParaActualizar(incidenteId)
				.orElseThrow(() -> new NoEncontradoException("No existe el incidente " + incidenteId + "."));

		Long ambulanciaDelParamedico = servicioParamedico.ambulanciaAsignada(paramedicoId);
		if (!atencion.getAmbulancia().getId().equals(ambulanciaDelParamedico)) {
			throw new ConflictoException(CodigoError.ATENCION_AJENA,
					"La atención " + atencionId + " no es de la ambulancia del paramédico.");
		}

		boolean sinUnidades = cambio.ejecutar(atencion, incidente);
		sincronizarTraslado(atencion);
		if (difundir && incidenteId != null) {
			// Un solo evento por operación: si el incidente volvió a ACTIVO, ese mismo evento pide avisar a las unidades.
			eventos.publishEvent(new IncidenteActualizado(incidenteId, sinUnidades));
		}
		return atencion;
	}

	/** Evaluar el cierre solo tiene sentido con incidente: un traslado no se comparte con otras unidades. */
	private boolean evaluarSiHayIncidente(Incidente incidente) {
		return incidente != null && evaluarIncidente(incidente);
	}

	/**
	 * El traslado sigue a su atención. Entregado es traslado cumplido; sin traslado es una salida que no llevó a
	 * nadie. Y cuando la unidad se cae —rechazo, avería, la unidad no correspondía— el pedido no muere: vuelve a
	 * la cola a buscar otra, porque la familia sigue necesitando el viaje.
	 */
	private void sincronizarTraslado(Atencion atencion) {
		Traslado traslado = atencion.getTraslado();
		if (traslado == null || traslado.getEstado() != EstadoTraslado.ASIGNADO) {
			return;
		}
		switch (atencion.getEstado()) {
			case PACIENTE_ENTREGADO -> traslado.completar();
			case SIN_TRASLADO -> {
				if (atencion.getMotivoSinTraslado() == MotivoSinTraslado.UNIDAD_NO_CORRESPONDE) {
					traslado.devolverABusqueda();
				} else {
					traslado.marcarNoRealizado();
				}
			}
			case CANCELADA -> {
				if (atencion.getMotivoCancelacion() != MotivoCancelacionAtencion.CANCELADA_POR_SOLICITANTE) {
					traslado.devolverABusqueda();
				}
			}
			default -> {
				// Los hitos intermedios no mueven el estado del pedido: sigue asignado hasta que termine.
			}
		}
	}

	/**
	 * estados.md, Cascadas: solo con el incidente EN_ATENCION y sin atenciones activas. Una entrega manda sobre todo
	 * (I3, ATENDIDO). Si nadie entregó pero alguien resolvió sin trasladar, el desenlace sale del motivo: nadie en el
	 * lugar es FALSA_ALARMA y ya se lo habían llevado es ATENDIDO_EXTERNAMENTE. Si no pasó ninguna, vuelve a ACTIVO (I2).
	 *
	 * @return si el incidente volvió a ACTIVO, o sea que quedó abierto y otra vez sin ninguna unidad en camino.
	 */
	private boolean evaluarIncidente(Incidente incidente) {
		if (incidente.getEstado() != EstadoIncidente.EN_ATENCION || atenciones.existeActivaPorIncidente(incidente.getId())) {
			return false;
		}
		if (atenciones.existsByIncidenteIdAndEstado(incidente.getId(), EstadoAtencion.PACIENTE_ENTREGADO)) {
			incidente.cambiarEstado(EstadoIncidente.ATENDIDO);
			incidentes.save(incidente);
			return false;
		}
		List<MotivoSinTraslado> sinTraslado = atenciones.buscarMotivosSinTraslado(incidente.getId());
		if (sinTraslado.isEmpty()) {
			// I2: nadie llegó a resolver nada, el incidente vuelve a esperar una unidad.
			incidente.cambiarEstado(EstadoIncidente.ACTIVO);
			incidentes.save(incidente);
			return true;
		}
		// Alguien fue y resolvió sin trasladar: el desenlace sale de lo que encontró en el lugar.
		if (sinTraslado.contains(MotivoSinTraslado.TRASLADO_POR_OTRO_MEDIO)) {
			incidente.cerrar(EstadoIncidente.ATENDIDO_EXTERNAMENTE, MotivoCierreIncidente.ATENDIDO_EXTERNAMENTE);
		} else if (sinTraslado.stream().allMatch(motivo -> motivo == MotivoSinTraslado.NO_HABIA_PACIENTE)) {
			incidente.cerrar(EstadoIncidente.FALSA_ALARMA, MotivoCierreIncidente.FALSA_ALARMA_VERIFICADA);
		} else {
			incidente.cambiarEstado(EstadoIncidente.ATENDIDO);
		}
		incidentes.save(incidente);
		return false;
	}

	/**
	 * Cambio sobre la atención y su cascada en el incidente. Devuelve si el incidente se quedó sin unidades y volvió a
	 * esperar una, para que la difusión avise a las disponibles.
	 */
	@FunctionalInterface
	private interface CambioDeAtencion {

		boolean ejecutar(Atencion atencion, Incidente incidente);

	}

}
