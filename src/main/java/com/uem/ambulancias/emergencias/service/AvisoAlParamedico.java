package com.uem.ambulancias.emergencias.service;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.function.BiConsumer;

import com.uem.ambulancias.emergencias.domain.CentroSalud;
import com.uem.ambulancias.emergencias.domain.Traslado;
import com.uem.ambulancias.emergencias.repository.TrasladoRepository;
import com.uem.ambulancias.flota.repository.TurnoRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Le avisa por push a la tripulación de la unidad, después del commit, del traslado que le tocó y del que le
 * sacaron. Sin esto, la asignación solo se ve entrando a la app, y el que maneja no tiene por qué estar mirándola.
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
	private final TurnoRepository turnos;
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

	private void avisar(Long trasladoId, Long ambulanciaId, BiConsumer<List<String>, AvisoDeTraslado> enviar) {
		try {
			List<String> tokens = turnos.buscarAbiertosConAvisoPorAmbulancias(List.of(ambulanciaId)).stream()
					.map(turno -> turno.getParamedico().getTokenPush())
					.toList();
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
