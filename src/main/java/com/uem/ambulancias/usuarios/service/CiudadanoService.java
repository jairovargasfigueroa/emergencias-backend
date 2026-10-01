package com.uem.ambulancias.usuarios.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.uem.ambulancias.comun.error.CodigoError;
import com.uem.ambulancias.comun.error.ConflictoException;
import com.uem.ambulancias.comun.error.NoEncontradoException;
import com.uem.ambulancias.comun.web.Textos;
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
	 * Entrada con el número ya verificado por SMS. Si ese número tiene cuenta, es esa: la que la persona usaba desde
	 * antes, con todo lo suyo, y reinstalar la app no la duplica. Si no, se crea, pero solo con su nombre y con el
	 * aviso de privacidad aceptado: la primera vez, la app los pide y vuelve a llamar con el mismo token.
	 */
	@Transactional
	public Usuario ingresar(String telefonoVerificado, String nombreCompleto, Boolean aceptaPrivacidad) {
		String telefono = numeroNacional(telefonoVerificado);
		Instant ahora = Instant.now();
		Optional<Usuario> existente = usuarios.buscarCiudadanoPorTelefonoNacional(telefono);
		if (existente.isPresent()) {
			existente.get().marcarTelefonoVerificado(ahora);
			return existente.get();
		}
		String nombre = Textos.opcional(nombreCompleto);
		if (nombre == null || !Boolean.TRUE.equals(aceptaPrivacidad)) {
			throw new ConflictoException(CodigoError.NOMBRE_REQUERIDO,
					"Es la primera vez que entras con este número: escribe tu nombre y acepta el aviso de privacidad.");
		}
		return usuarios.save(Usuario.registrarCiudadano(nombre, telefono, ahora));
	}

	/**
	 * El número como se marca dentro del país: solo dígitos, y sin el 591 cuando es un número boliviano completo
	 * ({@code +59171234567} queda {@code 71234567}). Es la misma normalización que usa la búsqueda en la base.
	 */
	private static String numeroNacional(String telefono) {
		String digitos = telefono.replaceAll("[^0-9]", "");
		return digitos.length() == 11 && digitos.startsWith("591") ? digitos.substring(3) : digitos;
	}

	/**
	 * A dónde mandarle los avisos. Un teléfono nuevo reemplaza al anterior, y si ese teléfono estaba a nombre de otra
	 * cuenta, deja de estarlo en la misma transacción: los avisos de una familia no le llegan a otra.
	 */
	@Transactional
	public void registrarDispositivo(Long ciudadanoId, String tokenPush) {
		Usuario ciudadano = buscarCiudadanoActivo(ciudadanoId);
		String token = tokenPush.trim();
		usuarios.liberarTokenPush(token, ciudadanoId);
		ciudadano.registrarDispositivo(token);
	}

	/**
	 * Al cerrar sesión, el teléfono deja de recibir sus avisos: puede quedar en manos de otro de la familia. Vale
	 * aunque la cuenta esté dada de baja, porque salir de la app siempre tiene que poder hacerse.
	 */
	@Transactional
	public void quitarDispositivo(Long ciudadanoId) {
		usuarios.findByIdAndRol(ciudadanoId, RolUsuario.CIUDADANO)
				.orElseThrow(() -> new NoEncontradoException("No existe el ciudadano " + ciudadanoId + "."))
				.quitarTokenPush();
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
