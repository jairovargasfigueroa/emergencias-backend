package com.uem.ambulancias.emergencias.service;

import java.time.Instant;
import java.util.List;

import com.uem.ambulancias.comun.error.CodigoError;
import com.uem.ambulancias.comun.error.ConflictoException;
import com.uem.ambulancias.comun.error.NoEncontradoException;
import com.uem.ambulancias.comun.geo.Geo;
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
		Traslado traslado = buscarPropio(solicitanteId, trasladoId);
		Pedido pedido = resolver(datos);
		traslado.reprogramar(pedido.necesidades(), pedido.origen(), pedido.origenReferencia(),
				pedido.contactoNombre(), pedido.contactoTelefono(), pedido.centro(), pedido.destino(),
				pedido.destinoDetalle(), pedido.horario(), pedido.tipoUnidad());
		return traslados.save(traslado);
	}

	/** Corregir la referencia, el contacto y las observaciones. Se puede hasta con la unidad en camino. */
	@Transactional
	public Traslado actualizarDetalles(Long solicitanteId, Long trasladoId, String origenReferencia,
			String contactoNombre, String contactoTelefono, String observaciones) {
		Traslado traslado = buscarPropio(solicitanteId, trasladoId);
		String nombre = vacioComoNulo(contactoNombre);
		String telefono = vacioComoNulo(contactoTelefono);
		exigirContactoCompleto(nombre, telefono);
		traslado.actualizarDetalles(vacioComoNulo(origenReferencia), nombre, telefono,
				vacioComoNulo(observaciones));
		return traslados.save(traslado);
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

	private Traslado buscarPropio(Long solicitanteId, Long trasladoId) {
		Traslado traslado = traslados.findById(trasladoId)
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

	/** Lo que el ciudadano ve en su pestaña: los próximos y el historial, lo más reciente primero. */
	public List<Traslado> mios(Long solicitanteId) {
		return traslados.findBySolicitanteIdOrderByFechaHoraCreacionDesc(solicitanteId);
	}

	/**
	 * Retirar el pedido. Mientras no haya unidad asignada no hay nada más que deshacer; con una unidad ya en
	 * camino además hay que cancelar su atención y liberarla, y eso vive junto con la asignación.
	 */
	@Transactional
	public Traslado cancelar(Long solicitanteId, Long trasladoId) {
		Traslado traslado = buscarPropio(solicitanteId, trasladoId);
		if (!traslado.getEstado().isVigente()) {
			throw new ConflictoException(CodigoError.TRASLADO_FINALIZADO,
					"El traslado ya terminó y no se puede cancelar.");
		}
		// Con la unidad ya en camino, cancelar también le devuelve la libertad: si no, queda tomada al pedo.
		atenciones.buscarActivaPorTraslado(trasladoId).ifPresent(atencion -> {
			atencion.cancelar(MotivoCancelacionAtencion.CANCELADA_POR_SOLICITANTE);
			ambulancias.actualizarEstado(atencion.getAmbulancia().getId(), EstadoAmbulancia.DISPONIBLE);
			eventos.publishEvent(new UnidadLiberada(atencion.getAmbulancia().getId()));
		});
		traslado.cancelar(ciudadanos.buscarCiudadanoActivo(solicitanteId), Instant.now());
		return traslados.save(traslado);
	}

	/** Nulo significa que viaja quien pide. Si no, tiene que ser alguien que él mismo registró. */
	private Usuario resolverPasajero(Usuario solicitante, Long pasajeroId) {
		if (pasajeroId == null || pasajeroId.equals(solicitante.getId())) {
			return solicitante;
		}
		if (!usuarios.existsByIdAndRegistradoPorId(pasajeroId, solicitante.getId())) {
			throw new ConflictoException(CodigoError.PASAJERO_AJENO,
					"Solo se puede pedir un traslado para una persona propia.");
		}
		return usuarios.findById(pasajeroId)
				.orElseThrow(() -> new NoEncontradoException("No existe la persona " + pasajeroId + "."));
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
					"No se llega a esa hora. Elegí una más tarde.");
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
