package com.uem.ambulancias.emergencias.service;

import java.util.List;

import com.uem.ambulancias.emergencias.domain.CentroSalud;
import com.uem.ambulancias.emergencias.repository.CentroSaludRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Catálogo de centros de salud para elegir el destino de la entrega. Su carga no forma parte de este sprint.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CentroSaludService {

	private final CentroSaludRepository centrosSalud;

	public List<CentroSalud> listarActivos() {
		return centrosSalud.findByActivoTrueOrderByNombreAsc();
	}

}
