package com.uem.ambulancias.emergencias.service;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;

import com.uem.ambulancias.emergencias.domain.CentroSalud;
import com.uem.ambulancias.emergencias.domain.Traslado;
import com.uem.ambulancias.emergencias.repository.TrasladoRepository;
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
 * Le avisa por push al paramédico responsable del traslado que le tocó, después del commit. Sin esto la
 * asignación solo se ve entrando a la app, y el que maneja no tiene por qué estar mirándola.
 *
 * <p>Se lee el traslado de nuevo en vez de arrastrarlo en el evento: sus datos cuelgan de asociaciones perezosas
 * y acá ya no queda la sesión que las abrió.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AvisoAlParamedico {

	private final TrasladoRepository traslados;
	private final UsuarioRepository usuarios;
	private final NotificadorPush notificador;
	private final TrasladoProperties config;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
	public void avisar(TrasladoAsignado evento) {
		try {
			String token = usuarios.findById(evento.paramedicoId()).map(Usuario::getTokenPush).orElse(null);
			if (token == null) {
				// Todavía no abrió la app en ningún teléfono: no hay a dónde mandarlo y no es un error.
				log.info("El paramédico {} no tiene dispositivo registrado: el traslado {} solo se verá en la app.",
						evento.paramedicoId(), evento.trasladoId());
				return;
			}
			traslados.findById(evento.trasladoId())
					.ifPresent(traslado -> notificador.notificarTrasladoAsignado(token, armar(traslado)));
		} catch (RuntimeException e) {
			// La asignación ya está confirmada: que falle el aviso no debe convertirse en un error de la petición.
			log.error("No se pudo avisar del traslado {} al paramédico {}.", evento.trasladoId(),
					evento.paramedicoId(), e);
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
