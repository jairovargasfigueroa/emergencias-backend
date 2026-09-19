package com.uem.ambulancias.emergencias.domain;

import java.time.Instant;

import com.uem.ambulancias.comun.error.CodigoError;
import com.uem.ambulancias.comun.error.ConflictoException;
import com.uem.ambulancias.comun.geo.Geo;
import com.uem.ambulancias.flota.domain.Ambulancia;

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
import org.locationtech.jts.geom.Point;

/**
 * Lo que una unidad hace en un incidente. Cada hito congela su hora y su ubicación; el paciente se
 * registra aquí porque cada unidad recoge al suyo.
 */
@Entity
@Table(name = "atencion")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Atencion {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Instant horaToma;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private EstadoAtencion estado;

	private Instant horaLlegada;

	@Column(columnDefinition = Geo.COLUMNA_PUNTO)
	private Point ubicacionLlegada;

	private Instant horaRecogida;

	@Column(columnDefinition = Geo.COLUMNA_PUNTO)
	private Point ubicacionRecogida;

	private Instant horaLlegadaHospital;

	@Column(columnDefinition = Geo.COLUMNA_PUNTO)
	private Point ubicacionLlegadaHospital;

	private Instant horaEntrega;

	@Column(columnDefinition = Geo.COLUMNA_PUNTO)
	private Point ubicacionEntrega;

	@Enumerated(EnumType.STRING)
	private MotivoSinTraslado motivoSinTraslado;

	private Instant horaSinTraslado;

	@Column(columnDefinition = Geo.COLUMNA_PUNTO)
	private Point ubicacionSinTraslado;

	/** Cuándo quedó libre la unidad. Mientras sea {@code null}, sigue ocupada aunque el paciente ya esté entregado. */
	private Instant horaLiberacion;

	@Enumerated(EnumType.STRING)
	private MotivoCancelacionAtencion motivoCancelacion;

	private Instant horaCancelacion;

	private String nombrePaciente;

	private String documentoPaciente;

	@Column(columnDefinition = "text")
	private String destinoDescripcion;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "incidente_id", nullable = false)
	private Incidente incidente;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "ambulancia_id", nullable = false)
	private Ambulancia ambulancia;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "centro_salud_id")
	private CentroSalud centroSalud;

	/** ME-1 A0: la atención nace EN_CAMINO cuando la unidad toma o se suma al incidente. */
	public static Atencion iniciar(Incidente incidente, Ambulancia ambulancia, Instant horaToma) {
		Atencion atencion = new Atencion();
		atencion.incidente = incidente;
		atencion.ambulancia = ambulancia;
		atencion.horaToma = horaToma;
		atencion.estado = EstadoAtencion.EN_CAMINO;
		return atencion;
	}

	/**
	 * ME-1 A1 a A3: valida que {@code nuevo} sea el siguiente estado y, en la misma operación, congela la hora y la
	 * ubicación de ese hito. No se saltan estados ni se retrocede, así que un hito congelado nunca se sobrescribe.
	 */
	public void marcarHito(EstadoAtencion nuevo, Point ubicacion) {
		if (estado.siguienteHito() != nuevo) {
			throw transicionInvalida(nuevo);
		}
		Instant ahora = Instant.now();
		switch (nuevo) {
			case EN_EL_LUGAR -> {
				horaLlegada = ahora;
				ubicacionLlegada = ubicacion;
			}
			case PACIENTE_RECOGIDO -> {
				horaRecogida = ahora;
				ubicacionRecogida = ubicacion;
			}
			case EN_HOSPITAL -> {
				horaLlegadaHospital = ahora;
				ubicacionLlegadaHospital = ubicacion;
			}
			case PACIENTE_ENTREGADO -> {
				horaEntrega = ahora;
				ubicacionEntrega = ubicacion;
			}
			default -> throw transicionInvalida(nuevo);
		}
		estado = nuevo;
	}

	/**
	 * ME-1 A3 con su destino: la ubicación siempre, el centro del catálogo si se indicó y una descripción libre para
	 * lo no catalogado. La entrega nunca se bloquea por el catálogo.
	 */
	public void entregar(Point ubicacion, CentroSalud centroSalud, String destinoDescripcion) {
		marcarHito(EstadoAtencion.PACIENTE_ENTREGADO, ubicacion);
		this.centroSalud = centroSalud;
		this.destinoDescripcion = destinoDescripcion;
	}

	/**
	 * La salida que no traslada a nadie: se lo atendió ahí, se negó, no había nadie, ya se lo habían llevado o
	 * falleció. Solo desde el lugar, porque es lo que se encontró allí; con el paciente ya a bordo no aplica.
	 */
	public void cerrarSinTraslado(MotivoSinTraslado motivo, Point ubicacion) {
		if (motivo == null) {
			throw new IllegalArgumentException("El motivo del cierre sin traslado es obligatorio.");
		}
		if (estado != EstadoAtencion.EN_EL_LUGAR) {
			throw transicionInvalida(EstadoAtencion.SIN_TRASLADO);
		}
		estado = EstadoAtencion.SIN_TRASLADO;
		motivoSinTraslado = motivo;
		horaSinTraslado = Instant.now();
		ubicacionSinTraslado = ubicacion;
	}

	/**
	 * La unidad se desocupa. Es un hito aparte de la entrega: entre dejar al paciente en el hospital y quedar libre
	 * pasan la entrega al médico, el papeleo y la limpieza, y en ese rato la unidad no puede recibir otra emergencia.
	 */
	public void liberar() {
		if (!estado.isResuelta()) {
			throw new ConflictoException(CodigoError.TRANSICION_INVALIDA,
					"La atención " + id + " todavía no terminó: no se puede liberar la unidad.");
		}
		if (horaLiberacion != null) {
			throw new ConflictoException(CodigoError.TRANSICION_INVALIDA,
					"La atención " + id + " ya se liberó.");
		}
		horaLiberacion = Instant.now();
	}

	/** La unidad sigue tomada por esta atención: trabajando, o ya resuelta pero todavía sin liberarse. */
	public boolean ocupaLaUnidad() {
		return estado.isActiva() || (estado.isResuelta() && horaLiberacion == null);
	}

	/** ME-1 A4: desde cualquier estado activo y con motivo. CANCELADA es terminal: nunca se reabre. */
	public void cancelar(MotivoCancelacionAtencion motivo) {
		if (motivo == null) {
			throw new IllegalArgumentException("El motivo de cancelación es obligatorio.");
		}
		if (!estado.isActiva()) {
			throw transicionInvalida(EstadoAtencion.CANCELADA);
		}
		estado = EstadoAtencion.CANCELADA;
		motivoCancelacion = motivo;
		horaCancelacion = Instant.now();
	}

	/** Datos del paciente: opcionales y editables solo mientras la atención esté activa (PB-05 R3). */
	public void actualizarPaciente(String nombrePaciente, String documentoPaciente) {
		if (!estado.isActiva()) {
			throw new ConflictoException(CodigoError.ATENCION_FINALIZADA,
					"La atención " + id + " ya terminó: no se pueden editar los datos del paciente.");
		}
		this.nombrePaciente = nombrePaciente;
		this.documentoPaciente = documentoPaciente;
	}

	private ConflictoException transicionInvalida(EstadoAtencion destino) {
		return new ConflictoException(CodigoError.TRANSICION_INVALIDA,
				"La atención " + id + " no puede pasar de " + estado + " a " + destino + ".");
	}

}
