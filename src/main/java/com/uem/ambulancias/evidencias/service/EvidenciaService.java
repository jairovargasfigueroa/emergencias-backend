package com.uem.ambulancias.evidencias.service;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

import com.uem.ambulancias.comun.error.CodigoError;
import com.uem.ambulancias.comun.error.ConflictoException;
import com.uem.ambulancias.comun.error.NoEncontradoException;
import com.uem.ambulancias.emergencias.domain.Alerta;
import com.uem.ambulancias.emergencias.repository.AlertaRepository;
import com.uem.ambulancias.emergencias.repository.IncidenteRepository;
import com.uem.ambulancias.evidencias.domain.Evidencia;
import com.uem.ambulancias.evidencias.domain.FormatoEvidencia;
import com.uem.ambulancias.evidencias.repository.EvidenciaRepository;
import com.uem.ambulancias.usuarios.domain.RolUsuario;

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
	private final ColaDeTrabajosIa cola;
	private final AccesoAlIncidente acceso;

	/**
	 * Registra la evidencia y firma su subida. Se hace con el incidente bloqueado, igual que al completar los detalles
	 * de la alerta: dos pedidos simultáneos no pueden pasarse juntos del máximo por alerta ni del máximo por incidente,
	 * que suma las alertas de todos los que avisaron. Si la firma falla, no queda nada guardado.
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
		if (!config.admite(formato.modalidad())) {
			throw new ConflictoException(CodigoError.FORMATO_NO_ADMITIDO, "Por ahora no se aceptan videos.");
		}
		if (evidencias.contarVigentesPorAlerta(alertaId) >= config.maximoPorAlerta()) {
			throw new ConflictoException(CodigoError.LIMITE_DE_EVIDENCIAS,
					"La alerta " + alertaId + " ya tiene " + config.maximoPorAlerta() + " evidencias.");
		}
		if (evidencias.contarVigentesPorIncidente(incidenteId) >= config.maximoPorIncidente()) {
			throw new ConflictoException(CodigoError.LIMITE_DE_EVIDENCIAS_INCIDENTE,
					"El incidente " + incidenteId + " ya tiene " + config.maximoPorIncidente() + " evidencias.");
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

	/**
	 * La app terminó de subir. Se pregunta al almacén si el archivo está y si es el que se firmó, sin descargarlo. Con
	 * la fila bloqueada, y confirmar de nuevo una evidencia ya confirmada no hace nada: la app puede repetir el aviso
	 * si se le cortó la respuesta. El análisis se encola en la misma transacción: no hay evidencia subida sin su
	 * trabajo, ni trabajo de una evidencia que no llegó.
	 */
	@Transactional
	public Evidencia confirmar(Long evidenciaId, Long ciudadanoId) {
		Evidencia evidencia = evidencias.buscarParaActualizar(evidenciaId)
				.orElseThrow(() -> new NoEncontradoException("No existe la evidencia " + evidenciaId + "."));
		exigirQueSeaSuya(evidencia, ciudadanoId);
		if (!evidencia.isPendienteDeSubida()) {
			return evidencia;
		}

		ObjetoAlmacenado objeto = almacen.verificarObjeto(evidencia.getClaveObjeto())
				.orElseThrow(() -> new ConflictoException(CodigoError.EVIDENCIA_NO_SUBIDA,
						"El archivo de la evidencia " + evidenciaId + " todavía no está subido."));
		boolean mismoTamano = objeto.tamanoBytes() == evidencia.getTamanoBytes();
		boolean mismoContenido = objeto.sha256Base64() == null
				|| objeto.sha256Base64().equals(evidencia.getSha256Base64());
		if (!mismoTamano || !mismoContenido) {
			throw new ConflictoException(CodigoError.EVIDENCIA_NO_COINCIDE,
					"El archivo subido no es el que se anunció para la evidencia " + evidenciaId + ".");
		}

		evidencia.confirmarSubida(Instant.now());
		cola.encolarAnalisis(evidencia);
		return evidencia;
	}

	/**
	 * URL temporal para que el personal vea o escuche el archivo. Solo si el archivo ya llegó al almacén, y para un
	 * paramédico solo si está atendiendo el incidente de la alerta.
	 */
	@Transactional(readOnly = true)
	public LecturaFirmada firmarLectura(Long evidenciaId, Long usuarioId, RolUsuario rol) {
		Evidencia evidencia = evidencias.buscarConAlerta(evidenciaId)
				.filter(Evidencia::tieneArchivo)
				.orElseThrow(() -> new NoEncontradoException("No existe la evidencia " + evidenciaId + "."));
		acceso.exigir(evidencia.getAlerta().getIncidente().getId(), usuarioId, rol);
		return almacen.firmarLectura(evidencia.getClaveObjeto(), config.vigenciaLectura());
	}

	/**
	 * Descarta las que se firmaron hace más de {@link EvidenciaProperties#abandono()} y nunca se confirmaron. Devuelve
	 * sus claves para borrar después del commit lo que haya quedado a medio subir en el almacén.
	 */
	@Transactional
	public List<String> descartarAbandonadas(Instant ahora) {
		List<Evidencia> abandonadas = evidencias.buscarPendientesRegistradasAntesDe(ahora.minus(config.abandono()));
		abandonadas.forEach(Evidencia::descartar);
		return abandonadas.stream().map(Evidencia::getClaveObjeto).filter(Objects::nonNull).toList();
	}

	/** Descarta las registradas hace más de {@link EvidenciaProperties#retencion()}. Devuelve cuántas. */
	@Transactional
	public int descartarVencidas(Instant ahora) {
		return evidencias.descartarRegistradasAntesDe(ahora.minus(config.retencion()));
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
