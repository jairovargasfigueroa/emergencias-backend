package com.uem.ambulancias.evidencias.service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Barre las evidencias que ya no sirven y las pasa a DESCARTADA: las que se firmaron y nunca se subieron, y las que
 * pasaron el tiempo de retención. Corre con la IA encendida o apagada, porque las evidencias se juntan igual.
 *
 * <p>De las abandonadas se borra el archivo, por si la app llegó a subirlo y nunca avisó. De las vencidas no: en el
 * bucket hay una regla de ciclo de vida sobre {@code alertas/} que borra los archivos a los 90 días, configurada en
 * la consola de AWS. Si se cambia {@code sga.evidencias.dias-retencion}, hay que cambiar también esa regla.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LimpiezaDeEvidencias {

	private final EvidenciaService evidenciaService;
	private final AlmacenDeEvidencias almacen;

	@Scheduled(fixedDelayString = "${sga.evidencias.limpieza-cada-min:60}", timeUnit = TimeUnit.MINUTES)
	public void barrer() {
		Instant ahora = Instant.now();
		try {
			List<String> claves = evidenciaService.descartarAbandonadas(ahora);
			if (!claves.isEmpty()) {
				log.info("Evidencias abandonadas sin subir: {}.", claves.size());
			}
			claves.forEach(this::borrar);

			int vencidas = evidenciaService.descartarVencidas(ahora);
			if (vencidas > 0) {
				log.info("Evidencias descartadas por retención: {}.", vencidas);
			}
		} catch (RuntimeException e) {
			// La próxima pasada vuelve a encontrar lo que quedó sin descartar.
			log.error("No se pudo terminar la limpieza de evidencias.", e);
		}
	}

	/** Si el archivo nunca llegó, el almacén no hace nada; si no responde, el objeto queda para la regla de S3. */
	private void borrar(String claveObjeto) {
		try {
			almacen.borrar(claveObjeto);
		} catch (RuntimeException e) {
			log.warn("No se pudo borrar {} del almacén: queda para la regla de ciclo de vida.", claveObjeto);
		}
	}

}
