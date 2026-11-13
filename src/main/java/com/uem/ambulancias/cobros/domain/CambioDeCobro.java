package com.uem.ambulancias.cobros.domain;

import java.math.BigDecimal;
import java.time.Instant;

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

/** Un ajuste o una anulación de un cobro: lo que había, lo que quedó, por qué, cuándo y quién. No se modifica. */
@Entity
@Table(name = "cambio_de_cobro")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CambioDeCobro {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "cobro_id", nullable = false)
	private Cobro cobro;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TipoCambioDeCobro tipo;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ModalidadPago modalidadAnterior;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal importeAnterior;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private EstadoCobro estadoAnterior;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ModalidadPago modalidadNueva;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal importeNuevo;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private EstadoCobro estadoNuevo;

	@Column(nullable = false, length = 1000)
	private String motivo;

	@Column(nullable = false)
	private Instant fechaHora;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "responsable_id", nullable = false)
	private Usuario responsable;

	static CambioDeCobro de(Cobro cobro, TipoCambioDeCobro tipo, ModalidadPago modalidadNueva, BigDecimal importeNuevo,
			EstadoCobro estadoNuevo, String motivo, Usuario responsable, Instant ahora) {
		CambioDeCobro cambio = new CambioDeCobro();
		cambio.cobro = cobro;
		cambio.tipo = tipo;
		cambio.modalidadAnterior = cobro.getModalidad();
		cambio.importeAnterior = cobro.getImporte();
		cambio.estadoAnterior = cobro.getEstado();
		cambio.modalidadNueva = modalidadNueva;
		cambio.importeNuevo = importeNuevo;
		cambio.estadoNuevo = estadoNuevo;
		cambio.motivo = motivo;
		cambio.fechaHora = ahora;
		cambio.responsable = responsable;
		return cambio;
	}

}
