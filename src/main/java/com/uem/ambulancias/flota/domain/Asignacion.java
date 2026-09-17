package com.uem.ambulancias.flota.domain;

import java.time.Instant;

import com.uem.ambulancias.usuarios.domain.Usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Vincula un paramédico con una ambulancia y conserva el historial. {@code fechaFin = null} es la
 * asignación vigente.
 */
@Entity
@Table(name = "asignacion")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Asignacion {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Instant fechaInicio;

	private Instant fechaFin;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "paramedico_id", nullable = false)
	private Usuario paramedico;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "ambulancia_id", nullable = false)
	private Ambulancia ambulancia;

	public static Asignacion iniciar(Usuario paramedico, Ambulancia ambulancia, Instant inicio) {
		Asignacion asignacion = new Asignacion();
		asignacion.paramedico = paramedico;
		asignacion.ambulancia = ambulancia;
		asignacion.fechaInicio = inicio;
		return asignacion;
	}

	/** Cierra la asignación vigente. Una asignación ya cerrada no cambia. */
	public void cerrar(Instant fin) {
		if (fechaFin == null) {
			fechaFin = fin;
		}
	}

	public boolean isVigente() {
		return fechaFin == null;
	}

}
