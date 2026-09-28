package com.uem.ambulancias.emergencias.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.uem.ambulancias.comun.error.CodigoError;
import com.uem.ambulancias.comun.error.ConflictoException;
import com.uem.ambulancias.comun.error.NoEncontradoException;
import com.uem.ambulancias.emergencias.domain.Atencion;
import com.uem.ambulancias.emergencias.domain.EstadoAtencion;
import com.uem.ambulancias.emergencias.domain.EstadoTraslado;
import com.uem.ambulancias.emergencias.domain.MotivoCancelacionAtencion;
import com.uem.ambulancias.emergencias.domain.Traslado;
import com.uem.ambulancias.emergencias.repository.AtencionRepository;
import com.uem.ambulancias.emergencias.repository.TrasladoRepository;
import com.uem.ambulancias.flota.domain.Ambulancia;
import com.uem.ambulancias.flota.domain.EstadoAmbulancia;
import com.uem.ambulancias.flota.domain.TipoUnidad;
import com.uem.ambulancias.flota.domain.Turno;
import com.uem.ambulancias.flota.repository.AmbulanciaRepository;
import com.uem.ambulancias.flota.repository.TurnoRepository;
import com.uem.ambulancias.usuarios.domain.RolUsuario;
import com.uem.ambulancias.usuarios.domain.Usuario;
import com.uem.ambulancias.usuarios.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * El motor de la asignación. Un traslado no reserva unidad al pedirse: recién a la hora de salir se busca una
 * entre las que están libres en ese momento, y si no hay, se vuelve a intentar hasta la última salida posible.
 *
 * <p>Cada operación es su propia transacción, así un traslado que falla no arrastra a los demás del barrido.
 */
@Service
@RequiredArgsConstructor
public class AsignadorDeTraslados {

	private final TrasladoRepository traslados;
	private final AtencionRepository atenciones;
	private final AmbulanciaRepository ambulancias;
	private final TurnoRepository turnos;
	private final UsuarioRepository usuarios;
	private final TrasladoProperties config;
	private final ApplicationEventPublisher eventos;

	/** Los programados a los que ya les llegó la hora de salir pasan a buscar unidad. */
	@Transactional
	public int abrirBusquedas(Instant ahora) {
		List<Traslado> porSalir = traslados.buscarPorSalir(ahora);
		porSalir.forEach(Traslado::empezarBusqueda);
		traslados.saveAll(porSalir);
		return porSalir.size();
	}

	/**
	 * A los que se les pasó la última salida posible ya no los alcanza ninguna unidad. Se cierran para que la
	 * familia se entere a tiempo, en vez de quedarse esperando algo que no va a llegar.
	 */
	@Transactional
	public List<Traslado> vencerLosQueNoLlegan(Instant ahora) {
		List<Traslado> vencidos = traslados.buscarVencidos(ahora);
		vencidos.forEach(Traslado::marcarNoCubierto);
		return traslados.saveAll(vencidos);
	}

	/**
	 * Intenta darle unidad a un traslado. Vacío significa que en este instante no hay ninguna que sirva, no que
	 * el traslado se haya caído: se reintenta mientras todavía se llegue a tiempo.
	 *
	 * <p>El traslado se bloquea antes de mirar su estado: el administrador puede estar asignándolo a mano en ese
	 * mismo momento, y sin el bloqueo los dos lo veían esperando y salían dos unidades para el mismo viaje.
	 */
	@Transactional
	public Optional<Atencion> intentarAsignar(Long trasladoId) {
		Traslado traslado = traslados.buscarParaActualizar(trasladoId).orElse(null);
		if (traslado == null || traslado.getEstado() != EstadoTraslado.BUSCANDO_UNIDAD) {
			return Optional.empty();
		}
		List<String> tiposQueSirven = tiposQueCubren(traslado.tipoUnidadEfectivo());
		// Las que ya lo tuvieron y lo dejaron no entran: a mano, el administrador sí puede elegirlas.
		List<Long> yaLoTuvieron = atenciones.buscarAmbulanciasQueLoTuvieron(trasladoId);
		Instant posicionDesde = Instant.now().minus(config.posicionVigente());
		for (Long candidataId : ambulancias.buscarIdsDisponiblesParaTraslado(traslado.getOrigen().getY(),
				traslado.getOrigen().getX(), tiposQueSirven, posicionDesde)) {
			if (yaLoTuvieron.contains(candidataId)) {
				continue;
			}
			Optional<Atencion> asignada = tomar(traslado, candidataId, null);
			if (asignada.isPresent()) {
				return asignada;
			}
		}
		return Optional.empty();
	}

	/**
	 * Asignación a mano desde el panel: el sistema no encontró nada, o el administrador sabe algo que el sistema
	 * no. Se verifica igual que la unidad alcance, para no mandar a alguien a un viaje que no va a poder hacer.
	 */
	@Transactional
	public Atencion asignarA(Long trasladoId, Long ambulanciaId, Long administradorId) {
		Usuario administrador = usuarios.findByIdAndRol(administradorId, RolUsuario.ADMIN)
				.orElseThrow(() -> new NoEncontradoException("No existe el administrador " + administradorId + "."));
		Traslado traslado = traslados.buscarParaActualizar(trasladoId)
				.orElseThrow(() -> new NoEncontradoException("No existe el traslado " + trasladoId + "."));
		if (traslado.getEstado() != EstadoTraslado.BUSCANDO_UNIDAD) {
			throw new ConflictoException(CodigoError.TRANSICION_INVALIDA,
					"El traslado ya no está esperando unidad: puede que el sistema se la haya asignado recién.");
		}
		// Bloqueada desde la primera lectura: si se leyera antes sin bloqueo, la que se bloquea después sería esa
		// misma copia, y no se vería que en el medio se fue a una emergencia.
		Ambulancia elegida = ambulancias.buscarParaActualizar(ambulanciaId)
				.orElseThrow(() -> new NoEncontradoException("No existe la ambulancia " + ambulanciaId + "."));
		if (!elegida.getTipoUnidad().cubreA(traslado.tipoUnidadEfectivo())) {
			throw new ConflictoException(CodigoError.UNIDAD_INSUFICIENTE,
					"La unidad " + elegida.getPlaca() + " no alcanza para este traslado.");
		}
		return tomar(traslado, ambulanciaId, administrador)
				.orElseThrow(() -> new ConflictoException(CodigoError.AMBULANCIA_NO_DISPONIBLE,
						"Esa unidad no está disponible o no tiene a nadie en turno."));
	}

	/**
	 * El administrador le saca el traslado a la unidad que lo tiene y lo devuelve a la búsqueda, primero en la
	 * fila. Es lo que hace un despachador cuando ve que una unidad no llega: la llama, y si hace falta manda otra.
	 * Solo mientras la unidad viene en camino: en la puerta o con el paciente a bordo, sacársela no arregla nada.
	 */
	@Transactional
	public Traslado devolverABusqueda(Long trasladoId) {
		Traslado traslado = traslados.buscarParaActualizar(trasladoId)
				.orElseThrow(() -> new NoEncontradoException("No existe el traslado " + trasladoId + "."));
		Atencion enCurso = traslado.getEstado() == EstadoTraslado.ASIGNADO
				? atenciones.buscarActivaPorTraslado(trasladoId).orElse(null)
				: null;
		if (enCurso == null) {
			throw new ConflictoException(CodigoError.TRANSICION_INVALIDA,
					"El traslado no tiene una unidad a la que sacárselo.");
		}
		if (enCurso.getEstado() != EstadoAtencion.EN_CAMINO) {
			throw new ConflictoException(CodigoError.TRANSICION_INVALIDA,
					"La unidad ya llegó: desde ahí el traslado lo resuelve la tripulación.");
		}
		Long ambulanciaId = enCurso.getAmbulancia().getId();
		enCurso.cancelar(MotivoCancelacionAtencion.REASIGNADA);
		ambulancias.actualizarEstado(ambulanciaId, EstadoAmbulancia.DISPONIBLE);
		traslado.devolverABusqueda(Instant.now(), config.busquedaTrasDevolucion(), config.acercamiento());
		eventos.publishEvent(new TrasladoRetirado(trasladoId, ambulanciaId, MotivoCancelacionAtencion.REASIGNADA));
		eventos.publishEvent(new UnidadLiberada(ambulanciaId));
		return traslados.save(traslado);
	}

	/**
	 * Se bloquea la fila antes de tomarla: entre la consulta por cercanía y este momento, la unidad pudo haberse
	 * ido a una emergencia. Si ya no está disponible, se sigue con la siguiente candidata.
	 */
	private Optional<Atencion> tomar(Traslado traslado, Long ambulanciaId, Usuario asignadoPor) {
		Ambulancia ambulancia = ambulancias.buscarParaActualizar(ambulanciaId).orElse(null);
		if (ambulancia == null || !ambulancia.puedeAtender()) {
			return Optional.empty();
		}
		Usuario responsable = responsableDe(ambulanciaId);
		if (responsable == null) {
			return Optional.empty();
		}
		Atencion atencion = atenciones
				.save(Atencion.iniciarTraslado(traslado, ambulancia, responsable, asignadoPor, Instant.now()));
		ambulancia.cambiarEstado(EstadoAmbulancia.EN_ATENCION);
		traslado.asignar();
		traslados.save(traslado);
		// La tripulación está manejando, no mirando la app: si no se le avisa, se entera recién cuando la abre.
		eventos.publishEvent(new TrasladoAsignado(traslado.getId(), ambulanciaId));
		return Optional.of(atencion);
	}

	/** Quién responde por la atención: de los que estén en turno en esa unidad, el que entró primero. */
	private Usuario responsableDe(Long ambulanciaId) {
		return turnos.buscarAbiertosPorAmbulancias(List.of(ambulanciaId)).stream()
				.findFirst()
				.map(Turno::getParamedico)
				.orElse(null);
	}

	/** Los tipos de la flota que alcanzan para lo que el traslado necesita. Una mejor sirve; una menor no. */
	private List<String> tiposQueCubren(TipoUnidad requerido) {
		return TipoUnidad.ESCALERA.stream().filter(tipo -> tipo.cubreA(requerido)).map(Enum::name).toList();
	}

}
