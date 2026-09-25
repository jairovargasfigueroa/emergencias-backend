package com.uem.ambulancias.emergencias.domain;

import java.time.Instant;

import com.uem.ambulancias.comun.error.CodigoError;
import com.uem.ambulancias.comun.error.ConflictoException;
import com.uem.ambulancias.comun.geo.Geo;
import com.uem.ambulancias.flota.domain.TipoUnidad;
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
import org.hibernate.annotations.Checks;
import org.locationtech.jts.geom.Point;

/**
 * Pedido de traslado. Nace cuando el ciudadano lo pide y vive aunque nunca llegue a ejecutarse. No guarda la
 * ambulancia: eso pertenece al trabajo, y el trabajo es la {@link Atencion} que se crea al asignar.
 */
@Entity
@Table(name = "traslado")
@Checks({
		@Check(name = "ck_traslado_ventana", constraints = "hora_limite_salida >= hora_salida_estimada"),
		@Check(name = "ck_traslado_contacto", constraints = "(contacto_nombre is null) = (contacto_telefono is null)"),
		@Check(name = "ck_traslado_horario", constraints = "(modo_horario = 'PROGRAMADO') = (hora_cita is not null)") })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Traslado {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "solicitante_id", nullable = false)
	private Usuario solicitante;

	/** Quien viaja. Puede ser el mismo solicitante, o alguien que él registró y no entra a la app. */
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "pasajero_id", nullable = false)
	private Usuario pasajero;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Movilidad movilidad;

	@Column(nullable = false)
	private boolean requiereOxigeno;

	@Column(nullable = false)
	private boolean requiereEquipo;

	@Column(nullable = false)
	private boolean requiereAislamiento;

	private Integer pesoAproximado;

	@Column(nullable = false)
	private int acompanantes;

	private String observaciones;

	/** Lo que eligió el ciudadano. No cambia nunca, aunque después se descubra que estaba mal. */
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TipoUnidad tipoUnidadPedido;

	/** Lo que de verdad hacía falta, cuando alguien lo corrige. Nulo mientras nadie lo toque. */
	@Enumerated(EnumType.STRING)
	private TipoUnidad tipoUnidadCorregido;

	@Column(nullable = false, columnDefinition = Geo.COLUMNA_PUNTO)
	private Point origen;

	/** "Portón verde, casa de dos pisos". Es lo que hace que la ambulancia encuentre la puerta. */
	private String origenReferencia;

	/** Quien recibe a la ambulancia en el origen. Nulo significa que es el propio solicitante. */
	private String contactoNombre;

	private String contactoTelefono;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "centro_salud_destino_id")
	private CentroSalud centroSaludDestino;

	/** Siempre presente: si el destino es un centro, se copia su punto al pedir. */
	@Column(nullable = false, columnDefinition = Geo.COLUMNA_PUNTO)
	private Point destino;

	/** A qué área llega: "Diálisis", "Emergencias". */
	private String destinoDetalle;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ModoHorario modoHorario;

	/** A qué hora tiene que estar en el destino. Nula si es para ahora. */
	private Instant horaCita;

	@Column(nullable = false)
	private Instant horaSalidaEstimada;

	/** Última salida posible para llegar a tiempo. Pasada esta hora ya no tiene sentido seguir buscando. */
	@Column(nullable = false)
	private Instant horaLimiteSalida;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private EstadoTraslado estado;

	@Column(nullable = false)
	private Instant fechaHoraCreacion;

	private Instant fechaHoraCancelacion;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "cancelado_por_id")
	private Usuario canceladoPor;

	/**
	 * Un pedido programado nace esperando su día; uno inmediato nace ya buscando unidad. En ninguno de los dos
	 * casos se reserva una ambulancia: eso pasa recién a la hora de salir.
	 */
	public static Traslado registrar(Usuario solicitante, Usuario pasajero, Necesidades necesidades,
			Point origen, String origenReferencia, String contactoNombre, String contactoTelefono,
			CentroSalud centroSaludDestino, Point destino, String destinoDetalle,
			Horario horario, TipoUnidad tipoUnidadPedido, Instant ahora) {

		Traslado traslado = new Traslado();
		traslado.solicitante = solicitante;
		traslado.pasajero = pasajero;
		traslado.movilidad = necesidades.movilidad();
		traslado.requiereOxigeno = necesidades.oxigeno();
		traslado.requiereEquipo = necesidades.equipo();
		traslado.requiereAislamiento = necesidades.aislamiento();
		traslado.pesoAproximado = necesidades.pesoAproximado();
		traslado.acompanantes = necesidades.acompanantes();
		traslado.observaciones = necesidades.observaciones();
		traslado.tipoUnidadPedido = tipoUnidadPedido;
		traslado.origen = origen;
		traslado.origenReferencia = origenReferencia;
		traslado.contactoNombre = contactoNombre;
		traslado.contactoTelefono = contactoTelefono;
		traslado.centroSaludDestino = centroSaludDestino;
		traslado.destino = destino;
		traslado.destinoDetalle = destinoDetalle;
		traslado.modoHorario = horario.modo();
		traslado.horaCita = horario.horaCita();
		traslado.horaSalidaEstimada = horario.salidaEstimada();
		traslado.horaLimiteSalida = horario.limiteSalida();
		traslado.estado = horario.modo() == ModoHorario.INMEDIATO
				? EstadoTraslado.BUSCANDO_UNIDAD
				: EstadoTraslado.PROGRAMADO;
		traslado.fechaHoraCreacion = ahora;
		return traslado;
	}

	/**
	 * Cambiar el pedido entero. Solo mientras no haya salido nadie: con una unidad en camino, el paramédico ya
	 * se fue con otra información, y mandarlo a otro lado sin avisarle no es una edición, es otro viaje.
	 *
	 * <p>Se limpia la corrección del tipo de unidad: se corrigió sobre datos que acaban de cambiar.
	 */
	public void reprogramar(Necesidades necesidades, Point origen, String origenReferencia, String contactoNombre,
			String contactoTelefono, CentroSalud centroSaludDestino, Point destino, String destinoDetalle,
			Horario horario, TipoUnidad tipoUnidadPedido) {
		exigirQueNadieHayaSalido();
		this.movilidad = necesidades.movilidad();
		this.requiereOxigeno = necesidades.oxigeno();
		this.requiereEquipo = necesidades.equipo();
		this.requiereAislamiento = necesidades.aislamiento();
		this.pesoAproximado = necesidades.pesoAproximado();
		this.acompanantes = necesidades.acompanantes();
		this.observaciones = necesidades.observaciones();
		this.tipoUnidadPedido = tipoUnidadPedido;
		this.tipoUnidadCorregido = null;
		this.origen = origen;
		this.origenReferencia = origenReferencia;
		this.contactoNombre = contactoNombre;
		this.contactoTelefono = contactoTelefono;
		this.centroSaludDestino = centroSaludDestino;
		this.destino = destino;
		this.destinoDetalle = destinoDetalle;
		this.modoHorario = horario.modo();
		this.horaCita = horario.horaCita();
		this.horaSalidaEstimada = horario.salidaEstimada();
		this.horaLimiteSalida = horario.limiteSalida();
		// Si movieron la cita para más tarde, deja de ser hora de salir y el pedido vuelve a esperar su día.
		this.estado = horario.modo() == ModoHorario.INMEDIATO
				? EstadoTraslado.BUSCANDO_UNIDAD
				: EstadoTraslado.PROGRAMADO;
	}

	/**
	 * Lo que solo ayuda a la tripulación a encontrar la puerta y a hablar con alguien. No cambia ninguna decisión
	 * que el sistema ya tomó, así que se puede corregir hasta con la unidad en camino, que es cuando más sirve.
	 */
	public void actualizarDetalles(String origenReferencia, String contactoNombre, String contactoTelefono,
			String observaciones) {
		if (!estado.isVigente()) {
			throw new ConflictoException(CodigoError.TRASLADO_FINALIZADO,
					"El traslado ya terminó y no se puede editar.");
		}
		this.origenReferencia = origenReferencia;
		this.contactoNombre = contactoNombre;
		this.contactoTelefono = contactoTelefono;
		this.observaciones = observaciones;
	}

	private void exigirQueNadieHayaSalido() {
		if (estado != EstadoTraslado.PROGRAMADO && estado != EstadoTraslado.BUSCANDO_UNIDAD) {
			throw new ConflictoException(CodigoError.TRASLADO_FINALIZADO,
					"Con la unidad ya asignada solo se pueden corregir la referencia, el contacto y las observaciones.");
		}
	}

	/** El tipo que hay que buscar: el corregido si alguien lo arregló, y si no el que pidió el ciudadano. */
	public TipoUnidad tipoUnidadEfectivo() {
		return tipoUnidadCorregido != null ? tipoUnidadCorregido : tipoUnidadPedido;
	}

	/**
	 * Se corrige cuando la unidad enviada no alcanzó. Sin esto el traslado vuelve a buscar el mismo tipo que ya
	 * falló y el bucle no termina nunca.
	 */
	public void corregirTipoUnidad(TipoUnidad tipo) {
		tipoUnidadCorregido = tipo;
	}

	/** Llegó la hora de salir. */
	public void empezarBusqueda() {
		pasarA(EstadoTraslado.BUSCANDO_UNIDAD);
	}

	/** Ya hay una atención creada y una unidad tomada. */
	public void asignar() {
		pasarA(EstadoTraslado.ASIGNADO);
	}

	/** El paramédico rechazó, o su unidad no correspondía: el pedido sigue vivo y vuelve a la cola. */
	public void devolverABusqueda() {
		pasarA(EstadoTraslado.BUSCANDO_UNIDAD);
	}

	public void completar() {
		pasarA(EstadoTraslado.COMPLETADO);
	}

	/** Fue una unidad pero nadie viajó. El porqué queda en el motivo de la atención. */
	public void marcarNoRealizado() {
		pasarA(EstadoTraslado.NO_REALIZADO);
	}

	/** Se pasó la última salida posible: hay que avisarle a la familia en vez de dejarla esperando. */
	public void marcarNoCubierto() {
		pasarA(EstadoTraslado.NO_CUBIERTO);
	}

	public void cancelar(Usuario quien, Instant ahora) {
		pasarA(EstadoTraslado.CANCELADO);
		canceladoPor = quien;
		fechaHoraCancelacion = ahora;
	}

	public boolean esDe(Long usuarioId) {
		return solicitante.getId().equals(usuarioId);
	}

	private void pasarA(EstadoTraslado nuevo) {
		if (!estado.puedePasarA(nuevo)) {
			throw new ConflictoException(CodigoError.TRANSICION_INVALIDA,
					"El traslado no puede pasar de " + estado + " a " + nuevo + ".");
		}
		estado = nuevo;
	}

}
