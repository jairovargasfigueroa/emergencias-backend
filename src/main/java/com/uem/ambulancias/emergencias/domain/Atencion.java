package com.uem.ambulancias.emergencias.domain;

import java.time.Duration;
import java.time.Instant;

import com.uem.ambulancias.comun.error.CodigoError;
import com.uem.ambulancias.comun.error.ConflictoException;
import com.uem.ambulancias.comun.geo.Geo;
import com.uem.ambulancias.flota.domain.Ambulancia;
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
import org.hibernate.annotations.Check;
import org.locationtech.jts.geom.Point;

/**
 * Lo que una unidad hace en un incidente o en un traslado. Cada hito congela su hora y su ubicación; el paciente
 * se registra aquí porque cada unidad recoge al suyo.
 */
@Entity
@Table(name = "atencion")
@Check(name = "ck_atencion_origen", constraints = "(incidente_id is null) <> (traslado_id is null)")
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

	/**
	 * Solo en traslados: la unidad llegó y el paciente no estaba listo. La espera termina sola en el hito
	 * siguiente, así que con una marca alcanza. Es tiempo de unidad que la empresa está pagando.
	 */
	private Instant horaAvisoNoListo;

	/**
	 * Solo en traslados: hasta cuándo espera la tripulación a ese paciente. Se fija al marcar que no estaba listo,
	 * con la tolerancia de ese momento, y antes de esa hora la unidad no puede retirarse por ese motivo.
	 */
	private Instant esperaHasta;

	private String nombrePaciente;

	private String documentoPaciente;

	@Column(columnDefinition = "text")
	private String destinoDescripcion;

	/** Nulo cuando la atención viene de un traslado. Exactamente uno de los dos padres está puesto. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "incidente_id")
	private Incidente incidente;

	/** Nulo cuando la atención viene de una emergencia. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "traslado_id")
	private Traslado traslado;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "ambulancia_id", nullable = false)
	private Ambulancia ambulancia;

	/**
	 * Quién responde por esta atención: el que marca los hitos y, cuando haya cobros, el que rinde. No es toda la
	 * tripulación, que se sabe por los turnos abiertos sobre esa unidad; es uno solo, y por eso se guarda.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "paramedico_responsable_id")
	private Usuario paramedicoResponsable;

	/**
	 * Quién asignó la unidad. Nulo significa que la asignó el sistema. Si con el tiempo resulta que casi todas
	 * las asigna el administrador a mano, eso está diciendo que la regla automática no sirve.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "asignado_por_id")
	private Usuario asignadoPor;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "centro_salud_id")
	private CentroSalud centroSalud;

	/** Quién la cerró desde la central porque la tripulación no podía. Nulo en todas las demás. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "cerrada_por_id")
	private Usuario cerradaPor;

	/** ME-1 A0: la atención nace EN_CAMINO cuando la unidad toma o se suma al incidente. */
	public static Atencion iniciar(Incidente incidente, Ambulancia ambulancia, Usuario paramedicoResponsable,
			Instant horaToma) {
		Atencion atencion = nueva(ambulancia, paramedicoResponsable, horaToma);
		atencion.incidente = incidente;
		return atencion;
	}

	/**
	 * La atención de un traslado nace igual que la de un incidente, con la misma máquina de hitos. Lo único
	 * distinto es de dónde cuelga y que el destino ya se conoce antes de salir.
	 */
	public static Atencion iniciarTraslado(Traslado traslado, Ambulancia ambulancia, Usuario paramedicoResponsable,
			Usuario asignadoPor, Instant horaToma) {
		Atencion atencion = nueva(ambulancia, paramedicoResponsable, horaToma);
		atencion.traslado = traslado;
		atencion.asignadoPor = asignadoPor;
		atencion.centroSalud = traslado.getCentroSaludDestino();
		atencion.destinoDescripcion = traslado.getDestinoDetalle();
		atencion.nombrePaciente = traslado.getPasajero().getNombreCompleto();
		return atencion;
	}

	private static Atencion nueva(Ambulancia ambulancia, Usuario paramedicoResponsable, Instant horaToma) {
		Atencion atencion = new Atencion();
		atencion.ambulancia = ambulancia;
		atencion.paramedicoResponsable = paramedicoResponsable;
		atencion.horaToma = horaToma;
		atencion.estado = EstadoAtencion.EN_CAMINO;
		return atencion;
	}

	public boolean esDeTraslado() {
		return traslado != null;
	}

	/** La mandó la central y no la tomó la tripulación por su cuenta. */
	public void marcarDespachadaPor(Usuario administrador) {
		asignadoPor = administrador;
	}

	/**
	 * Solo en traslados. Deja la marca, arranca la espera y no cambia el estado: la unidad sigue en la puerta, y
	 * si sube al paciente o se retira lo decide la tripulación cuando pase la tolerancia.
	 */
	public void marcarPacienteNoListo(Instant ahora, Duration espera) {
		if (estado != EstadoAtencion.EN_EL_LUGAR) {
			throw new ConflictoException(CodigoError.TRANSICION_INVALIDA,
					"Que el paciente no está listo se marca en la puerta, con la unidad ya en el lugar.");
		}
		if (horaAvisoNoListo == null) {
			horaAvisoNoListo = ahora;
			esperaHasta = ahora.plus(espera);
		}
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
		if (!motivo.valePara(OrigenAtencion.de(this))) {
			throw new ConflictoException(CodigoError.VALIDACION, "Ese motivo no corresponde a esta atención.");
		}
		if (estado != EstadoAtencion.EN_EL_LUGAR) {
			throw transicionInvalida(EstadoAtencion.SIN_TRASLADO);
		}
		if (motivo == MotivoSinTraslado.PACIENTE_NO_LISTO) {
			exigirEsperaCumplida();
		}
		estado = EstadoAtencion.SIN_TRASLADO;
		motivoSinTraslado = motivo;
		horaSinTraslado = Instant.now();
		ubicacionSinTraslado = ubicacion;
	}

	/**
	 * Retirarse porque el paciente no estaba listo es de después de la espera, como en cualquier servicio de
	 * traslados: primero se avisa que no está listo, y recién cuando pasa la tolerancia la unidad se puede ir.
	 */
	private void exigirEsperaCumplida() {
		if (esperaHasta == null) {
			throw new ConflictoException(CodigoError.ESPERA_EN_CURSO,
					"Primero marca que el paciente no está listo: desde ahí corre el tiempo de espera.");
		}
		if (Instant.now().isBefore(esperaHasta)) {
			throw new ConflictoException(CodigoError.ESPERA_EN_CURSO, "Todavía no terminó el tiempo de espera.");
		}
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

	/**
	 * La central la cancela porque la tripulación no responde. Solo antes de tener al paciente: el caso sigue su camino
	 * normal, con otra unidad si alguien la sigue esperando.
	 */
	public void cancelarDesdeLaCentral(Usuario administrador) {
		if (estado != EstadoAtencion.EN_CAMINO && estado != EstadoAtencion.EN_EL_LUGAR) {
			throw new ConflictoException(CodigoError.TRANSICION_INVALIDA,
					"Con el paciente a bordo no se cancela: dala por entregada.");
		}
		cancelar(MotivoCancelacionAtencion.CERRADA_POR_CENTRAL);
		cerradaPor = administrador;
	}

	/**
	 * La central la da por entregada cuando la tripulación no pudo marcarlo: llevaba al paciente a bordo, así que el
	 * viaje terminó en el destino. La hora es la de cuando se registra, no la real. Sin destino nuevo queda el que ya
	 * tenía, que en un traslado es el del pedido. La unidad se libera en el mismo paso.
	 */
	public void entregarDesdeLaCentral(CentroSalud centroSalud, String destinoDescripcion, Usuario administrador,
			Instant ahora) {
		if (estado != EstadoAtencion.PACIENTE_RECOGIDO && estado != EstadoAtencion.EN_HOSPITAL) {
			throw new ConflictoException(CodigoError.TRANSICION_INVALIDA,
					"Solo se da por entregada una atención con el paciente a bordo.");
		}
		estado = EstadoAtencion.PACIENTE_ENTREGADO;
		horaEntrega = ahora;
		if (centroSalud != null || destinoDescripcion != null) {
			this.centroSalud = centroSalud;
			this.destinoDescripcion = destinoDescripcion;
		}
		horaLiberacion = ahora;
		cerradaPor = administrador;
	}

	/** La central libera una unidad que ya había resuelto y no pudo marcar que quedó libre. */
	public void liberarDesdeLaCentral(Usuario administrador) {
		liberar();
		cerradaPor = administrador;
	}

	/** La unidad sigue tomada por esta atención: trabajando, o ya resuelta pero todavía sin liberarse. */
	public boolean ocupaLaUnidad() {
		return estado.isActiva() || (estado.isResuelta() && horaLiberacion == null);
	}

	/**
	 * La cancelación que pide la propia tripulación. No puede usar los motivos del sistema —el solicitante que
	 * cancela, el administrador que reasigna—, y en un traslado hay motivos que solo tienen sentido antes de tener al
	 * paciente: devolverlo es de antes de llegar, y desviarse a otra cosa, de antes de subirlo.
	 */
	public void cancelarPorLaTripulacion(MotivoCancelacionAtencion motivo) {
		switch (motivo) {
			case CANCELADA_POR_SOLICITANTE, REASIGNADA, CERRADA_POR_CENTRAL ->
					throw motivoInvalido("Ese motivo no lo elige la tripulación.");
			case RECHAZADA_POR_PARAMEDICO -> {
				if (!esDeTraslado()) {
					throw motivoInvalido("Solo un traslado se puede devolver.");
				}
				if (estado != EstadoAtencion.EN_CAMINO) {
					throw motivoInvalido("Un traslado se devuelve antes de llegar a buscar al paciente.");
				}
			}
			case DESVIADA -> {
				if (esDeTraslado() && estado != EstadoAtencion.EN_CAMINO && estado != EstadoAtencion.EN_EL_LUGAR) {
					throw motivoInvalido("Con el paciente a bordo, la unidad no se puede desviar.");
				}
			}
			case NO_SE_ENCONTRO_PACIENTE -> {
				if (esDeTraslado()) {
					throw motivoInvalido("En un traslado, si no hay nadie en la puerta se cierra sin traslado.");
				}
			}
			default -> {
				// Avería y otro motivo pueden pasar en cualquier momento.
			}
		}
		cancelar(motivo);
	}

	private static ConflictoException motivoInvalido(String mensaje) {
		return new ConflictoException(CodigoError.VALIDACION, mensaje);
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
