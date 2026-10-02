package com.uem.ambulancias.evidencias.service;

import java.time.Instant;

import com.uem.ambulancias.comun.error.CodigoError;
import com.uem.ambulancias.comun.error.ConflictoException;
import com.uem.ambulancias.comun.error.NoEncontradoException;
import com.uem.ambulancias.emergencias.domain.Alerta;
import com.uem.ambulancias.emergencias.repository.AlertaRepository;
import com.uem.ambulancias.emergencias.repository.IncidenteRepository;
import com.uem.ambulancias.evidencias.domain.Evidencia;
import com.uem.ambulancias.evidencias.domain.FormatoEvidencia;
import com.uem.ambulancias.evidencias.repository.EvidenciaRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Las evidencias del ciudadano. El archivo va directo de la app al almacén: acá se decide si se acepta, se firma la
 * subida y después se comprueba que lo subido sea lo que se firmó.
 */
@Service
@RequiredArgsConstructor
public class EvidenciaService {

	private final EvidenciaRepository evidencias;
	private final AlertaRepository alertas;
	private final IncidenteRepository incidentes;
	private final AlmacenDeEvidencias almacen;
	private final EvidenciaProperties config;

	/**
	 * Registra la evidencia y firma su subida. Se hace con el incidente bloqueado, igual que al completar los detalles
	 * de la alerta: dos pedidos simultáneos del mismo ciudadano no pueden pasarse juntos del máximo por alerta. Si la
	 * firma falla, no queda nada guardado.
	 */
	@Transactional
	public EvidenciaConSubida registrar(Long alertaId, Long ciudadanoId, String mimeType, long tamanoBytes,
			String sha256) {
		Long incidenteId = alertas.buscarIncidenteId(alertaId)
				.orElseThrow(() -> new NoEncontradoException("No existe la alerta " + alertaId + "."));
		incidentes.buscarParaActualizar(incidenteId)
				.orElseThrow(() -> new NoEncontradoException("No existe el incidente " + incidenteId + "."));
		Alerta alerta = alertas.findById(alertaId)
				.orElseThrow(() -> new NoEncontradoException("No existe la alerta " + alertaId + "."));

		if (!alerta.getEmisor().getId().equals(ciudadanoId)) {
			throw new ConflictoException(CodigoError.ALERTA_AJENA,
					"La alerta " + alertaId + " no es del ciudadano " + ciudadanoId + ".");
		}
		FormatoEvidencia formato = FormatoEvidencia.deMime(mimeType)
				.orElseThrow(() -> new ConflictoException(CodigoError.FORMATO_NO_ADMITIDO,
						"No se aceptan archivos de tipo " + mimeType + "."));
		if (evidencias.contarVigentesPorAlerta(alertaId) >= config.maximoPorAlerta()) {
			throw new ConflictoException(CodigoError.LIMITE_DE_EVIDENCIAS,
					"La alerta " + alertaId + " ya tiene " + config.maximoPorAlerta() + " evidencias.");
		}

		Evidencia evidencia = evidencias.save(Evidencia.registrar(alerta, formato, tamanoBytes, sha256,
				config.limiteBytes(formato.modalidad()), Instant.now()));
		evidencia.asignarClaveObjeto();
		return new EvidenciaConSubida(evidencia, firmarSubida(evidencia));
	}

	/**
	 * Una URL nueva para la misma evidencia: la anterior venció o la subida se cortó. Solo mientras el archivo no se
	 * haya confirmado y la alerta siga abierta.
	 */
	@Transactional(readOnly = true)
	public EvidenciaConSubida firmarDeNuevo(Long evidenciaId, Long ciudadanoId) {
		Evidencia evidencia = evidencias.buscarConAlerta(evidenciaId)
				.orElseThrow(() -> new NoEncontradoException("No existe la evidencia " + evidenciaId + "."));
		exigirQueSeaSuya(evidencia, ciudadanoId);
		if (!evidencia.isPendienteDeSubida()) {
			throw new ConflictoException(CodigoError.EVIDENCIA_YA_SUBIDA,
					"La evidencia " + evidenciaId + " ya está subida.");
		}
		Evidencia.exigirAlertaAbierta(evidencia.getAlerta());
		return new EvidenciaConSubida(evidencia, firmarSubida(evidencia));
	}

	private SubidaFirmada firmarSubida(Evidencia evidencia) {
		return almacen.firmarSubida(evidencia.getClaveObjeto(), evidencia.getMimeType(), evidencia.getTamanoBytes(),
				evidencia.getSha256Base64(), config.vigenciaSubida());
	}

	private static void exigirQueSeaSuya(Evidencia evidencia, Long ciudadanoId) {
		if (!evidencia.getAlerta().getEmisor().getId().equals(ciudadanoId)) {
			throw new ConflictoException(CodigoError.EVIDENCIA_AJENA,
					"La evidencia " + evidencia.getId() + " no es del ciudadano " + ciudadanoId + ".");
		}
	}

}
