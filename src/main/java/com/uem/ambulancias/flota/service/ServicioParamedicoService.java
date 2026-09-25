package com.uem.ambulancias.flota.service;

import com.uem.ambulancias.comun.error.CodigoError;
import com.uem.ambulancias.comun.error.ConflictoException;
import com.uem.ambulancias.comun.error.NoEncontradoException;
import com.uem.ambulancias.flota.repository.AsignacionRepository;
import com.uem.ambulancias.usuarios.domain.RolUsuario;
import com.uem.ambulancias.usuarios.domain.Usuario;
import com.uem.ambulancias.usuarios.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lo que la app del paramédico necesita saber de quien la usa: quién es y qué ambulancia opera.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ServicioParamedicoService {

	private final UsuarioRepository usuarios;
	private final AsignacionRepository asignaciones;

	/** Identificación provisional por teléfono, mientras no exista autenticación. Solo paramédicos activos. */
	public ParamedicoConAsignacion identificar(String telefono) {
		Usuario paramedico = usuarios
				.findFirstByTelefonoAndRolAndActivoTrueAndRegistradoPorIsNullOrderByIdAsc(telefono.trim(), RolUsuario.PARAMEDICO)
				.orElseThrow(() -> new NoEncontradoException("No hay un paramédico activo con ese teléfono."));
		return conAsignacionVigente(paramedico);
	}

	public ParamedicoConAsignacion servicioActual(Long paramedicoId) {
		return conAsignacionVigente(buscarParamedicoActivo(paramedicoId));
	}

	/** PB-03 R3: token para recibir push de incidentes nuevos con la app cerrada. */
	@Transactional
	public void registrarDispositivo(Long paramedicoId, String tokenPush) {
		buscarParamedicoActivo(paramedicoId).registrarDispositivo(tokenPush.trim());
	}

	/** PB-04 R6: la ambulancia es la de la asignación vigente del paramédico, nunca viaja en la petición. */
	public Long ambulanciaAsignada(Long paramedicoId) {
		buscarParamedicoActivo(paramedicoId);
		return asignaciones.buscarVigentePorParamedico(paramedicoId)
				.map(asignacion -> asignacion.getAmbulancia().getId())
				.orElseThrow(() -> new ConflictoException(CodigoError.SIN_SERVICIO,
						"El paramédico no tiene una ambulancia asignada."));
	}

	public Usuario buscarParamedicoActivo(Long paramedicoId) {
		return usuarios.findByIdAndRol(paramedicoId, RolUsuario.PARAMEDICO)
				.filter(Usuario::isActivo)
				.orElseThrow(() -> new NoEncontradoException("No existe un paramédico activo con id " + paramedicoId + "."));
	}

	private ParamedicoConAsignacion conAsignacionVigente(Usuario paramedico) {
		return new ParamedicoConAsignacion(paramedico,
				asignaciones.buscarVigentePorParamedico(paramedico.getId()).orElse(null));
	}

}
