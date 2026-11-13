package com.uem.ambulancias.cobros.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.uem.ambulancias.usuarios.domain.Usuario;

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
 * Lo que la central cobra de referencia por un tipo de servicio, desde un día. No se edita: una tarifa nueva se
 * registra con su propia vigencia y la anterior queda en el historial. Rige la de vigencia más reciente que ya empezó.
 */
@Entity
@Table(name = "tarifa_referencia")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TarifaReferencia {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TipoServicio tipoServicio;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal monto;

	@Column(nullable = false)
	private LocalDate vigenteDesde;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "registrada_por_id", nullable = false)
	private Usuario registradaPor;

	@Column(nullable = false)
	private Instant fechaRegistro;

	public static TarifaReferencia registrar(TipoServicio tipoServicio, BigDecimal monto, LocalDate vigenteDesde,
			Usuario registradaPor, Instant ahora) {
		TarifaReferencia tarifa = new TarifaReferencia();
		tarifa.tipoServicio = tipoServicio;
		tarifa.monto = monto;
		tarifa.vigenteDesde = vigenteDesde;
		tarifa.registradaPor = registradaPor;
		tarifa.fechaRegistro = ahora;
		return tarifa;
	}

}
