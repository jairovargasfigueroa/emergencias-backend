package com.uem.ambulancias.flota.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class DifusionDePosiciones {

	private final PublicadorDePosiciones publicador;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void difundir(PosicionActualizada posicion) {
		try {
			publicador.publicarPosicion(posicion);
		} catch (RuntimeException e) {
			log.error("No se pudo difundir la posición de la ambulancia {}.", posicion.ambulanciaId(), e);
		}
	}

}
