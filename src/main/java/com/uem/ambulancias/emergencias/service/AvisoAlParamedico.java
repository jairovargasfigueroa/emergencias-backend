package com.uem.ambulancias.emergencias.service;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import com.uem.ambulancias.emergencias.domain.CentroSalud;
import com.uem.ambulancias.emergencias.domain.Traslado;
import com.uem.ambulancias.emergencias.repository.AlertaRepository;
import com.uem.ambulancias.emergencias.repository.AtencionRepository;
import com.uem.ambulancias.emergencias.repository.IncidenteRepository;
import com.uem.ambulancias.emergencias.repository.TrasladoRepository;
import com.uem.ambulancias.flota.domain.EstadoAmbulancia;
import com.uem.ambulancias.flota.repository.AmbulanciaRepository;
import com.uem.ambulancias.flota.repository.TurnoRepository;
import com.uem.ambulancias.flota.service.NovedadDeLaUnidad;
import com.uem.ambulancias.flota.service.TurnoCerradoPorLaCentral;
import com.uem.ambulancias.usuarios.domain.Usuario;
import com.uem.ambulancias.usuarios.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Le avisa por push a la tripulación de la unidad, después del commit, del traslado que le tocó y del que le
 * sacaron, y de lo que la central le cambió sin que ella lo pidiera. Sin esto, esos cambios solo se ven entrando a la
 * app, y el que maneja no tiene por qué estar mirándola.
 *
 * <p>El aviso va a todos los que están de turno en la unidad, no solo al responsable: salen juntos, y el que
 * maneja puede no ser el que tiene el teléfono a mano.
 *
 * <p>Se lee el traslado de nuevo en vez de arrastrarlo en el evento: sus datos cuelgan de asociaciones perezosas
 * y acá ya no queda la sesión que las abrió.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AvisoAlParamedico {

	private final TrasladoRepository traslados;
	private final IncidenteRepository incidentes;
	private final AlertaRepository alertas;
	private final AtencionRepository atenciones;
	private final TurnoRepository turnos;
	private final AmbulanciaRepository ambulancias;
	private final UsuarioRepository usuarios;
	private final NotificadorPush notificador;
	private final TrasladoProperties config;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
	public void avisarAsignacion(TrasladoAsignado evento) {
		avisar(evento.trasladoId(), evento.ambulanciaId(), notificador::notificarTrasladoAsignado);
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
	public void avisarRetiro(TrasladoRetirado evento) {
		avisar(evento.trasladoId(), evento.ambulanciaId(),
				(tokens, aviso) -> notificador.notificarTrasladoRetirado(tokens, aviso, evento.motivo()));
	}

	/** La central los mandó a una emergencia: no la eligieron ellos, así que no tienen por qué estar mirando la app. */
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
	public void avisarDespacho(IncidenteDespachado evento) {
		try {
			List<String> tokens = tokensDeLaTripulacion(evento.ambulanciaId());
			if (tokens.isEmpty()) {
				log.info("Nadie de turno en la ambulancia {} tiene un teléfono registrado: el incidente {} solo se "
						+ "verá en la app.", evento.ambulanciaId(), evento.incidenteId());
				return;
			}
			incidentes.findById(evento.incidenteId()).ifPresent(incidente -> notificador.notificarIncidenteAsignado(
					tokens, new IncidentePublicado(incidente.getId(), incidente.getUbicacion().getY(),
							incidente.getUbicacion().getX(), incidente.getEstado(), incidente.getFechaHoraCreacion(),
							incidente.getCantidadAfectados(), alertas.buscarDescripciones(incidente.getId()),
							atenciones.buscarActivasPorIncidente(incidente.getId()).size())));
		} catch (RuntimeException e) {
			log.error("No se pudo avisar del incidente {} a la ambulancia {}.", evento.incidenteId(),
					evento.ambulanciaId(), e);
		}
	}

	/**
	 * La central le cambió algo a la unidad sin que la tripulación lo pidiera: le cerró la atención, la sacó de
	 * servicio o la volvió a poner. Con la app abierta ya lo ve en vivo; el aviso es para cuando no la está mirando.
	 */
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
	public void avisarNovedad(NovedadDeLaUnidad novedad) {
		try {
			List<String> tokens = tokensDeLaTripulacion(novedad.ambulanciaId());
			if (tokens.isEmpty()) {
				log.info("Nadie de turno en la ambulancia {} tiene un teléfono registrado: {} solo se verá en la app.",
						novedad.ambulanciaId(), novedad.tipo());
				return;
			}
			// Cómo quedó se lee después del cambio: la central decide si la deja disponible o fuera de servicio.
			ambulancias.findById(novedad.ambulanciaId()).ifPresent(ambulancia -> {
				String titulo = tituloDe(novedad.tipo());
				String cuerpo = cuerpoDe(novedad.tipo(), ambulancia.getEstado());
				notificador.notificarParamedicos(tokens.stream()
						.map(token -> new AvisoParaParamedico(token, titulo, cuerpo,
								Map.of("tipo", novedad.tipo().name())))
						.toList());
			});
		} catch (RuntimeException e) {
			log.error("No se pudo avisar a la ambulancia {} de {}.", novedad.ambulanciaId(), novedad.tipo(), e);
		}
	}

	/** Le cerraron el turno desde la central: sin el aviso sigue creyendo que está trabajando. */
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
	public void avisarCierreDeTurno(TurnoCerradoPorLaCentral evento) {
		try {
			usuarios.findById(evento.paramedicoId())
					.map(Usuario::getTokenPush)
					.filter(token -> !token.isBlank())
					.ifPresent(token -> notificador.notificarParamedicos(List.of(new AvisoParaParamedico(token,
							"La central cerró tu turno", "Ya no estás de turno ni compartes tu ubicación.",
							Map.of("tipo", "TURNO_CERRADO_POR_CENTRAL")))));
		} catch (RuntimeException e) {
			log.error("No se pudo avisar al paramédico {} que le cerraron el turno.", evento.paramedicoId(), e);
		}
	}

	private static String tituloDe(NovedadDeLaUnidad.Tipo tipo) {
		return switch (tipo) {
			case ATENCION_CANCELADA_POR_CENTRAL -> "La central canceló tu atención";
			case ATENCION_ENTREGADA_POR_CENTRAL -> "La central dio por entregado al paciente";
			case UNIDAD_LIBERADA_POR_CENTRAL -> "La central liberó tu unidad";
			case FUERA_DE_SERVICIO -> "Tu unidad quedó fuera de servicio";
			case REACTIVADA -> "Tu unidad volvió a servicio";
		};
	}

	/** Solo lo cierto: si en el medio le asignaron otra cosa, no se promete que quedó libre. */
	private static String cuerpoDe(NovedadDeLaUnidad.Tipo tipo, EstadoAmbulancia estado) {
		return switch (tipo) {
			case FUERA_DE_SERVICIO -> "La marcó la central. Mientras siga así, no vas a recibir emergencias.";
			case REACTIVADA -> "La reactivó la central. Ya puedes recibir emergencias.";
			default -> switch (estado) {
				case DISPONIBLE -> "Tu unidad quedó disponible.";
				case FUERA_DE_SERVICIO -> "Tu unidad quedó fuera de servicio.";
				default -> "Revisa en la app cómo quedó tu unidad.";
			};
		};
	}

	private List<String> tokensDeLaTripulacion(Long ambulanciaId) {
		return turnos.buscarAbiertosConAvisoPorAmbulancias(List.of(ambulanciaId)).stream()
				.map(turno -> turno.getParamedico().getTokenPush())
				.toList();
	}

	private void avisar(Long trasladoId, Long ambulanciaId, BiConsumer<List<String>, AvisoDeTraslado> enviar) {
		try {
			List<String> tokens = tokensDeLaTripulacion(ambulanciaId);
			if (tokens.isEmpty()) {
				// Nadie abrió la app en un teléfono todavía: no hay a dónde mandarlo y no es un error.
				log.info("Nadie de turno en la ambulancia {} tiene un teléfono registrado: el traslado {} solo se "
						+ "verá en la app.", ambulanciaId, trasladoId);
				return;
			}
			traslados.findById(trasladoId).ifPresent(traslado -> enviar.accept(tokens, armar(traslado)));
		} catch (RuntimeException e) {
			// El cambio ya está confirmado: que falle el aviso no debe convertirse en un error de la petición.
			log.error("No se pudo avisar del traslado {} a la ambulancia {}.", trasladoId, ambulanciaId, e);
		}
	}

	private AvisoDeTraslado armar(Traslado traslado) {
		Instant horaCita = traslado.getHoraCita();
		CentroSalud centro = traslado.getCentroSaludDestino();
		return new AvisoDeTraslado(
				traslado.getId(),
				traslado.getPasajero().getNombreCompleto(),
				horaCita == null ? null : LocalTime.ofInstant(horaCita, ZoneId.of(config.zona())),
				// Sin centro de salud el destino es un punto del mapa: lo único que se puede nombrar es el detalle.
				centro != null ? centro.getNombre() : traslado.getDestinoDetalle(),
				traslado.getOrigenReferencia());
	}

}
