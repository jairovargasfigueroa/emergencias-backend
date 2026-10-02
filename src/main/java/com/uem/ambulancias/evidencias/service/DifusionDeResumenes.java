package com.uem.ambulancias.evidencias.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Avisa la versión nueva del resumen después del commit, para que nadie la pida antes de que exista. */
@Slf4j
@Component
@RequiredArgsConstructor
public class DifusionDeResumenes {

	private final PublicadorDeResumenes publicador;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void difundir(ResumenActualizado evento) {
		try {
			publicador.publicarResumenNuevo(evento.incidenteId(), evento.version());
		} catch (RuntimeException e) {
			// La versión ya está guardada: si el aviso falla, el personal la ve igual la próxima vez que consulte.
			log.error("No se pudo avisar el resumen {} del incidente {}.", evento.version(), evento.incidenteId(), e);
		}
	}

}
