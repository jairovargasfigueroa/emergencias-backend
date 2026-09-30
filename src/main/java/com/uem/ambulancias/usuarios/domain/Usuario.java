package com.uem.ambulancias.usuarios.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Única entidad para todos los actores, diferenciados por rol. Baja lógica con {@code activo}.
 */
@Entity
@Table(name = "usuario")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Usuario {

	/**
	 * Intentos equivocados que se toleran, del código de activación o del PIN, antes de que haga falta un código
	 * nuevo de la central.
	 */
	public static final int INTENTOS_PERMITIDOS = 5;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String nombreCompleto;

	@Column(nullable = false)
	private String telefono;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private RolUsuario rol;

	@Column(nullable = false)
	private boolean activo;

	/** Correo del administrador: con él entra al panel. Los demás roles no lo tienen. */
	@Column(unique = true)
	private String correo;

	/** Clave cifrada del administrador: con ella entra al panel. Los demás roles no la tienen. */
	private String clave;

	/**
	 * Código de activación del paramédico, cifrado. La central se lo entrega en persona y sirve una sola vez: para
	 * crear su PIN y vincular su teléfono. Nulo si no tiene uno pendiente.
	 */
	private String codigoActivacionCifrado;

	/** Hasta cuándo sirve el código de activación. */
	private Instant codigoActivacionVenceEn;

	/** Códigos equivocados desde que se generó el último. */
	private Integer codigoActivacionIntentos;

	/** PIN cifrado del paramédico: la app se lo pide para entrar y al iniciar cada turno. */
	private String pinCifrado;

	/** PINs equivocados seguidos. Un acierto los vuelve a cero. */
	private Integer pinIntentos;

	/** Cuándo se bloqueó el PIN por intentos fallidos. Nulo si no está bloqueado. */
	private Instant pinBloqueadoEn;

	/**
	 * Clave cifrada del teléfono vinculado. La genera el servidor al activar y solo la conoce la app de ese teléfono:
	 * activar otro teléfono la reemplaza, y el anterior queda afuera.
	 */
	private String claveDispositivoCifrada;

	/** Token de notificaciones push del teléfono del paramédico. */
	@Column(length = 512)
	private String tokenPush;

	/**
	 * Quién cargó a esta persona. Nulo significa que se registró sola y entra a la app; con valor es una entrada
	 * en la agenda de ese ciudadano —un familiar al que traslada, un contacto de confianza— que no inicia sesión
	 * ni tiene su teléfono verificado. Por eso varios dependientes pueden compartir un mismo número.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "registrado_por_id")
	private Usuario registradoPor;

	/** Personal registrado por el administrador: nace activo con rol PARAMEDICO. */
	public static Usuario registrarParamedico(String nombreCompleto, String telefono) {
		return nuevo(nombreCompleto, telefono, RolUsuario.PARAMEDICO);
	}

	/** Administrador del panel: entra con correo y clave, que llega ya cifrada. */
	public static Usuario registrarAdmin(String nombreCompleto, String telefono, String correo, String claveCifrada) {
		Usuario usuario = nuevo(nombreCompleto, telefono, RolUsuario.ADMIN);
		usuario.correo = correo;
		usuario.clave = claveCifrada;
		return usuario;
	}

	/** Registro ligero desde la app: nace activo con rol CIUDADANO. */
	public static Usuario registrarCiudadano(String nombreCompleto, String telefono) {
		return nuevo(nombreCompleto, telefono, RolUsuario.CIUDADANO);
	}

	/**
	 * Persona cargada por un ciudadano: su mamá, a la que traslada, o su hermano como contacto. No es una cuenta
	 * y no hay nada que verificar, porque el número que se pone suele ser el de quien la registra.
	 */
	public static Usuario registrarDependiente(String nombreCompleto, String telefono, Usuario registradoPor) {
		Usuario usuario = nuevo(nombreCompleto, telefono, RolUsuario.CIUDADANO);
		usuario.registradoPor = registradoPor;
		return usuario;
	}

	/** Si esta persona entra a la app por su cuenta o es solo una entrada en la agenda de otro. */
	public boolean esCuentaPropia() {
		return registradoPor == null;
	}

	private static Usuario nuevo(String nombreCompleto, String telefono, RolUsuario rol) {
		Usuario usuario = new Usuario();
		usuario.nombreCompleto = nombreCompleto;
		usuario.telefono = telefono;
		usuario.rol = rol;
		usuario.activo = true;
		return usuario;
	}

	/** Baja lógica: el usuario y su historial se conservan. */
	public void desactivar() {
		activo = false;
	}

	/** Deshace la baja lógica. Vuelve con todo lo suyo: su asignación nunca se cerró. */
	public void activar() {
		activo = true;
	}

	/**
	 * Corrige lo que se cargó mal. El teléfono no es un dato de contacto cualquiera: es con lo que el paramédico
	 * dice quién es al activar su app y al entrar, así que cambiarlo cambia el número con el que tiene que hacerlo.
	 */
	public void corregirDatos(String nombreCompleto, String telefono) {
		this.nombreCompleto = nombreCompleto;
		this.telefono = telefono;
	}

	/** La clave nueva llega ya cifrada: la entidad nunca ve la original. */
	public void cambiarClave(String claveCifrada) {
		this.clave = claveCifrada;
	}

	/** Un dispositivo nuevo reemplaza al anterior. */
	public void registrarDispositivo(String tokenPush) {
		this.tokenPush = tokenPush;
	}

	/**
	 * La central genera un código para que el paramédico active su app. Reemplaza al anterior sin usar y empieza
	 * con los intentos en cero. Llega ya cifrado: la entidad nunca ve el original.
	 */
	public void emitirCodigoActivacion(String codigoCifrado, Instant venceEn) {
		codigoActivacionCifrado = codigoCifrado;
		codigoActivacionVenceEn = venceEn;
		codigoActivacionIntentos = 0;
	}

	/** Hay un código sin usar ni anular. Que siga vigente se pregunta aparte. */
	public boolean tieneCodigoActivacion() {
		return codigoActivacionCifrado != null;
	}

	public boolean codigoActivacionVencido(Instant ahora) {
		return codigoActivacionVenceEn == null || !ahora.isBefore(codigoActivacionVenceEn);
	}

	/**
	 * Cuenta un código equivocado y devuelve cuántos intentos le quedan. Con el último el código deja de servir:
	 * probar códigos al azar no puede ser una forma de entrar.
	 */
	public int registrarCodigoFallido() {
		codigoActivacionIntentos = contar(codigoActivacionIntentos) + 1;
		int restantes = Math.max(0, INTENTOS_PERMITIDOS - codigoActivacionIntentos);
		if (restantes == 0) {
			anularCodigoActivacion();
		}
		return restantes;
	}

	/**
	 * Activación con el código de la central: queda el PIN nuevo, desbloqueado, y este teléfono como el único
	 * vinculado. El que estaba antes, si había, deja de servir. El código ya usado no vuelve a servir.
	 */
	public void activarAcceso(String pinCifrado, String claveDispositivoCifrada) {
		this.pinCifrado = pinCifrado;
		this.claveDispositivoCifrada = claveDispositivoCifrada;
		pinIntentos = 0;
		pinBloqueadoEn = null;
		anularCodigoActivacion();
	}

	/** Tiene PIN y un teléfono vinculado: ya puede entrar a su app. */
	public boolean isAccesoActivado() {
		return pinCifrado != null && claveDispositivoCifrada != null;
	}

	public boolean isPinBloqueado() {
		return pinBloqueadoEn != null;
	}

	/**
	 * Cuenta un PIN equivocado y devuelve cuántos intentos le quedan. Con el último el PIN se bloquea hasta que la
	 * central le genere un código nuevo: seis dígitos se adivinan si se deja probar sin límite.
	 */
	public int registrarPinFallido(Instant ahora) {
		pinIntentos = contar(pinIntentos) + 1;
		int restantes = Math.max(0, INTENTOS_PERMITIDOS - pinIntentos);
		if (restantes == 0) {
			pinBloqueadoEn = ahora;
		}
		return restantes;
	}

	/** Un PIN correcto borra los errores anteriores: el límite es de intentos seguidos. */
	public void registrarPinCorrecto() {
		pinIntentos = 0;
	}

	private void anularCodigoActivacion() {
		codigoActivacionCifrado = null;
		codigoActivacionVenceEn = null;
	}

	/** Las columnas de intentos llegan nulas en las filas que ya existían. */
	private static int contar(Integer intentos) {
		return intentos == null ? 0 : intentos;
	}

}
