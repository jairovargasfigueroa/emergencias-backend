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

	/**
	 * Sin nadie de turno la unidad deja de reportar, así que su última posición pasa a ser un dato muerto y se
	 * retira del mapa.
	 *
	 * <p>Después del commit, como todo lo que se publica: si el cierre del turno se revirtiera y la posición ya
	 * estuviera borrada, habríamos apagado del mapa a una unidad que sigue trabajando.
	 */
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void retirar(UnidadSinTripulacion unidad) {
		try {
			publicador.retirarPosicion(unidad.ambulanciaId());
		} catch (RuntimeException e) {
			log.error("No se pudo retirar la posición de la ambulancia {}.", unidad.ambulanciaId(), e);
		}
	}

}
