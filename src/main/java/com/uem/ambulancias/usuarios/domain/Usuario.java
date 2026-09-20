package com.uem.ambulancias.usuarios.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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

	/** Clave cifrada del administrador. Los demás roles entran sin clave por ahora. */
	private String clave;

	/** Token de notificaciones push del teléfono del paramédico. */
	@Column(length = 512)
	private String tokenPush;

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

	/** La clave nueva llega ya cifrada: la entidad nunca ve la original. */
	public void cambiarClave(String claveCifrada) {
		this.clave = claveCifrada;
	}

	/** Un dispositivo nuevo reemplaza al anterior. */
	public void registrarDispositivo(String tokenPush) {
		this.tokenPush = tokenPush;
	}

}
