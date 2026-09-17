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

	/** Personal registrado por el administrador: nace activo con rol PARAMEDICO. */
	public static Usuario registrarParamedico(String nombreCompleto, String telefono) {
		Usuario usuario = new Usuario();
		usuario.nombreCompleto = nombreCompleto;
		usuario.telefono = telefono;
		usuario.rol = RolUsuario.PARAMEDICO;
		usuario.activo = true;
		return usuario;
	}

	/** Baja lógica: el usuario y su historial se conservan. */
	public void desactivar() {
		activo = false;
	}

}
