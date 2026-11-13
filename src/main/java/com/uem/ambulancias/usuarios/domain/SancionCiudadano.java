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
 * Una desactivación o reactivación de la cuenta de un ciudadano: con su motivo, cuándo y quién. No se borra ni se
 * corrige: si fue un error, se reactiva, y queda también eso.
 */
@Entity
@Table(name = "sancion_ciudadano")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SancionCiudadano {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "ciudadano_id", nullable = false)
	private Usuario ciudadano;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TipoSancion tipo;

	@Column(nullable = false, length = 1000)
	private String motivo;

	@Column(nullable = false)
	private Instant fechaHora;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "administrador_id", nullable = false)
	private Usuario administrador;

	public static SancionCiudadano registrar(Usuario ciudadano, TipoSancion tipo, String motivo,
			Usuario administrador, Instant fechaHora) {
		SancionCiudadano sancion = new SancionCiudadano();
		sancion.ciudadano = ciudadano;
		sancion.tipo = tipo;
		sancion.motivo = motivo;
		sancion.administrador = administrador;
		sancion.fechaHora = fechaHora;
		return sancion;
	}

}
