package com.uem.ambulancias.flota.service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import com.uem.ambulancias.comun.error.CodigoError;
import com.uem.ambulancias.comun.error.ConflictoException;
import com.uem.ambulancias.comun.error.NoEncontradoException;
import com.uem.ambulancias.flota.domain.Ambulancia;
import com.uem.ambulancias.flota.domain.Asignacion;
import com.uem.ambulancias.flota.domain.TipoUnidad;
import com.uem.ambulancias.flota.repository.AmbulanciaRepository;
import com.uem.ambulancias.flota.repository.AsignacionRepository;
import com.uem.ambulancias.flota.repository.TripulantesEnTurno;
import com.uem.ambulancias.flota.repository.TurnoRepository;
import com.uem.ambulancias.usuarios.domain.RolUsuario;
import com.uem.ambulancias.usuarios.domain.Usuario;
import com.uem.ambulancias.usuarios.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AmbulanciaService {

	private final AmbulanciaRepository ambulancias;
	private final AsignacionRepository asignaciones;
	private final TurnoRepository turnos;
	private final UsuarioRepository usuarios;
	private final ApplicationEventPublisher eventos;

	/** La placa se guarda sin espacios y en mayúsculas, y es única (R5). */
	@Transactional
	public Ambulancia registrar(String placa, TipoUnidad tipoUnidad) {
		String placaNormalizada = normalizarPlaca(placa);
		if (ambulancias.existsByPlaca(placaNormalizada)) {
			throw placaDuplicada(placaNormalizada);
		}
		try {
			return ambulancias.saveAndFlush(Ambulancia.registrar(placaNormalizada, tipoUnidad));
		} catch (DataIntegrityViolationException e) {
			// Otra petición registró la misma placa entre la verificación y el guardado.
			throw placaDuplicada(placaNormalizada);
		}
	}

	public List<Ambulancia> listar() {
		return ambulancias.findAllByOrderByPlacaAsc();
	}

	/** Cuántos tienen turno abierto en cada unidad. Las que no aparecen no tienen a nadie. */
	public Map<Long, Long> tripulantesEnTurno() {
		return turnos.contarAbiertosPorUnidad().stream()
				.collect(Collectors.toMap(TripulantesEnTurno::ambulanciaId, TripulantesEnTurno::cantidad));
	}

	public long tripulantesEnTurno(Long ambulanciaId) {
		return turnos.contarAbiertosPorAmbulancia(ambulanciaId);
	}

	@Transactional
	public Ambulancia marcarFueraDeServicio(Long id) {
		Ambulancia ambulancia = buscarParaActualizar(id);
		ambulancia.marcarFueraDeServicio();
		eventos.publishEvent(new UnidadActualizada(id));
		return ambulancia;
	}

	/**
	 * ME-1 M5: vuelve de una avería. La puede pedir el administrador, o el paramédico de esa misma unidad desde
	 * su app. Se verifica de quién es porque era la única ruta del sistema donde la ambulancia salía de la URL y
	 * no del token: un paramédico podía poner en servicio una unidad ajena que seguía rota de verdad.
	 */
	@Transactional
	public Ambulancia reactivar(Long id, Long usuarioId) {
		exigirQueSeaSuUnidad(id, usuarioId);
		Ambulancia ambulancia = buscarParaActualizar(id);
		ambulancia.reactivar(turnos.contarAbiertosPorAmbulancia(id) > 0);
		eventos.publishEvent(new UnidadActualizada(id));
		return ambulancia;
	}

	/** Corrige la placa o el tipo. La placa sigue las mismas reglas que al registrar, sin contarse a sí misma. */
	@Transactional
	public Ambulancia editar(Long id, String placa, TipoUnidad tipoUnidad) {
		Ambulancia ambulancia = buscarParaActualizar(id);
		String placaNormalizada = normalizarPlaca(placa);
		if (ambulancias.existsByPlacaAndIdNot(placaNormalizada, id)) {
			throw placaDuplicada(placaNormalizada);
		}
		try {
			ambulancia.corregirDatos(placaNormalizada, tipoUnidad);
			ambulancias.flush();
			eventos.publishEvent(new UnidadActualizada(id));
			return ambulancia;
		} catch (DataIntegrityViolationException e) {
			// Otra petición se quedó con la misma placa entre la verificación y el guardado.
			throw placaDuplicada(placaNormalizada);
		}
	}

	/** Deshace la baja lógica. La placa nunca se libera, así que no hay nada que revisar. */
	@Transactional
	public Ambulancia activar(Long id) {
		Ambulancia ambulancia = buscarParaActualizar(id);
		ambulancia.activar();
		eventos.publishEvent(new UnidadActualizada(id));
		return ambulancia;
	}

	private void exigirQueSeaSuUnidad(Long ambulanciaId, Long usuarioId) {
		Usuario usuario = usuarios.findById(usuarioId)
				.orElseThrow(() -> new NoEncontradoException("No existe el usuario " + usuarioId + "."));
		if (usuario.getRol() != RolUsuario.PARAMEDICO) {
			return;
		}
		boolean esSuUnidad = asignaciones.buscarVigentePorParamedico(usuarioId)
				.filter(asignacion -> asignacion.getAmbulancia().getId().equals(ambulanciaId))
				.isPresent();
		if (!esSuUnidad) {
			throw new ConflictoException(CodigoError.AMBULANCIA_AJENA,
					"Solo puedes volver a servicio la ambulancia que tienes asignada.");
		}
	}

	/** Sin espacios en ninguna parte y en mayúsculas, para que "ABC 123" y "ABC123" sean la misma placa. */
	private static String normalizarPlaca(String placa) {
		return placa.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
	}


	/**
	 * Baja lógica. Atendiendo no se puede, y eso lo cuida la propia ambulancia. Con gente de turno adentro tampoco:
	 * quedarían trabajando en una unidad que ya no existe para el sistema.
	 */
	@Transactional
	public Ambulancia desactivar(Long id) {
		Ambulancia ambulancia = buscarParaActualizar(id);
		if (turnos.contarAbiertosPorAmbulancia(id) > 0) {
			throw new ConflictoException(CodigoError.PARAMEDICO_EN_TURNO,
					"La ambulancia " + ambulancia.getPlaca() + " tiene gente de turno. Ciérrales el turno antes de "
							+ "desactivarla.");
		}
		ambulancia.desactivar();
		eventos.publishEvent(new UnidadActualizada(id));
		return ambulancia;
	}

	public List<Asignacion> historialDeAsignaciones(Long id) {
		if (!ambulancias.existsById(id)) {
			throw noEncontrada(id);
		}
		return asignaciones.buscarPorAmbulancia(id);
	}

	private Ambulancia buscarParaActualizar(Long id) {
		return ambulancias.buscarParaActualizar(id).orElseThrow(() -> noEncontrada(id));
	}

	private static NoEncontradoException noEncontrada(Long id) {
		return new NoEncontradoException("No existe la ambulancia " + id + ".");
	}

	private static ConflictoException placaDuplicada(String placa) {
		return new ConflictoException(CodigoError.PLACA_DUPLICADA, "Ya existe una ambulancia con la placa " + placa + ".");
	}

}
