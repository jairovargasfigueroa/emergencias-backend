package com.uem.ambulancias.emergencias.service;

import java.util.List;
import java.util.Map;

import com.uem.ambulancias.emergencias.domain.Incidente;
import com.uem.ambulancias.emergencias.domain.MotivoCierreIncidente;
import com.uem.ambulancias.emergencias.repository.AlertaRepository;
import com.uem.ambulancias.emergencias.repository.EmisorConAviso;
import com.uem.ambulancias.emergencias.repository.IncidenteRepository;

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

	private final AlertaRepository alertas;
	private final IncidenteRepository incidentes;
	private final NotificadorPush notificador;

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
