package com.uem.ambulancias.emergencias.service;

import java.util.List;
import java.util.Optional;

import com.uem.ambulancias.comun.error.CodigoError;
import com.uem.ambulancias.comun.error.ConflictoException;
import com.uem.ambulancias.comun.error.NoEncontradoException;
import com.uem.ambulancias.emergencias.domain.Atencion;
import com.uem.ambulancias.emergencias.domain.CentroSalud;
import com.uem.ambulancias.emergencias.domain.EstadoAtencion;
import com.uem.ambulancias.emergencias.domain.EstadoIncidente;
import com.uem.ambulancias.emergencias.domain.Incidente;
import com.uem.ambulancias.emergencias.domain.MotivoCancelacionAtencion;
import com.uem.ambulancias.emergencias.domain.MotivoCierreIncidente;
import com.uem.ambulancias.emergencias.domain.MotivoSinTraslado;
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
	private final IncidenteRepository incidentes;
	private final AmbulanciaRepository ambulancias;
	private final CentroSaludRepository centrosSalud;
	private final ServicioParamedicoService servicioParamedico;
	private final ApplicationEventPublisher eventos;

	/**
	 * La atención que tiene ocupada a la ambulancia del paramédico, si tiene una. Incluye la que ya entregó al
	 * paciente y todavía no se liberó: la unidad sigue tomada y esa pantalla es la que ofrece liberarse.
	 */
	@Transactional(readOnly = true)
	public Optional<Atencion> atencionActiva(Long paramedicoId) {
		return atenciones.buscarQueOcupaAmbulancia(servicioParamedico.ambulanciaAsignada(paramedicoId));
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
			return evaluarIncidente(incidente);
		});
	}

	/** La unidad termina de entregar, limpia y queda libre (M2). Recién acá puede recibir otra emergencia. */
	@Transactional
	public Atencion liberar(Long atencionId, Long paramedicoId) {
		return aplicar(atencionId, paramedicoId, false, (atencion, incidente) -> {
			atencion.liberar();
			ambulancias.actualizarEstado(atencion.getAmbulancia().getId(), EstadoAmbulancia.DISPONIBLE);
			return false;
		});
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
			return evaluarIncidente(incidente);
		});
	}

	/** ME-1 A4. Cascada: FUERA_DE_SERVICIO si es por avería (M3), si no DISPONIBLE (M2); luego se evalúa el incidente. */
	@Transactional
	public Atencion cancelar(Long atencionId, Long paramedicoId, MotivoCancelacionAtencion motivo) {
		return aplicar(atencionId, paramedicoId, true, (atencion, incidente) -> {
			atencion.cancelar(motivo);
			ambulancias.actualizarEstado(atencion.getAmbulancia().getId(),
					motivo == MotivoCancelacionAtencion.AVERIA ? EstadoAmbulancia.FUERA_DE_SERVICIO : EstadoAmbulancia.DISPONIBLE);
			return evaluarIncidente(incidente);
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
		Long incidenteId = atenciones.buscarIncidenteId(atencionId)
				.orElseThrow(() -> new NoEncontradoException("No existe la atención " + atencionId + "."));
		Incidente incidente = incidentes.buscarParaActualizar(incidenteId)
				.orElseThrow(() -> new NoEncontradoException("No existe el incidente " + incidenteId + "."));
		Atencion atencion = atenciones.findById(atencionId)
				.orElseThrow(() -> new NoEncontradoException("No existe la atención " + atencionId + "."));

		Long ambulanciaDelParamedico = servicioParamedico.ambulanciaAsignada(paramedicoId);
		if (!atencion.getAmbulancia().getId().equals(ambulanciaDelParamedico)) {
			throw new ConflictoException(CodigoError.ATENCION_AJENA,
					"La atención " + atencionId + " no es de la ambulancia del paramédico.");
		}

		boolean sinUnidades = cambio.ejecutar(atencion, incidente);
		if (difundir) {
			// Un solo evento por operación: si el incidente volvió a ACTIVO, ese mismo evento pide avisar a las unidades.
			eventos.publishEvent(new IncidenteActualizado(incidenteId, sinUnidades));
		}
		return atencion;
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
