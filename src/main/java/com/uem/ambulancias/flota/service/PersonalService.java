package com.uem.ambulancias.flota.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.uem.ambulancias.comun.error.NoEncontradoException;
import com.uem.ambulancias.flota.domain.Asignacion;
import com.uem.ambulancias.flota.repository.AsignacionRepository;
import com.uem.ambulancias.usuarios.domain.RolUsuario;
import com.uem.ambulancias.usuarios.domain.Usuario;
import com.uem.ambulancias.usuarios.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;
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

	@Transactional
	public ParamedicoConAsignacion registrarParamedico(String nombreCompleto, String telefono) {
		Usuario paramedico = usuarios.save(Usuario.registrarParamedico(nombreCompleto.trim(), telefono.trim()));
		return new ParamedicoConAsignacion(paramedico, null);
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
