package com.uem.ambulancias.seguridad.service;

import com.uem.ambulancias.flota.service.ParamedicoConAsignacion;
import com.uem.ambulancias.flota.service.ServicioParamedicoService;
import com.uem.ambulancias.usuarios.domain.RolUsuario;
import com.uem.ambulancias.usuarios.domain.Usuario;
import com.uem.ambulancias.usuarios.repository.UsuarioRepository;
import com.uem.ambulancias.usuarios.service.CiudadanoService;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Las tres puertas de entrada al sistema. Cada una termina en un token, y de ahí en adelante ninguna petición vuelve
 * a decir quién es: lo dice el token.
 */
@Service
@RequiredArgsConstructor
public class AutenticacionService {

	private final UsuarioRepository usuarios;
	private final CiudadanoService ciudadanoService;
	private final ServicioParamedicoService servicioParamedicoService;
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
	 * App del paramédico: se identifica con su teléfono, igual que antes. Todavía no hay clave, así que esto no
	 * prueba que sea él; lo que sí queda cerrado es que no pueda hacerse pasar por un administrador.
	 */
	@Transactional(readOnly = true)
	public SesionParamedico ingresarParamedico(String telefono) {
		ParamedicoConAsignacion identificado = servicioParamedicoService.identificar(telefono);
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

	public record SesionCiudadano(String token, Usuario ciudadano) {
	}

}
