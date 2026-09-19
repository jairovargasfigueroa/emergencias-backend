package com.uem.ambulancias.seguridad.service;

import com.uem.ambulancias.usuarios.domain.RolUsuario;
import com.uem.ambulancias.usuarios.domain.Usuario;
import com.uem.ambulancias.usuarios.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Siembra el primer administrador al arrancar, si está configurado y todavía no existe ninguno. Se ejecuta en cada
 * arranque pero solo hace algo la primera vez: cambiar la clave en el servidor no pisa la que el administrador haya
 * puesto después.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInicial implements ApplicationRunner {

	private final UsuarioRepository usuarios;
	private final PasswordEncoder cifrador;
	private final AdminInicialProperties propiedades;

	@Override
	@Transactional
	public void run(ApplicationArguments argumentos) {
		if (usuarios.existsByRol(RolUsuario.ADMIN)) {
			return;
		}
		if (!propiedades.configurado()) {
			log.warn("No hay ningún administrador y sga.admin.correo/clave no están configurados: nadie puede entrar al panel.");
			return;
		}
		String correo = propiedades.correo().trim().toLowerCase();
		usuarios.save(Usuario.registrarAdmin(propiedades.nombre(), propiedades.telefono(), correo,
				cifrador.encode(propiedades.clave())));
		log.info("Administrador inicial creado con el correo {}.", correo);
	}

}
