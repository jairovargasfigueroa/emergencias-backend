package com.uem.ambulancias.seguridad.service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;

import com.uem.ambulancias.comun.error.CodigoError;
import com.uem.ambulancias.comun.error.ConflictoException;
import com.uem.ambulancias.comun.error.NoEncontradoException;
import com.uem.ambulancias.usuarios.domain.RolUsuario;
import com.uem.ambulancias.usuarios.domain.Usuario;
import com.uem.ambulancias.usuarios.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cómo prueba un paramédico que es él, igual que en las centrales de verdad: la central crea su cuenta y le entrega
 * en persona un código de activación; con ese código crea su PIN en la app, y ese teléfono queda vinculado a su
 * cuenta. De ahí en adelante entra, y abre cada turno, con su PIN y desde ese teléfono.
 */
@Service
@RequiredArgsConstructor
public class AccesoParamedicoService {

	/** Sin 0, O, 1, I ni L: el código se dicta o se copia de un papel, y esos se confunden entre sí. */
	private static final String ALFABETO_CODIGO = "23456789ABCDEFGHJKMNPQRSTUVWXYZ";

	private static final int LARGO_CODIGO = 8;

	private final UsuarioRepository usuarios;
	private final PasswordEncoder cifrador;
	private final SeguridadProperties propiedades;
	private final SecureRandom azar = new SecureRandom();

	/**
	 * Código para que el paramédico active su app: la primera vez, cuando cambia de teléfono o cuando se le bloqueó
	 * el PIN. Reemplaza al anterior sin usar. Se muestra una sola vez; acá solo queda cifrado.
	 */
	@Transactional
	public CodigoDeActivacion generarCodigoActivacion(Long paramedicoId) {
		Usuario paramedico = usuarios.buscarParaActualizar(paramedicoId, RolUsuario.PARAMEDICO)
				.orElseThrow(() -> new NoEncontradoException("No existe el paramédico " + paramedicoId + "."));
		if (!paramedico.isActivo()) {
			throw new ConflictoException(CodigoError.PARAMEDICO_INACTIVO,
					paramedico.getNombreCompleto() + " está desactivado. Actívalo antes de generarle un código.");
		}
		String codigo = codigoAleatorio();
		Instant venceEn = Instant.now().plus(Duration.ofHours(propiedades.horasCodigoActivacion()));
		paramedico.emitirCodigoActivacion(cifrador.encode(codigo), venceEn);
		return new CodigoDeActivacion(codigo.substring(0, 4) + "-" + codigo.substring(4), venceEn);
	}

	private String codigoAleatorio() {
		StringBuilder codigo = new StringBuilder(LARGO_CODIGO);
		for (int i = 0; i < LARGO_CODIGO; i++) {
			codigo.append(ALFABETO_CODIGO.charAt(azar.nextInt(ALFABETO_CODIGO.length())));
		}
		return codigo.toString();
	}

	/** El código tal como se le muestra al administrador, {@code XXXX-XXXX}, y hasta cuándo sirve. */
	public record CodigoDeActivacion(String codigo, Instant venceEn) {
	}

}
