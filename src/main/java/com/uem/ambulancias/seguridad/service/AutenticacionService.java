package com.uem.ambulancias.seguridad.service;

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
		return new SesionParamedicoActivado(tokens.paraApp(activacion.identificado().paramedico()),
				activacion.identificado(), activacion.claveDispositivo());
	}

	/** App del paramédico: el teléfono dice quién es; el PIN y la clave del teléfono vinculado prueban que es él. */
	public SesionParamedico ingresarParamedico(String telefono, String pin, String claveDispositivo) {
		ParamedicoConAsignacion identificado = accesoParamedico.ingresar(telefono, pin, claveDispositivo);
		return new SesionParamedico(tokens.paraApp(identificado.paramedico()), identificado);
	}

	/** App del ciudadano: el registro ligero de PB-02 R1 es también su entrada. Repetirlo devuelve el mismo usuario. */
	@Transactional
	public SesionCiudadano registrarCiudadano(String nombreCompleto, String telefono) {
		Usuario ciudadano = ciudadanoService.registrar(nombreCompleto, telefono);
		return new SesionCiudadano(tokens.paraApp(ciudadano), ciudadano);
	}

	public record SesionAdmin(String token, Usuario admin) {
	}

	public record SesionParamedico(String token, ParamedicoConAsignacion identificado) {
	}

	public record SesionParamedicoActivado(String token, ParamedicoConAsignacion identificado,
			String claveDispositivo) {
	}

	public record SesionCiudadano(String token, Usuario ciudadano) {
	}

}
