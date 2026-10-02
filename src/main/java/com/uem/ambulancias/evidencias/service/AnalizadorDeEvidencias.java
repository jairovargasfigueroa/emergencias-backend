package com.uem.ambulancias.evidencias.service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.uem.ambulancias.evidencias.domain.TipoTrabajoIa;
import com.uem.ambulancias.evidencias.domain.TrabajoIa;
import com.uem.ambulancias.evidencias.service.FalloDelServicioIa.Desenlace;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Worker de los análisis: toma de a un trabajo, firma la lectura, llama al servicio y guarda. De a uno porque cada
 * llamada puede tardar minutos y el trabajo tomado es del worker solo por un rato: tomar varios juntos haría vencer
 * el bloqueo de los últimos mientras se procesan los primeros.
 *
 * <p>Solo existe con {@code sga.ia.habilitado=true}.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "sga.ia", name = "habilitado", havingValue = "true")
@RequiredArgsConstructor
public class AnalizadorDeEvidencias {

	private final ColaDeTrabajosIa cola;
	private final AnalisisEvidenciaService analisisService;
	private final ServicioDeAnalisis servicio;

	@Scheduled(fixedDelayString = "${sga.ia.trabajos.barrido-seg:10}", timeUnit = TimeUnit.SECONDS)
	public void barrer() {
		List<TrabajoIa> tomados = cola.tomar(TipoTrabajoIa.ANALISIS, 1);
		while (!tomados.isEmpty()) {
			procesar(tomados.getFirst().getId());
			tomados = cola.tomar(TipoTrabajoIa.ANALISIS, 1);
		}
	}

	private void procesar(Long trabajoId) {
		try {
			Optional<PedidoDeAnalisis> pedido = analisisService.prepararPedido(trabajoId);
			if (pedido.isEmpty()) {
				return;
			}
			analisisService.guardar(trabajoId, analizar(trabajoId, pedido.get()));
		} catch (FalloDelServicioIa e) {
			log.warn("Análisis del trabajo {} sin resultado: {} ({}).", trabajoId, e.getCodigo(), e.getDesenlace());
			switch (e.getDesenlace()) {
				case REINTENTAR, FIRMAR_DE_NUEVO -> cola.reintentar(trabajoId, e.getCodigo());
				case DEFINITIVO -> cola.fallar(trabajoId, e.getCodigo());
				case REVISION -> cola.pasarARevision(trabajoId, e.getCodigo());
			}
		} catch (RuntimeException e) {
			log.error("Falló el análisis del trabajo {} de este lado.", trabajoId, e);
			cola.reintentar(trabajoId, "error_del_backend");
		}
	}

	/** Si la URL venció antes de que el servicio la usara, se firma otra y se prueba una vez más en el mismo intento. */
	private AnalisisRecibido analizar(Long trabajoId, PedidoDeAnalisis pedido) {
		try {
			return servicio.analizar(pedido);
		} catch (FalloDelServicioIa e) {
			if (e.getDesenlace() != Desenlace.FIRMAR_DE_NUEVO) {
				throw e;
			}
			log.info("La URL de lectura del trabajo {} venció: se firma otra.", trabajoId);
			Optional<PedidoDeAnalisis> otro = analisisService.prepararPedido(trabajoId);
			if (otro.isEmpty()) {
				throw e;
			}
			return servicio.analizar(otro.get());
		}
	}

}
