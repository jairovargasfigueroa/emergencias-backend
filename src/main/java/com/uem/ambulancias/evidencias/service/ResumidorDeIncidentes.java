package com.uem.ambulancias.evidencias.service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.uem.ambulancias.evidencias.domain.TipoTrabajoIa;
import com.uem.ambulancias.evidencias.domain.TrabajoIa;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Worker de los resúmenes: toma de a un trabajo, junta todo lo que se sabe del incidente, llama al servicio y guarda
 * la versión si mejora a la vigente. Corre aparte del de los análisis, así un resumen largo no frena las evidencias
 * que siguen llegando.
 *
 * <p>Solo existe con {@code sga.ia.habilitado=true}.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "sga.ia", name = "habilitado", havingValue = "true")
@RequiredArgsConstructor
public class ResumidorDeIncidentes {

	private final ColaDeTrabajosIa cola;
	private final ResumenIncidenteService resumenService;
	private final ServicioDeAnalisis servicio;

	@Scheduled(fixedDelayString = "${sga.ia.trabajos.barrido-seg:10}", timeUnit = TimeUnit.SECONDS)
	public void barrer() {
		List<TrabajoIa> tomados = cola.tomar(TipoTrabajoIa.RESUMEN, 1);
		while (!tomados.isEmpty()) {
			procesar(tomados.getFirst().getId());
			tomados = cola.tomar(TipoTrabajoIa.RESUMEN, 1);
		}
	}

	private void procesar(Long trabajoId) {
		try {
			Optional<PedidoDeResumen> pedido = resumenService.prepararPedido(trabajoId);
			if (pedido.isEmpty()) {
				return;
			}
			resumenService.guardar(trabajoId, servicio.resumir(pedido.get())).ifPresent(resumen -> log.info(
					"Incidente {}: resumen versión {}.", pedido.get().incidenteId(), resumen.getVersion()));
		} catch (FalloDelServicioIa e) {
			log.warn("Resumen del trabajo {} sin resultado: {} ({}).", trabajoId, e.getCodigo(), e.getDesenlace());
			switch (e.getDesenlace()) {
				// El resumen no descarga archivos: una URL vencida no le corresponde, se trata como algo pasajero.
				case REINTENTAR, FIRMAR_DE_NUEVO -> cola.reintentar(trabajoId, e.getCodigo());
				case DEFINITIVO -> cola.fallar(trabajoId, e.getCodigo());
				case REVISION -> cola.pasarARevision(trabajoId, e.getCodigo());
			}
		} catch (RuntimeException e) {
			log.error("Falló el resumen del trabajo {} de este lado.", trabajoId, e);
			cola.reintentar(trabajoId, "error_del_backend");
		}
	}

}
