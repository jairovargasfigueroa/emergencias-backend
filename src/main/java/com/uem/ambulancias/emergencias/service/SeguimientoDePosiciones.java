package com.uem.ambulancias.emergencias.service;

import com.uem.ambulancias.emergencias.repository.AtencionRepository;
import com.uem.ambulancias.flota.service.PosicionActualizada;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * PB-06 R2: el ciudadano ve la posición de una unidad solo mientras esa unidad tenga una atención activa en su
 * incidente. Se garantiza en los datos publicados: la posición se copia al seguimiento únicamente en ese caso.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeguimientoDePosiciones {

	private final AtencionRepository atenciones;
	private final PublicadorDeIncidentes publicador;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
	public void copiarAlSeguimiento(PosicionActualizada posicion) {
		try {
			atenciones.buscarActivaPorAmbulancia(posicion.ambulanciaId())
					.ifPresent(atencion -> publicador.publicarPosicionEnSeguimiento(atencion.getIncidente().getId(),
							posicion));
		} catch (RuntimeException e) {
			log.error("No se pudo copiar al seguimiento la posición de la ambulancia {}.", posicion.ambulanciaId(), e);
		}
	}

}
