package com.uem.ambulancias.flota.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.uem.ambulancias.comun.error.CodigoError;
import com.uem.ambulancias.comun.error.ConflictoException;
import com.uem.ambulancias.comun.error.NoEncontradoException;
import com.uem.ambulancias.flota.domain.Asignacion;
import com.uem.ambulancias.flota.repository.AsignacionRepository;
import com.uem.ambulancias.usuarios.domain.RolUsuario;
import com.uem.ambulancias.usuarios.domain.Usuario;
import com.uem.ambulancias.usuarios.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Personal de la flota: paramédicos registrados por el administrador (R1).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PersonalService {

	private final UsuarioRepository usuarios;
	private final AsignacionRepository asignaciones;

	/**
	 * El teléfono es hoy la única credencial del paramédico: con eso y nada más entra a su app. Por eso no puede
	 * repetirse entre paramédicos activos — dos con el mismo número serían la misma cuenta, y el ingreso le
	 * entregaría la sesión a uno de los dos sin forma de saber a cuál.
	 */
	@Transactional
	public ParamedicoConAsignacion registrarParamedico(String nombreCompleto, String telefono) {
		String telefonoLimpio = telefono.trim();
		if (usuarios.existsByTelefonoAndRolAndActivoTrueAndRegistradoPorIsNull(telefonoLimpio, RolUsuario.PARAMEDICO)) {
			throw telefonoDuplicado(telefonoLimpio);
		}
		try {
			Usuario paramedico = usuarios
					.saveAndFlush(Usuario.registrarParamedico(nombreCompleto.trim(), telefonoLimpio));
			return new ParamedicoConAsignacion(paramedico, null);
		} catch (DataIntegrityViolationException e) {
			// Otra petición registró el mismo número entre la verificación y el guardado.
			throw telefonoDuplicado(telefonoLimpio);
		}
	}

	private static ConflictoException telefonoDuplicado(String telefono) {
		return new ConflictoException(CodigoError.TELEFONO_DUPLICADO,
				"Ya hay un paramédico activo con el teléfono " + telefono + ".");
	}

	/** Paramédicos ordenados por nombre, cada uno con su asignación vigente si la tiene. */
	public List<ParamedicoConAsignacion> listarParamedicos() {
		Map<Long, Asignacion> vigentesPorParamedico = asignaciones.buscarVigentes().stream()
				.collect(Collectors.toMap(asignacion -> asignacion.getParamedico().getId(), Function.identity(),
						(una, otra) -> una.getFechaInicio().isAfter(otra.getFechaInicio()) ? una : otra));
		return usuarios.findByRolOrderByNombreCompletoAsc(RolUsuario.PARAMEDICO).stream()
				.map(paramedico -> new ParamedicoConAsignacion(paramedico, vigentesPorParamedico.get(paramedico.getId())))
				.toList();
	}

	/** Baja lógica: sus asignaciones siguen intactas. */
	@Transactional
	public ParamedicoConAsignacion desactivarParamedico(Long id) {
		Usuario paramedico = buscarParamedico(id);
		paramedico.desactivar();
		return new ParamedicoConAsignacion(paramedico, asignaciones.buscarVigentePorParamedico(id).orElse(null));
	}

	public List<Asignacion> historialDeAsignaciones(Long paramedicoId) {
		buscarParamedico(paramedicoId);
		return asignaciones.buscarPorParamedico(paramedicoId);
	}

	private Usuario buscarParamedico(Long id) {
		return usuarios.findByIdAndRol(id, RolUsuario.PARAMEDICO)
				.orElseThrow(() -> new NoEncontradoException("No existe el paramédico " + id + "."));
	}

}
