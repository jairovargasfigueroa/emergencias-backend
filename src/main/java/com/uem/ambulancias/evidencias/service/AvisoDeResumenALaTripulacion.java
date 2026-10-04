package com.uem.ambulancias.evidencias.service;

import java.util.List;
import java.util.Map;

import com.uem.ambulancias.emergencias.repository.AtencionRepository;
import com.uem.ambulancias.emergencias.service.AvisoParaParamedico;
import com.uem.ambulancias.emergencias.service.NotificadorPush;
import com.uem.ambulancias.flota.repository.TurnoRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Le avisa por push a la tripulación que va en camino al incidente que el resumen cambió en algo importante. La señal
 * de Firebase solo la oye la app abierta; el push es para cuando van manejando con el teléfono en el bolsillo.
 *
 * <p>Solo se avisa un cambio importante (gravedad, tipo de suceso, personas o peligros) y solo a las unidades en
 * camino: un aviso que suena por todo deja de escucharse. Las demás versiones igual llegan a la pantalla por Firebase.
 *
 * <p>El push no lleva nada de lo que dice el resumen: la notificación se ve en la pantalla bloqueada. La app lo pide
 * a la API, que es la que decide quién puede verlo.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AvisoDeResumenALaTripulacion {

	static final String TIPO = "RESUMEN_IA";

	private final AtencionRepository atenciones;
	private final TurnoRepository turnos;
	private final NotificadorPush notificador;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
	public void avisar(ResumenActualizado evento) {
		if (!evento.importante()) {
			return;
		}
		try {
			List<Long> ambulancias = atenciones.buscarAmbulanciasEnCaminoAlIncidente(evento.incidenteId());
			if (ambulancias.isEmpty()) {
				// Nadie va en camino: quien tome el incidente lo verá al abrirlo, y quien ya llegó ve la escena.
				return;
			}
			Map<String, String> datos = Map.of("tipo", TIPO, "incidenteId", String.valueOf(evento.incidenteId()));
			List<AvisoParaParamedico> avisos = turnos.buscarAbiertosConAvisoPorAmbulancias(ambulancias).stream()
					.map(turno -> new AvisoParaParamedico(turno.getParamedico().getTokenPush(),
							"Información nueva del incidente", "Abre la app para ver el resumen actualizado.", datos))
					.toList();
			notificador.notificarParamedicos(avisos);
		} catch (RuntimeException e) {
			// La versión ya está guardada: si el push falla, la tripulación la ve igual al abrir el incidente.
			log.error("No se pudo avisar a la tripulación el resumen {} del incidente {}.", evento.version(),
					evento.incidenteId(), e);
		}
	}

}
