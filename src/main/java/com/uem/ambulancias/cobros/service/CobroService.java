package com.uem.ambulancias.cobros.service;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uem.ambulancias.cobros.domain.Cobro;
import com.uem.ambulancias.cobros.domain.TipoServicio;
import com.uem.ambulancias.cobros.dto.AjustarCobroRequest;
import com.uem.ambulancias.cobros.dto.CobroDeServicioResponse;
import com.uem.ambulancias.cobros.dto.CobroDeServicioResponse.CambioResponse;
import com.uem.ambulancias.cobros.dto.CobroDeServicioResponse.CobroResponse;
import com.uem.ambulancias.cobros.dto.RegistrarCobroRequest;
import com.uem.ambulancias.cobros.dto.TarifaResponse;
import com.uem.ambulancias.cobros.repository.CambioDeCobroRepository;
import com.uem.ambulancias.cobros.repository.CobroRepository;
import com.uem.ambulancias.comun.error.CodigoError;
import com.uem.ambulancias.comun.error.ConflictoException;
import com.uem.ambulancias.comun.error.NoEncontradoException;
import com.uem.ambulancias.emergencias.domain.Atencion;
import com.uem.ambulancias.emergencias.repository.AtencionRepository;
import com.uem.ambulancias.emergencias.service.TrasladoProperties;
import com.uem.ambulancias.usuarios.domain.RolUsuario;
import com.uem.ambulancias.usuarios.domain.Usuario;
import com.uem.ambulancias.usuarios.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

/**
 * PB-29: el cobro de cada servicio. Vive aparte de la atención y nunca la modifica: cobrar no cambia cómo terminó el
 * servicio. Corregir o anular deja el valor anterior, el motivo, la fecha y quién lo hizo.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CobroService {

	private final CobroRepository cobros;
	private final CambioDeCobroRepository cambios;
	private final AtencionRepository atenciones;
	private final UsuarioRepository usuarios;
	private final TarifaService tarifas;
	private final TrasladoProperties config;

	public CobroDeServicioResponse delServicio(Long atencionId) {
		Atencion atencion = buscarAtencion(atencionId);
		Cobro cobro = cobros.buscarPorAtencion(atencionId).orElse(null);
		TipoServicio tipo = TipoServicio.de(atencion);
		var dia = atencion.getHoraToma().atZone(ZoneId.of(config.zona())).toLocalDate();
		List<CambioResponse> suyos = cobro == null ? List.of()
				: cambios.buscarPorCobro(cobro.getId()).stream().map(CambioResponse::de).toList();
		return new CobroDeServicioResponse(atencionId, tipo, atencion.getEstado(), atencion.getEstado().isResuelta(),
				paciente(atencion), TarifaResponse.de(tarifas.vigenteAl(tipo, dia)),
				cobro == null ? null : CobroResponse.de(cobro), suyos);
	}

	@Transactional
	public CobroDeServicioResponse registrar(Long administradorId, RegistrarCobroRequest datos) {
		Atencion atencion = buscarAtencion(datos.atencionId());
		if (!atencion.getEstado().isResuelta()) {
			throw new ConflictoException(CodigoError.SERVICIO_NO_TERMINADO,
					"El servicio todavía no terminó: se cobra cuando el paciente se entrega o se cierra sin traslado.");
		}
		if (cobros.existsByAtencionId(atencion.getId())) {
			throw new ConflictoException(CodigoError.COBRO_YA_REGISTRADO,
					"Este servicio ya tiene su cobro: corrígelo con un ajuste.");
		}
		cobros.save(Cobro.registrar(atencion, datos.modalidad(), datos.importe(), datos.estado(),
				administrador(administradorId), Instant.now()));
		return delServicio(atencion.getId());
	}

	@Transactional
	public CobroDeServicioResponse ajustar(Long cobroId, Long administradorId, AjustarCobroRequest datos) {
		Cobro cobro = buscarCobro(cobroId);
		cambios.save(cobro.ajustar(datos.modalidad(), datos.importe(), datos.estado(), datos.motivo().trim(),
				administrador(administradorId), Instant.now()));
		return delServicio(cobro.getAtencion().getId());
	}

	@Transactional
	public CobroDeServicioResponse anular(Long cobroId, Long administradorId, String motivo) {
		Cobro cobro = buscarCobro(cobroId);
		cambios.save(cobro.anular(motivo.trim(), administrador(administradorId), Instant.now()));
		return delServicio(cobro.getAtencion().getId());
	}

	/** El nombre que registró la tripulación; en un traslado sin ese dato, el pasajero del pedido. */
	private static String paciente(Atencion atencion) {
		if (atencion.getNombrePaciente() != null) {
			return atencion.getNombrePaciente();
		}
		return atencion.getTraslado() == null ? null : atencion.getTraslado().getPasajero().getNombreCompleto();
	}

	private Atencion buscarAtencion(Long atencionId) {
		return atenciones.findById(atencionId)
				.orElseThrow(() -> new NoEncontradoException("No existe el servicio " + atencionId + "."));
	}

	private Cobro buscarCobro(Long cobroId) {
		return cobros.buscarParaActualizar(cobroId)
				.orElseThrow(() -> new NoEncontradoException("No existe el cobro " + cobroId + "."));
	}

	private Usuario administrador(Long administradorId) {
		return usuarios.findByIdAndRol(administradorId, RolUsuario.ADMIN)
				.orElseThrow(() -> new NoEncontradoException("No existe el administrador " + administradorId + "."));
	}

}
