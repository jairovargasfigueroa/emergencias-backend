package com.uem.ambulancias.emergencias.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.uem.ambulancias.comun.error.CodigoError;
import com.uem.ambulancias.comun.error.ConflictoException;
import com.uem.ambulancias.comun.error.NoEncontradoException;
import com.uem.ambulancias.comun.geo.Geo;
import com.uem.ambulancias.emergencias.domain.Atencion;
import com.uem.ambulancias.emergencias.domain.CentroSalud;
import com.uem.ambulancias.emergencias.domain.Horario;
import com.uem.ambulancias.emergencias.domain.MotivoCancelacionAtencion;
import com.uem.ambulancias.emergencias.domain.Necesidades;
import com.uem.ambulancias.emergencias.domain.Traslado;
import com.uem.ambulancias.emergencias.dto.RegistrarTrasladoRequest;
import com.uem.ambulancias.emergencias.repository.AtencionRepository;
import com.uem.ambulancias.emergencias.repository.CentroSaludRepository;
import com.uem.ambulancias.emergencias.repository.TrasladoRepository;
import com.uem.ambulancias.flota.domain.EstadoAmbulancia;
import com.uem.ambulancias.flota.domain.TipoUnidad;
import com.uem.ambulancias.flota.repository.AmbulanciaRepository;
import com.uem.ambulancias.flota.service.UnidadActualizada;
import com.uem.ambulancias.usuarios.domain.Usuario;
import com.uem.ambulancias.usuarios.repository.UsuarioRepository;
import com.uem.ambulancias.usuarios.service.CiudadanoService;

import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Point;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * El pedido de traslado, del lado del ciudadano. Pedir no reserva ninguna unidad: el traslado queda agendado y
 * recién a la hora de salir se busca una ambulancia entre las que estén libres en ese momento.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TrasladoService {

	private final TrasladoRepository traslados;
	private final AtencionRepository atenciones;
	private final AmbulanciaRepository ambulancias;
	private final UsuarioRepository usuarios;
	private final CentroSaludRepository centros;
	private final CiudadanoService ciudadanos;
	private final EstimadorDeTiempos estimador;
	private final SelectorDeUnidad selector;
	private final TrasladoProperties config;
	private final ApplicationEventPublisher eventos;

	@Transactional
	public Traslado registrar(Long solicitanteId, RegistrarTrasladoRequest datos) {
		Usuario solicitante = ciudadanos.buscarCiudadanoActivo(solicitanteId);
		Usuario pasajero = resolverPasajero(solicitante, datos.pasajeroId());
		Pedido pedido = resolver(datos);

		return traslados.save(Traslado.registrar(solicitante, pasajero, pedido.necesidades(), pedido.origen(),
				pedido.origenReferencia(), pedido.contactoNombre(), pedido.contactoTelefono(), pedido.centro(),
				pedido.destino(), pedido.destinoDetalle(), pedido.horario(), pedido.tipoUnidad(), Instant.now()));
	}

	/**
	 * Cambiar el pedido entero. Solo mientras nadie haya salido: el horario y el tipo de unidad se vuelven a
	 * calcular con los datos nuevos, porque si cambió la dirección o la hora, los viejos ya no significan nada.
	 */
	@Transactional
	public Traslado reprogramar(Long solicitanteId, Long trasladoId, RegistrarTrasladoRequest datos) {
		Traslado traslado = buscarPropioParaActualizar(solicitanteId, trasladoId);
		Pedido pedido = resolver(datos);
		traslado.reprogramar(pedido.necesidades(), pedido.origen(), pedido.origenReferencia(),
				pedido.contactoNombre(), pedido.contactoTelefono(), pedido.centro(), pedido.destino(),
				pedido.destinoDetalle(), pedido.horario(), pedido.tipoUnidad());
		return traslados.save(traslado);
	}

	/**
	 * Corregir la referencia, el contacto y las observaciones. Se puede hasta con la unidad en camino, así que la
	 * respuesta lleva en qué va la unidad: la app reemplaza con ella lo que tenía.
	 */
	@Transactional
	public TrasladoConAtencion actualizarDetalles(Long solicitanteId, Long trasladoId, String origenReferencia,
			String contactoNombre, String contactoTelefono, String observaciones) {
		Traslado traslado = buscarPropioParaActualizar(solicitanteId, trasladoId);
		String nombre = vacioComoNulo(contactoNombre);
		String telefono = vacioComoNulo(contactoTelefono);
		exigirContactoCompleto(nombre, telefono);
		traslado.actualizarDetalles(vacioComoNulo(origenReferencia), nombre, telefono,
				vacioComoNulo(observaciones));
		// Con la unidad en camino, la tripulación tiene que ver el contacto y la referencia nuevos.
		atenciones.buscarActivaPorTraslado(trasladoId)
				.ifPresent(atencion -> eventos.publishEvent(new UnidadActualizada(atencion.getAmbulancia().getId())));
		return conSuAtencion(List.of(traslados.save(traslado))).getFirst();
	}

	/** Lo que hay que calcular igual al pedir que al reprogramar. */
	private Pedido resolver(RegistrarTrasladoRequest datos) {
		Necesidades necesidades = new Necesidades(datos.movilidad(), datos.oxigeno(), datos.equipo(),
				datos.aislamiento(), datos.pesoAproximado(), datos.acompanantes(),
				vacioComoNulo(datos.observaciones()));
		TipoUnidad tipoUnidad = selector.resolver(necesidades, datos.tipoUnidad());

		Point origen = Geo.punto(datos.origenLatitud(), datos.origenLongitud());
		CentroSalud centro = resolverCentro(datos.centroSaludDestinoId());
		Point destino = resolverDestino(centro, datos);

		Instant ahora = Instant.now();
		Horario horario = datos.horaCita() == null
				? estimador.paraAhora(ahora)
				: estimador.paraCita(origen, destino, datos.horaCita());
		exigirQueLlegue(horario, ahora);

		String contactoNombre = vacioComoNulo(datos.contactoNombre());
		String contactoTelefono = vacioComoNulo(datos.contactoTelefono());
		exigirContactoCompleto(contactoNombre, contactoTelefono);

		return new Pedido(necesidades, tipoUnidad, origen, vacioComoNulo(datos.origenReferencia()), contactoNombre,
				contactoTelefono, centro, destino, vacioComoNulo(datos.destinoDetalle()), horario);
	}

	/**
	 * Con la fila bloqueada: todo lo que el ciudadano hace sobre su traslado lo cambia, y el barrido puede estar
	 * asignándolo en ese mismo momento. Sin el bloqueo, el que guarda último borra lo que hizo el otro.
	 */
	private Traslado buscarPropioParaActualizar(Long solicitanteId, Long trasladoId) {
		Traslado traslado = traslados.buscarParaActualizar(trasladoId)
				.orElseThrow(() -> new NoEncontradoException("No existe el traslado " + trasladoId + "."));
		if (!traslado.esDe(solicitanteId)) {
			throw new ConflictoException(CodigoError.TRASLADO_AJENO, "El traslado no es de este ciudadano.");
		}
		return traslado;
	}

	private record Pedido(Necesidades necesidades, TipoUnidad tipoUnidad, Point origen, String origenReferencia,
			String contactoNombre, String contactoTelefono, CentroSalud centro, Point destino,
			String destinoDetalle, Horario horario) {
	}

	/**
	 * Lo que el ciudadano ve en su pestaña: los próximos y el historial, lo más reciente primero. Cada uno con su
	 * unidad, porque en qué va es lo que decide si todavía se puede cancelar.
	 */
	public List<TrasladoConAtencion> mios(Long solicitanteId) {
		return conSuAtencion(traslados.findBySolicitanteIdOrderByFechaHoraCreacionDesc(solicitanteId));
	}

	/** La tabla del panel. El día se calcula en la zona de la empresa: el servidor puede estar en UTC. */
	public List<TrasladoConAtencion> delDia(LocalDate dia) {
		ZoneId zona = ZoneId.of(config.zona());
		LocalDate elDia = dia != null ? dia : LocalDate.now(zona);
		return conSuAtencion(traslados.buscarEntre(elDia.atStartOfDay(zona).toInstant(),
				elDia.plusDays(1).atStartOfDay(zona).toInstant()));
	}

	/** El detalle de un traslado para el panel, con la unidad que lo está haciendo si ya tiene una. */
	public TrasladoConAtencion detalle(Long trasladoId) {
		Traslado traslado = traslados.findById(trasladoId)
				.orElseThrow(() -> new NoEncontradoException("No existe el traslado " + trasladoId + "."));
		return conSuAtencion(List.of(traslado)).getFirst();
	}

	/**
	 * La bandeja de problemas: los que tienen una unidad atrasada, los que siguen esperando unidad, y los que se
	 * vencieron sin ella hasta que alguien le avise a la familia.
	 */
	public List<TrasladoConAtencion> problemas() {
		return conSuAtencion(traslados.buscarProblemas(Instant.now()));
	}

	/** El administrador ya le avisó a la familia que no hubo unidad: sale de la bandeja. */
	@Transactional
	public TrasladoConAtencion marcarFamiliaAvisada(Long trasladoId) {
		Traslado traslado = traslados.buscarParaActualizar(trasladoId)
				.orElseThrow(() -> new NoEncontradoException("No existe el traslado " + trasladoId + "."));
		traslado.marcarFamiliaAvisada(Instant.now());
		return conSuAtencion(List.of(traslados.save(traslado))).getFirst();
	}

	/**
	 * Una sola consulta para las atenciones de toda la lista, en vez de una por fila. Un traslado devuelto tiene
	 * varias: cuenta la última, y solo si el traslado sigue en sus manos o terminó con ella. Uno que volvió a
	 * buscar unidad no tiene a nadie, aunque antes haya tenido.
	 */
	private List<TrasladoConAtencion> conSuAtencion(List<Traslado> lista) {
		if (lista.isEmpty()) {
			return List.of();
		}
		Map<Long, Atencion> ultimaPorTraslado = atenciones
				.buscarPorTraslados(lista.stream().map(Traslado::getId).toList()).stream()
				.collect(Collectors.toMap(atencion -> atencion.getTraslado().getId(), atencion -> atencion,
						(masNueva, masVieja) -> masNueva));
		return lista.stream()
				.map(t -> new TrasladoConAtencion(t,
						t.getEstado().isConUnidad() ? ultimaPorTraslado.get(t.getId()) : null))
				.toList();
	}

	/**
	 * Retirar el pedido. Mientras no haya unidad asignada no hay nada más que deshacer; con una unidad ya en
	 * camino además hay que cancelar su atención y liberarla.
	 *
	 * <p>Como en una central de verdad, se puede avisar que ya no hace falta mientras la unidad viene. Cuando ya
	 * está en la puerta, eso se habla con la tripulación, que cierra el viaje con el motivo que corresponda: si no,
	 * el paciente podría estar a bordo y la ambulancia quedaría libre en el sistema.
	 */
	@Transactional
	public Traslado cancelar(Long solicitanteId, Long trasladoId) {
		Traslado traslado = buscarPropioParaActualizar(solicitanteId, trasladoId);
		if (!traslado.getEstado().isVigente()) {
			throw new ConflictoException(CodigoError.TRASLADO_FINALIZADO,
					"El traslado ya terminó y no se puede cancelar.");
		}
		Atencion enCurso = atenciones.buscarActivaPorTraslado(trasladoId).orElse(null);
		if (enCurso != null && enCurso.getHoraLlegada() != null) {
			throw new ConflictoException(CodigoError.UNIDAD_EN_EL_LUGAR,
					"La unidad ya llegó. Si no van a viajar, díselo a la tripulación.");
		}
		// Con la unidad en camino, cancelar también la deja libre: si no, quedaría tomada por un viaje que ya no existe.
		if (enCurso != null) {
			Long ambulanciaId = enCurso.getAmbulancia().getId();
			enCurso.cancelar(MotivoCancelacionAtencion.CANCELADA_POR_SOLICITANTE);
			ambulancias.actualizarEstado(ambulanciaId, EstadoAmbulancia.DISPONIBLE);
			eventos.publishEvent(new UnidadLiberada(ambulanciaId));
			eventos.publishEvent(new UnidadActualizada(ambulanciaId));
			eventos.publishEvent(new TrasladoRetirado(trasladoId, ambulanciaId,
					MotivoCancelacionAtencion.CANCELADA_POR_SOLICITANTE));
		}
		traslado.cancelar(ciudadanos.buscarCiudadanoActivo(solicitanteId), Instant.now());
		return traslados.save(traslado);
	}

	/**
	 * Nulo significa que viaja quien pide. Si no, tiene que ser alguien que él mismo registró y que sigue en su
	 * perfil: a quien quitó no se le piden viajes nuevos, aunque se esté repitiendo uno viejo.
	 */
	private Usuario resolverPasajero(Usuario solicitante, Long pasajeroId) {
		if (pasajeroId == null || pasajeroId.equals(solicitante.getId())) {
			return solicitante;
		}
		if (!usuarios.existsByIdAndRegistradoPorId(pasajeroId, solicitante.getId())) {
			throw new ConflictoException(CodigoError.PASAJERO_AJENO,
					"Solo se puede pedir un traslado para una persona propia.");
		}
		Usuario pasajero = usuarios.findById(pasajeroId)
				.orElseThrow(() -> new NoEncontradoException("No existe la persona " + pasajeroId + "."));
		if (!pasajero.isActivo()) {
			throw new ConflictoException(CodigoError.PASAJERO_AJENO,
					"Esa persona ya no está en tu perfil. Elige quién viaja.");
		}
		return pasajero;
	}

	private CentroSalud resolverCentro(Long centroId) {
		if (centroId == null) {
			return null;
		}
		return centros.findByIdAndActivoTrue(centroId)
				.orElseThrow(() -> new NoEncontradoException("No existe el centro de salud " + centroId + "."));
	}

	/** El punto del destino siempre queda guardado: si es un centro se copia el suyo, si no lo marcan en el mapa. */
	private Point resolverDestino(CentroSalud centro, RegistrarTrasladoRequest datos) {
		if (centro != null) {
			return centro.getUbicacion();
		}
		if (datos.destinoLatitud() == null || datos.destinoLongitud() == null) {
			throw new ConflictoException(CodigoError.DESTINO_REQUERIDO,
					"Hay que elegir un centro de salud o marcar el destino en el mapa.");
		}
		return Geo.punto(datos.destinoLatitud(), datos.destinoLongitud());
	}

	private void exigirQueLlegue(Horario horario, Instant ahora) {
		if (horario.limiteSalida().isBefore(ahora)) {
			throw new ConflictoException(CodigoError.HORA_INALCANZABLE,
					"No se llega a esa hora. Elige una más tarde.");
		}
	}

	/** Nombre sin teléfono no sirve para nada: o están los dos o no está ninguno, y entonces recibe el solicitante. */
	private void exigirContactoCompleto(String nombre, String telefono) {
		if ((nombre == null) != (telefono == null)) {
			throw new ConflictoException(CodigoError.VALIDACION,
					"El contacto necesita nombre y teléfono, o ninguno de los dos.");
		}
	}

	private String vacioComoNulo(String texto) {
		return texto == null || texto.isBlank() ? null : texto.trim();
	}

}
