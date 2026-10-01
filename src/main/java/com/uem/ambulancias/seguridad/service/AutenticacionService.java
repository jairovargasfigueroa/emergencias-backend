package com.uem.ambulancias.seguridad.service;

import java.time.Instant;

import com.uem.ambulancias.flota.service.ParamedicoConAsignacion;
import com.uem.ambulancias.usuarios.domain.RolUsuario;
import com.uem.ambulancias.usuarios.domain.Usuario;
import com.uem.ambulancias.usuarios.repository.UsuarioRepository;
import com.uem.ambulancias.usuarios.service.CiudadanoService;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Las puertas de entrada al sistema. Cada una termina en un token, y de ahí en adelante ninguna petición vuelve a
 * decir quién es: lo dice el token.
 */
@Service
@RequiredArgsConstructor
public class AutenticacionService {

	private final UsuarioRepository usuarios;
	private final CiudadanoService ciudadanoService;
	private final AccesoParamedicoService accesoParamedico;
	private final VerificadorDeTelefono verificadorDeTelefono;
	private final PasswordEncoder cifrador;
	private final TokenService tokens;

	/** Panel: correo y clave. Un correo que no existe y una clave equivocada fallan igual y con el mismo mensaje. */
	@Transactional(readOnly = true)
	public SesionAdmin ingresarAdmin(String correo, String clave) {
		Usuario admin = usuarios.findByCorreoAndRol(correo.trim().toLowerCase(), RolUsuario.ADMIN)
				.filter(Usuario::isActivo)
				.filter(usuario -> usuario.getClave() != null && cifrador.matches(clave, usuario.getClave()))
				.orElseThrow(CredencialesInvalidasException::new);
		return new SesionAdmin(tokens.paraPanel(admin), admin);
	}

	/**
	 * App del paramédico, primera vez en un teléfono: con el código que le dio la central crea su PIN y deja el
	 * teléfono vinculado. Sale con la sesión abierta y con la clave del teléfono, que la app guarda.
	 *
	 * <p>Esta y la siguiente no abren transacción propia: la del acceso tiene que confirmarse aunque el código o el
	 * PIN fallen, para que el intento quede contado, y una transacción de afuera la desharía.
	 */
	public SesionParamedicoActivado activarParamedico(String telefono, String codigo, String pin) {
		AccesoParamedicoService.Activacion activacion = accesoParamedico.activar(telefono, codigo, pin);
		TokenService.TokenEmitido token = tokens.paraApp(activacion.identificado().paramedico());
		return new SesionParamedicoActivado(token.token(), token.venceEn(), activacion.identificado(),
				activacion.claveDispositivo());
	}

	/** App del paramédico: el teléfono dice quién es; el PIN y la clave del teléfono vinculado prueban que es él. */
	public SesionParamedico ingresarParamedico(String telefono, String pin, String claveDispositivo) {
		ParamedicoConAsignacion identificado = accesoParamedico.ingresar(telefono, pin, claveDispositivo);
		TokenService.TokenEmitido token = tokens.paraApp(identificado.paramedico());
		return new SesionParamedico(token.token(), token.venceEn(), identificado);
	}

	/**
	 * App del ciudadano: entra con su número verificado por SMS, que es lo que prueba que la cuenta es suya. La
	 * verificación va antes y afuera de la transacción: es una llamada a Firebase, y la base no tiene por qué quedar
	 * esperándola.
	 */
	public SesionCiudadano ingresarCiudadano(String idToken, String nombreCompleto, Boolean aceptaPrivacidad) {
		String telefono = verificadorDeTelefono.telefonoVerificado(idToken);
		Usuario ciudadano = ciudadanoService.ingresar(telefono, nombreCompleto, aceptaPrivacidad);
		TokenService.TokenEmitido token = tokens.paraApp(ciudadano);
		return new SesionCiudadano(token.token(), token.venceEn(), ciudadano);
	}

	/**
	 * Renueva la sesión de una app sin volver a pedir credenciales, mientras siga valiendo lo que la abrió: para el
	 * ciudadano, su número verificado; para el paramédico, el teléfono vinculado. Así una cuenta sin verificar se queda
	 * afuera cuando se le termina el token. Un teléfono que ya no es el del paramédico ni llega acá: su sesión se cerró
	 * cuando la central le generó el código. El nuevo dura lo mismo que al entrar.
	 */
	@Transactional(readOnly = true)
	public TokenService.TokenEmitido renovarSesion(Long usuarioId, String claveDispositivo) {
		Usuario usuario = usuarios.findById(usuarioId)
				.filter(Usuario::isActivo)
				.orElseThrow(() -> new SesionNoRenovableException("Tu cuenta ya no está activa."));
		switch (usuario.getRol()) {
			case CIUDADANO -> {
				if (!usuario.esCuentaPropia() || usuario.getTelefonoVerificadoEn() == null) {
					throw new SesionNoRenovableException("Verifica tu número para seguir usando la app.");
				}
			}
			case PARAMEDICO -> {
				if (!accesoParamedico.esDispositivoVinculado(usuario, claveDispositivo)) {
					throw new SesionNoRenovableException(
							"Este teléfono ya no está vinculado a tu cuenta. Actívalo con un código nuevo de la central.");
				}
			}
			default -> throw new SesionNoRenovableException("Esta sesión no se renueva.");
		}
		return tokens.paraApp(usuario);
	}

	public record SesionAdmin(String token, Usuario admin) {
	}

	public record SesionParamedico(String token, Instant venceEn, ParamedicoConAsignacion identificado) {
	}

	public record SesionParamedicoActivado(String token, Instant venceEn, ParamedicoConAsignacion identificado,
			String claveDispositivo) {
	}

	public record SesionCiudadano(String token, Instant venceEn, Usuario ciudadano) {
	}

}
