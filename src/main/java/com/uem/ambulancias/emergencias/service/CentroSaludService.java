package com.uem.ambulancias.emergencias.service;

import java.util.List;

import com.uem.ambulancias.comun.error.NoEncontradoException;
import com.uem.ambulancias.comun.geo.Geo;
import com.uem.ambulancias.emergencias.domain.CentroSalud;
import com.uem.ambulancias.emergencias.dto.GuardarCentroSaludRequest;
import com.uem.ambulancias.emergencias.repository.CentroSaludRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Catálogo de centros de salud: lo eligen el paramédico (dónde entrega al paciente) y el ciudadano (destino de un
 * traslado), y lo mantiene la central.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CentroSaludService {

	private final CentroSaludRepository centrosSalud;

	public List<CentroSalud> listarActivos() {
		return centrosSalud.findByActivoTrueOrderByNombreAsc();
	}


	/** Todos, también los dados de baja, para que la central los administre. Primero los activos. */
	public List<CentroSalud> listarTodos() {
		return centrosSalud.findAllByOrderByActivoDescNombreAsc();
	}

	@Transactional
	public CentroSalud registrar(GuardarCentroSaludRequest datos) {
		return centrosSalud.save(CentroSalud.registrar(datos.nombre().trim(),
				Geo.punto(datos.latitud(), datos.longitud()), sinVacios(datos.direccion())));
	}

	@Transactional
	public CentroSalud corregir(Long id, GuardarCentroSaludRequest datos) {
		CentroSalud centro = buscar(id);
		centro.corregir(datos.nombre().trim(), Geo.punto(datos.latitud(), datos.longitud()),
				sinVacios(datos.direccion()));
		return centro;
	}

	@Transactional
	public CentroSalud desactivar(Long id) {
		CentroSalud centro = buscar(id);
		centro.desactivar();
		return centro;
	}

	@Transactional
	public CentroSalud activar(Long id) {
		CentroSalud centro = buscar(id);
		centro.activar();
		return centro;
	}

	private CentroSalud buscar(Long id) {
		return centrosSalud.findById(id)
				.orElseThrow(() -> new NoEncontradoException("No existe el centro de salud " + id + "."));
	}

	private static String sinVacios(String texto) {
		return texto == null || texto.isBlank() ? null : texto.trim();
	}

}
