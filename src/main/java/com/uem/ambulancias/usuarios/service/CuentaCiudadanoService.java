package com.uem.ambulancias.usuarios.service;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uem.ambulancias.comun.error.CodigoError;
import com.uem.ambulancias.comun.error.ConflictoException;
import com.uem.ambulancias.comun.error.NoEncontradoException;
import com.uem.ambulancias.usuarios.domain.RolUsuario;
import com.uem.ambulancias.usuarios.domain.SancionCiudadano;
import com.uem.ambulancias.usuarios.domain.TipoSancion;
import com.uem.ambulancias.usuarios.domain.Usuario;
import com.uem.ambulancias.usuarios.repository.SancionCiudadanoRepository;
import com.uem.ambulancias.usuarios.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

/**
 * PB-23: la central desactiva la cuenta de un ciudadano que hace mal uso del servicio, y la reactiva si hace falta.
 * Cada decisión queda con su motivo, su fecha y quién la tomó.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CuentaCiudadanoService {

	private final UsuarioRepository usuarios;
	private final SancionCiudadanoRepository sanciones;

	/** Solo cuentas propias: una persona a cargo no entra a la app, no hay nada que desactivarle. */
	public Usuario buscarCuenta(Long ciudadanoId) {
		return usuarios.findByIdAndRol(ciudadanoId, RolUsuario.CIUDADANO)
				.filter(Usuario::esCuentaPropia)
				.orElseThrow(() -> new NoEncontradoException("No existe el ciudadano " + ciudadanoId + "."));
	}

	public List<SancionCiudadano> sancionesDe(Long ciudadanoId) {
		return sanciones.buscarPorCiudadano(ciudadanoId);
	}

	@Transactional
	public Usuario desactivar(Long ciudadanoId, Long administradorId, String motivo) {
		Usuario ciudadano = bloquear(ciudadanoId);
		if (!ciudadano.isActivo()) {
			throw new ConflictoException(CodigoError.TRANSICION_INVALIDA, "La cuenta ya está desactivada.");
		}
		Instant ahora = Instant.now();
		ciudadano.desactivarCuenta(ahora);
		sanciones.save(SancionCiudadano.registrar(ciudadano, TipoSancion.DESACTIVACION, motivo.trim(),
				administrador(administradorId), ahora));
		return ciudadano;
	}

	@Transactional
	public Usuario reactivar(Long ciudadanoId, Long administradorId, String motivo) {
		Usuario ciudadano = bloquear(ciudadanoId);
		if (ciudadano.isActivo()) {
			throw new ConflictoException(CodigoError.TRANSICION_INVALIDA, "La cuenta ya está activa.");
		}
		ciudadano.activar();
		sanciones.save(SancionCiudadano.registrar(ciudadano, TipoSancion.REACTIVACION, motivo.trim(),
				administrador(administradorId), Instant.now()));
		return ciudadano;
	}

	private Usuario bloquear(Long ciudadanoId) {
		return usuarios.buscarParaActualizar(ciudadanoId, RolUsuario.CIUDADANO)
				.filter(Usuario::esCuentaPropia)
				.orElseThrow(() -> new NoEncontradoException("No existe el ciudadano " + ciudadanoId + "."));
	}

	private Usuario administrador(Long administradorId) {
		return usuarios.findByIdAndRol(administradorId, RolUsuario.ADMIN)
				.orElseThrow(() -> new NoEncontradoException("No existe el administrador " + administradorId + "."));
	}

}
