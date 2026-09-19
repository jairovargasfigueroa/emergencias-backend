package com.uem.ambulancias.flota.domain;

import java.time.Instant;

import com.uem.ambulancias.comun.error.CodigoError;
import com.uem.ambulancias.comun.error.ConflictoException;
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
 * Desde cuándo y hasta cuándo un paramédico está trabajando. La asignación dice a qué ambulancia pertenece, que es un
 * dato de larga duración; el turno dice si hoy está adentro, que es lo que el sistema necesita para saber con qué
 * unidades cuenta ahora y a quién avisarle.
 *
 * <p>La ambulancia se congela al iniciar y no se lee de la asignación: si mañana lo reasignan a otra unidad, el
 * historial de anoche no tiene por qué cambiar.
 */
@Entity
@Table(name = "turno")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Turno {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Instant inicio;

	/** {@code null} mientras el turno siga abierto. */
	private Instant fin;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "paramedico_id", nullable = false)
	private Usuario paramedico;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "ambulancia_id", nullable = false)
	private Ambulancia ambulancia;

	public static Turno iniciar(Usuario paramedico, Ambulancia ambulancia, Instant inicio) {
		Turno turno = new Turno();
		turno.paramedico = paramedico;
		turno.ambulancia = ambulancia;
		turno.inicio = inicio;
		return turno;
	}

	public boolean isAbierto() {
		return fin == null;
	}

	public void terminar(Instant fin) {
		if (!isAbierto()) {
			throw new ConflictoException(CodigoError.TRANSICION_INVALIDA, "El turno " + id + " ya estaba terminado.");
		}
		this.fin = fin;
	}

}
