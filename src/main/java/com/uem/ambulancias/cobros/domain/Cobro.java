package com.uem.ambulancias.cobros.domain;

import java.math.BigDecimal;
import java.time.Instant;

import com.uem.ambulancias.comun.error.CodigoError;
import com.uem.ambulancias.comun.error.ConflictoException;
import com.uem.ambulancias.emergencias.domain.Atencion;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * El cobro de un servicio: una atención terminada. Es aparte de la atención y no la toca: cobrar, corregir o anular
 * no cambia cómo terminó el servicio. Cada corrección o anulación deja un {@link CambioDeCobro} con lo que había.
 */
@Entity
@Table(name = "cobro")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Cobro {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "atencion_id", nullable = false, unique = true)
	private Atencion atencion;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TipoServicio tipoServicio;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ModalidadPago modalidad;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal importe;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private EstadoCobro estado;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "registrado_por_id", nullable = false)
	private Usuario registradoPor;

	@Column(nullable = false)
	private Instant fechaRegistro;

	public static Cobro registrar(Atencion atencion, ModalidadPago modalidad, BigDecimal importe, EstadoCobro estado,
			Usuario responsable, Instant ahora) {
		exigirEstadoElegible(estado);
		Cobro cobro = new Cobro();
		cobro.atencion = atencion;
		cobro.tipoServicio = TipoServicio.de(atencion);
		cobro.modalidad = modalidad;
		cobro.importe = importe;
		cobro.estado = estado;
		cobro.registradoPor = responsable;
		cobro.fechaRegistro = ahora;
		return cobro;
	}

	/** Corrige lo que haga falta. Lo anterior queda en el cambio que se devuelve. */
	public CambioDeCobro ajustar(ModalidadPago modalidad, BigDecimal importe, EstadoCobro estado, String motivo,
			Usuario responsable, Instant ahora) {
		exigirVigente();
		exigirEstadoElegible(estado);
		if (this.modalidad == modalidad && this.importe.compareTo(importe) == 0 && this.estado == estado) {
			throw new ConflictoException(CodigoError.TRANSICION_INVALIDA, "No cambió nada del cobro.");
		}
		CambioDeCobro cambio = CambioDeCobro.de(this, TipoCambioDeCobro.AJUSTE, modalidad, importe, estado, motivo,
				responsable, ahora);
		this.modalidad = modalidad;
		this.importe = importe;
		this.estado = estado;
		return cambio;
	}

	/** El servicio no se cobra. El importe que tenía queda en el cambio. */
	public CambioDeCobro anular(String motivo, Usuario responsable, Instant ahora) {
		exigirVigente();
		CambioDeCobro cambio = CambioDeCobro.de(this, TipoCambioDeCobro.ANULACION, modalidad, importe,
				EstadoCobro.ANULADO, motivo, responsable, ahora);
		this.estado = EstadoCobro.ANULADO;
		return cambio;
	}

	private void exigirVigente() {
		if (estado == EstadoCobro.ANULADO) {
			throw new ConflictoException(CodigoError.COBRO_ANULADO, "El cobro está anulado y ya no se modifica.");
		}
	}

	/** A anulado se llega solo anulando, que pide su motivo y lo deja dicho como anulación. */
	private static void exigirEstadoElegible(EstadoCobro estado) {
		if (estado == EstadoCobro.ANULADO) {
			throw new ConflictoException(CodigoError.TRANSICION_INVALIDA,
					"Para anular un cobro, usa la anulación: pide el motivo.");
		}
	}

}
