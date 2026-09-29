package com.uem.ambulancias.emergencias.service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.uem.ambulancias.emergencias.domain.Incidente;
import com.uem.ambulancias.emergencias.domain.MotivoCierreIncidente;
import com.uem.ambulancias.emergencias.domain.Traslado;
import com.uem.ambulancias.emergencias.repository.AlertaRepository;
import com.uem.ambulancias.emergencias.repository.EmisorConAviso;
import com.uem.ambulancias.emergencias.repository.IncidenteRepository;
import com.uem.ambulancias.emergencias.repository.TrasladoRepository;
import com.uem.ambulancias.usuarios.domain.Usuario;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Le avisa por push al ciudadano, después del commit, lo que tiene que saber de su pedido aunque tenga la app
 * cerrada. Con la app abierta ya lo ve en vivo: el aviso es para cuando no la está mirando.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AvisoAlCiudadano {

	private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

	private final AlertaRepository alertas;
	private final IncidenteRepository incidentes;
	private final TrasladoRepository traslados;
	private final NotificadorPush notificador;
	private final TrasladoProperties config;

	/** A todos los que avisaron del incidente y no retiraron su pedido, cada uno con su alerta. */
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
	public void avisar(NovedadDelIncidente novedad) {
		try {
			List<EmisorConAviso> emisores = alertas.buscarEmisoresConAviso(novedad.incidenteId());
			if (emisores.isEmpty()) {
				return;
			}
			Texto texto = textoDe(novedad);
			notificador.notificarCiudadanos(emisores.stream()
					.map(emisor -> new AvisoParaCiudadano(emisor.tokenPush(), texto.titulo(), texto.cuerpo(),
							Map.of("incidenteId", String.valueOf(novedad.incidenteId()),
									"alertaId", String.valueOf(emisor.alertaId()),
									"tipo", novedad.tipo().name())))
					.toList());
		} catch (RuntimeException e) {
			// El cambio ya está confirmado: que falle el aviso no debe convertirse en un error de la petición.
			log.error("No se pudo avisar a quienes pidieron la ambulancia del incidente {}.", novedad.incidenteId(), e);
		}
	}

	/** El traslado consiguió unidad: a quien lo pidió le sirve saber a qué hora pasan. */
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
	public void avisarAsignacion(TrasladoAsignado evento) {
		avisarDelTraslado(evento.trasladoId(), "UNIDAD_ASIGNADA");
	}

	/** A quien pidió el traslado, que puede no ser quien viaja: el pasajero muchas veces no tiene la app. */
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
	public void avisar(NovedadDelTraslado novedad) {
		avisarDelTraslado(novedad.trasladoId(), novedad.tipo().name());
	}

	private void avisarDelTraslado(Long trasladoId, String tipo) {
		try {
			traslados.findById(trasladoId).ifPresent(traslado -> {
				Usuario solicitante = traslado.getSolicitante();
				if (!solicitante.isActivo() || solicitante.getTokenPush() == null) {
					return;
				}
				Texto texto = textoDe(traslado, tipo);
				notificador.notificarCiudadanos(List.of(new AvisoParaCiudadano(solicitante.getTokenPush(),
						texto.titulo(), texto.cuerpo(),
						Map.of("trasladoId", String.valueOf(trasladoId), "tipo", tipo))));
			});
		} catch (RuntimeException e) {
			log.error("No se pudo avisar a quien pidió el traslado {}.", trasladoId, e);
		}
	}

	private Texto textoDe(Traslado traslado, String tipo) {
		String pasajero = traslado.getPasajero().getNombreCompleto();
		return switch (tipo) {
			case "UNIDAD_ASIGNADA" -> new Texto("Tu traslado tiene unidad",
					ventana(traslado).map(v -> "Pasamos a recoger a " + pasajero + " " + v + ".")
							.orElse("Una unidad va a buscar a " + pasajero + "."));
			case "UNIDAD_EN_LA_PUERTA" -> new Texto("La ambulancia llegó",
					"La unidad está en la puerta para buscar a " + pasajero + ".");
			case "NUEVA_BUSQUEDA" -> new Texto("Buscamos otra unidad",
					"La unidad no pudo hacer el traslado de " + pasajero + ". Ya buscamos otra"
							+ ventana(traslado).map(v -> ": pasamos " + v + ".").orElse("."));
			case "NO_CUBIERTO" -> new Texto("No conseguimos unidad",
					"No conseguimos una unidad a tiempo para el traslado de " + pasajero
							+ ". Si todavía lo necesitas, pídelo otra vez.");
			default -> new Texto("Traslado de mañana",
					ventana(traslado).map(v -> "Pasamos a buscar a " + pasajero + " " + v + ".")
							.orElse("Mañana pasamos a buscar a " + pasajero + ".")
							+ " Si ya no lo necesitas, cancélalo en la app.");
		};
	}

	/** "entre las 9:05 y las 9:25", en la hora de la empresa: el servidor puede estar en UTC y nadie vive en UTC. */
	private Optional<String> ventana(Traslado traslado) {
		Instant desde = traslado.getHoraRecogidaDesde();
		Instant hasta = traslado.getHoraRecogidaHasta();
		if (desde == null || hasta == null) {
			return Optional.empty();
		}
		ZoneId zona = ZoneId.of(config.zona());
		return Optional.of("entre las " + HORA.format(desde.atZone(zona)) + " y las "
				+ HORA.format(hasta.atZone(zona)));
	}

	private record Texto(String titulo, String cuerpo) {
	}

	private Texto textoDe(NovedadDelIncidente novedad) {
		return switch (novedad.tipo()) {
			case UNIDAD_EN_CAMINO -> new Texto("Una ambulancia va en camino",
					"Ya hay una unidad yendo a tu ubicación. Toca para ver por dónde viene.");
			case UNIDAD_LLEGO -> new Texto("La ambulancia llegó", "La unidad ya está en tu ubicación.");
			case BUSCANDO_OTRA_UNIDAD -> new Texto("Buscamos otra ambulancia",
					"La unidad que iba no puede llegar. Ya estamos buscando otra.");
			case CERRADO_POR_LA_CENTRAL -> new Texto("Tu pedido se cerró", cierre(novedad.incidenteId()));
		};
	}

	/** Sin cobertura se dice así, porque es lo que la persona necesita saber para buscar otra salida. */
	private String cierre(Long incidenteId) {
		boolean sinCobertura = incidentes.findById(incidenteId)
				.map(Incidente::getMotivoCierre)
				.filter(motivo -> motivo == MotivoCierreIncidente.SIN_COBERTURA)
				.isPresent();
		return sinCobertura
				? "No conseguimos una ambulancia. Si todavía necesitas ayuda, vuelve a pedirla."
				: "La central cerró tu pedido. Si todavía necesitas ayuda, vuelve a pedirla.";
	}

}
