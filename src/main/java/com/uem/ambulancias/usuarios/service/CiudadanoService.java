package com.uem.ambulancias.usuarios.service;

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
		return usuarios.findFirstByTelefonoAndRolOrderByIdAsc(telefonoLimpio, RolUsuario.CIUDADANO)
				.orElseGet(() -> usuarios.save(Usuario.registrarCiudadano(nombreCompleto.trim(), telefonoLimpio)));
	}

	/** Emisor de una alerta: un ciudadano activo. */
	public Usuario buscarCiudadanoActivo(Long id) {
		return usuarios.findByIdAndRol(id, RolUsuario.CIUDADANO)
				.filter(Usuario::isActivo)
				.orElseThrow(() -> new NoEncontradoException("No existe el ciudadano " + id + "."));
	}

}
