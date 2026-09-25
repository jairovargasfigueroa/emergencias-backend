package com.uem.ambulancias.emergencias.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.uem.ambulancias.emergencias.domain.Traslado;
import com.uem.ambulancias.emergencias.repository.TrasladoRepository;
import com.uem.ambulancias.flota.domain.TipoUnidad;
import com.uem.ambulancias.flota.repository.AmbulanciaRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Lo único que corre solo en todo el módulo. Un traslado programado el jueves no tiene quién lo despierte el
 * lunes a las nueve: este barrido es ese despertador.
 *
 * <p>Tres cosas en cada pasada: abrir la búsqueda de los que ya tienen que salir, cerrar los que ya no llegan, e
 * intentar asignar los que están esperando. Los que esperan salen ordenados por urgencia, así que cuando queda
 * una sola unidad se la lleva el que primero se cae.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PlanificadorDeTraslados {

	private final TrasladoRepository traslados;
	private final AsignadorDeTraslados asignador;
	private final AmbulanciaRepository ambulancias;
	private final TrasladoProperties config;

	/**
	 * Un traslado esperando y una unidad que se desocupa: engancharlos en el momento en vez de esperar al próximo
	 * barrido. Corre después del commit, cuando la unidad ya quedó libre de verdad.
	 */
	@TransactionalEventListener
	public void alLiberarseUnaUnidad(UnidadLiberada evento) {
		ambulancias.findById(evento.ambulanciaId()).ifPresent(unidad -> {
			List<TipoUnidad> queCubre = TipoUnidad.ESCALERA.stream()
					.filter(tipo -> unidad.getTipoUnidad().cubreA(tipo))
					.toList();
			if (queCubre.isEmpty()) {
				return;
			}
			for (Traslado traslado : traslados.buscarEsperandoUnidadDeTipo(queCubre)) {
				if (asignador.intentarAsignar(traslado.getId()).isPresent()) {
					log.info("Traslado {} asignado a la unidad que acaba de liberarse", traslado.getId());
					return;
				}
			}
		});
	}

	@Scheduled(fixedDelayString = "${sga.traslados.barrido-seg:120}", timeUnit = TimeUnit.SECONDS)
	public void barrer() {
		Instant ahora = Instant.now();

		int abiertos = asignador.abrirBusquedas(ahora);
		if (abiertos > 0) {
			log.info("Traslados que pasan a buscar unidad: {}", abiertos);
		}

		asignador.vencerLosQueNoLlegan(ahora).forEach(traslado -> log.warn(
				"Traslado {} sin unidad y ya no llega a tiempo: queda como no cubierto", traslado.getId()));

		asignarPendientes(ahora);
	}

	private void asignarPendientes(Instant ahora) {
		for (Traslado traslado : traslados.buscarEsperandoUnidad()) {
			if (asignador.intentarAsignar(traslado.getId()).isPresent()) {
				log.info("Traslado {} asignado", traslado.getId());
			} else {
				avisarSiApremia(traslado, ahora);
			}
		}
	}

	/**
	 * El aviso temprano: mientras falte mucho, que no haya unidad es normal y no vale la pena decir nada. Cerca
	 * del límite sí, porque es cuando todavía hay tiempo de que una persona haga algo.
	 */
	private void avisarSiApremia(Traslado traslado, Instant ahora) {
		Instant umbral = ahora.plus(Duration.ofMinutes(config.minutosAvisoTemprano()));
		if (traslado.getHoraLimiteSalida().isBefore(umbral)) {
			log.warn("Traslado {} sigue sin unidad. Ultima salida posible: {}", traslado.getId(),
					traslado.getHoraLimiteSalida());
		}
	}

}
