package com.uem.ambulancias.usuarios.service;

import java.util.List;

import com.uem.ambulancias.comun.error.NoEncontradoException;
import com.uem.ambulancias.usuarios.domain.RolUsuario;
import com.uem.ambulancias.usuarios.domain.Usuario;
import com.uem.ambulancias.usuarios.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CiudadanoService {

	private final UsuarioRepository usuarios;

	/**
	 * Registro ligero (PB-02 R1). Si ya existe un ciudadano con ese teléfono se devuelve el mismo, para que
	 * reinstalar la app no duplique usuarios.
	 */
	@Transactional
	public Usuario registrar(String nombreCompleto, String telefono) {
		String telefonoLimpio = telefono.trim();
		return usuarios.findFirstByTelefonoAndRolAndRegistradoPorIsNullOrderByIdAsc(telefonoLimpio, RolUsuario.CIUDADANO)
				.orElseGet(() -> usuarios.save(Usuario.registrarCiudadano(nombreCompleto.trim(), telefonoLimpio)));
	}

	/** Emisor de una alerta: un ciudadano activo. */
	public Usuario buscarCiudadanoActivo(Long id) {
		return usuarios.findByIdAndRol(id, RolUsuario.CIUDADANO)
				.filter(Usuario::isActivo)
				.orElseThrow(() -> new NoEncontradoException("No existe el ciudadano " + id + "."));
	}

	/** Las personas que este ciudadano cargó: a quienes traslada y sus contactos de confianza. */
	public List<Usuario> personasDe(Long ciudadanoId) {
		return usuarios.findByRegistradoPorIdAndActivoTrueOrderByNombreCompletoAsc(ciudadanoId);
	}

	/**
	 * Alta de una persona a cargo. No es una cuenta y no se verifica nada: el número que se pone suele ser el de
	 * quien la registra, así que varias personas pueden compartirlo.
	 */
	@Transactional
	public Usuario registrarPersona(Long ciudadanoId, String nombreCompleto, String telefono) {
		Usuario registrador = buscarCiudadanoActivo(ciudadanoId);
		return usuarios.save(Usuario.registrarDependiente(nombreCompleto.trim(), telefono.trim(), registrador));
	}

	/** Baja de una persona a cargo. Solo la puede dar de baja quien la registró. */
	@Transactional
	public void olvidarPersona(Long ciudadanoId, Long personaId) {
		Usuario persona = usuarios.findById(personaId)
				.filter(u -> u.getRegistradoPor() != null && u.getRegistradoPor().getId().equals(ciudadanoId))
				.orElseThrow(() -> new NoEncontradoException("No existe la persona " + personaId + "."));
		persona.desactivar();
		usuarios.save(persona);
	}

}
