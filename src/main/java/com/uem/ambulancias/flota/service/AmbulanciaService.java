package com.uem.ambulancias.flota.service;

import java.util.List;
import java.util.Locale;

import com.uem.ambulancias.comun.error.CodigoError;
import com.uem.ambulancias.comun.error.ConflictoException;
import com.uem.ambulancias.comun.error.NoEncontradoException;
import com.uem.ambulancias.flota.domain.Ambulancia;
import com.uem.ambulancias.flota.domain.Asignacion;
import com.uem.ambulancias.flota.repository.AmbulanciaRepository;
import com.uem.ambulancias.flota.repository.AsignacionRepository;
import com.uem.ambulancias.flota.repository.TurnoRepository;

import lombok.RequiredArgsConstructor;
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

	/** La placa se guarda sin espacios y en mayúsculas, y es única (R5). */
	@Transactional
	public Ambulancia registrar(String placa, String tipoUnidad) {
		String placaNormalizada = placa.trim().toUpperCase(Locale.ROOT);
		if (ambulancias.existsByPlaca(placaNormalizada)) {
			throw placaDuplicada(placaNormalizada);
		}
		try {
			return ambulancias.saveAndFlush(Ambulancia.registrar(placaNormalizada, tipoUnidad.trim()));
		} catch (DataIntegrityViolationException e) {
			// Otra petición registró la misma placa entre la verificación y el guardado.
			throw placaDuplicada(placaNormalizada);
		}
	}

	public List<Ambulancia> listar() {
		return ambulancias.findAllByOrderByPlacaAsc();
	}

	@Transactional
	public Ambulancia marcarFueraDeServicio(Long id) {
		Ambulancia ambulancia = buscarParaActualizar(id);
		ambulancia.marcarFueraDeServicio();
		return ambulancia;
	}

	@Transactional
	public Ambulancia reactivar(Long id) {
		Ambulancia ambulancia = buscarParaActualizar(id);
		ambulancia.reactivar(turnos.contarAbiertosPorAmbulancia(id) > 0);
		return ambulancia;
	}

	@Transactional
	public Ambulancia desactivar(Long id) {
		Ambulancia ambulancia = buscarParaActualizar(id);
		ambulancia.desactivar();
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
