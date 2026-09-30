package com.uem.ambulancias.seguridad.service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;

import com.uem.ambulancias.comun.error.CodigoError;
import com.uem.ambulancias.comun.error.ConflictoException;
import com.uem.ambulancias.comun.error.NoEncontradoException;
import com.uem.ambulancias.flota.service.ParamedicoConAsignacion;
import com.uem.ambulancias.flota.service.ServicioParamedicoService;
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
 *
 * <p>Los intentos equivocados se cuentan aunque la operación se rechace. Por eso los métodos que verifican confirman
 * su transacción también cuando terminan en un 409: si se deshiciera, el contador nunca llegaría al límite. Antes de
 * ese rechazo no cambia nada más.
 */
@Service
@RequiredArgsConstructor
public class AccesoParamedicoService {

	/** Sin 0, O, 1, I ni L: el código se dicta o se copia de un papel, y esos se confunden entre sí. */
	private static final String ALFABETO_CODIGO = "23456789ABCDEFGHJKMNPQRSTUVWXYZ";

	private static final int LARGO_CODIGO = 8;

	/** 256 bits: no se adivina, y cabe de sobra en lo que acepta el cifrado. */
	private static final int BYTES_CLAVE_DISPOSITIVO = 32;

	private final UsuarioRepository usuarios;
	private final ServicioParamedicoService servicioParamedico;
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

	/**
	 * Activa la app en un teléfono. El código prueba que es él; el PIN es lo que se le va a pedir de acá en adelante;
	 * y la clave que se genera, que solo guarda la app, ata la cuenta a este teléfono. Esa clave se devuelve en claro
	 * esta única vez.
	 */
	@Transactional(noRollbackFor = ConflictoException.class)
	public Activacion activar(String telefono, String codigo, String pin) {
		Usuario paramedico = buscarPorTelefonoParaActualizar(telefono);
		if (!paramedico.tieneCodigoActivacion()) {
			throw new IntentoFallidoException(CodigoError.CODIGO_ACTIVACION_INVALIDO,
					"No tienes un código de activación pendiente. Pídele uno a la central.", 0);
		}
		if (paramedico.codigoActivacionVencido(Instant.now())) {
			throw new ConflictoException(CodigoError.CODIGO_ACTIVACION_VENCIDO,
					"Tu código de activación venció. Pídele uno nuevo a la central.");
		}
		// Antes que el código: un PIN débil no tiene que gastarle un intento.
		exigirPinFuerte(pin);
		if (!cifrador.matches(normalizarCodigo(codigo), paramedico.getCodigoActivacionCifrado())) {
			int restantes = paramedico.registrarCodigoFallido();
			throw new IntentoFallidoException(CodigoError.CODIGO_ACTIVACION_INVALIDO, restantes == 0
					? "El código no es correcto y ya no sirve. Pídele uno nuevo a la central."
					: "El código no es correcto. " + quedan(restantes), restantes);
		}
		String claveDispositivo = claveAleatoria();
		paramedico.activarAcceso(cifrador.encode(pin), cifrador.encode(claveDispositivo));
		return new Activacion(servicioParamedico.servicioActual(paramedico.getId()), claveDispositivo);
	}

	/** Entrada a la app: el teléfono dice quién es; el PIN y la clave del teléfono vinculado lo prueban. */
	@Transactional(noRollbackFor = ConflictoException.class)
	public ParamedicoConAsignacion ingresar(String telefono, String pin, String claveDispositivo) {
		Usuario paramedico = buscarPorTelefonoParaActualizar(telefono);
		verificar(paramedico, pin, claveDispositivo);
		return servicioParamedico.servicioActual(paramedico.getId());
	}

	/**
	 * El PIN se pide otra vez al iniciar cada turno, como quien ficha al entrar: la sesión de la app dura meses, y el
	 * teléfono pudo quedar en otras manos. Valen las mismas reglas que al entrar.
	 */
	@Transactional(noRollbackFor = ConflictoException.class)
	public void verificarPin(Long paramedicoId, String pin, String claveDispositivo) {
		Usuario paramedico = usuarios.buscarParaActualizar(paramedicoId, RolUsuario.PARAMEDICO)
				.filter(Usuario::isActivo)
				.orElseThrow(() -> new NoEncontradoException("No existe un paramédico activo con id " + paramedicoId + "."));
		verificar(paramedico, pin, claveDispositivo);
	}

	/**
	 * Primero el teléfono y después el PIN: desde un teléfono que no es el suyo no se puede probar ningún PIN, así
	 * que nadie le bloquea la cuenta a otro sabiendo solo su número.
	 */
	private void verificar(Usuario paramedico, String pin, String claveDispositivo) {
		if (!paramedico.isAccesoActivado()) {
			throw new ConflictoException(CodigoError.PARAMEDICO_SIN_ACTIVAR,
					"Todavía no activaste tu cuenta. Pídele a la central tu código de activación.");
		}
		if (!cifrador.matches(claveDispositivo, paramedico.getClaveDispositivoCifrada())) {
			throw new ConflictoException(CodigoError.DISPOSITIVO_NO_VINCULADO,
					"Este teléfono no está vinculado a tu cuenta. Para usarlo, pídele a la central un código de activación.");
		}
		if (paramedico.isPinBloqueado()) {
			throw pinBloqueado();
		}
		if (!cifrador.matches(pin, paramedico.getPinCifrado())) {
			int restantes = paramedico.registrarPinFallido(Instant.now());
			if (restantes == 0) {
				throw pinBloqueado();
			}
			throw new IntentoFallidoException(CodigoError.PIN_INCORRECTO, "El PIN no es correcto. " + quedan(restantes),
					restantes);
		}
		paramedico.registrarPinCorrecto();
	}

	/**
	 * El paramédico activo con ese teléfono, igual que se lo identificaba antes, con su fila bloqueada: dos intentos a
	 * la vez no pueden contarse como uno solo.
	 */
	private Usuario buscarPorTelefonoParaActualizar(String telefono) {
		return usuarios.buscarIdsActivosPorTelefono(telefono.trim(), RolUsuario.PARAMEDICO).stream()
				.findFirst()
				.flatMap(id -> usuarios.buscarParaActualizar(id, RolUsuario.PARAMEDICO))
				.filter(Usuario::isActivo)
				.orElseThrow(() -> new NoEncontradoException("No hay un paramédico activo con ese teléfono."));
	}

	/** Todos los dígitos iguales o seguidos es lo primero que prueba quien agarra un teléfono ajeno. */
	private static void exigirPinFuerte(String pin) {
		boolean iguales = true;
		boolean ascendentes = true;
		boolean descendentes = true;
		for (int i = 1; i < pin.length(); i++) {
			int paso = pin.charAt(i) - pin.charAt(i - 1);
			iguales &= paso == 0;
			ascendentes &= paso == 1;
			descendentes &= paso == -1;
		}
		if (iguales || ascendentes || descendentes) {
			throw new ConflictoException(CodigoError.PIN_DEBIL,
					"Ese PIN es muy fácil de adivinar. No uses todos los dígitos iguales ni seguidos, como 111111 o 123456.");
		}
	}

	private static IntentoFallidoException pinBloqueado() {
		return new IntentoFallidoException(CodigoError.PIN_BLOQUEADO,
				"Tu PIN quedó bloqueado por demasiados intentos. Pídele a la central un código de activación para crear uno nuevo.",
				0);
	}

	private static String quedan(int restantes) {
		return restantes == 1 ? "Te queda 1 intento." : "Te quedan " + restantes + " intentos.";
	}

	/** El código vale como la persona lo escriba: con o sin guion, con espacios o en minúsculas. */
	private static String normalizarCodigo(String codigo) {
		return codigo.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
	}

	private String codigoAleatorio() {
		StringBuilder codigo = new StringBuilder(LARGO_CODIGO);
		for (int i = 0; i < LARGO_CODIGO; i++) {
			codigo.append(ALFABETO_CODIGO.charAt(azar.nextInt(ALFABETO_CODIGO.length())));
		}
		return codigo.toString();
	}

	private String claveAleatoria() {
		byte[] bytes = new byte[BYTES_CLAVE_DISPOSITIVO];
		azar.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	/** El código tal como se le muestra al administrador, {@code XXXX-XXXX}, y hasta cuándo sirve. */
	public record CodigoDeActivacion(String codigo, Instant venceEn) {
	}

	/** El paramédico recién activado y la clave de su teléfono, que no se vuelve a mostrar. */
	public record Activacion(ParamedicoConAsignacion identificado, String claveDispositivo) {
	}

}
