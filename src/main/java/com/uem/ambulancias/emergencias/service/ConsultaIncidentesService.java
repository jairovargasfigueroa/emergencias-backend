package com.uem.ambulancias.emergencias.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.uem.ambulancias.comun.error.NoEncontradoException;
import com.uem.ambulancias.emergencias.domain.Atencion;
import com.uem.ambulancias.emergencias.domain.EstadoIncidente;
import com.uem.ambulancias.emergencias.domain.Incidente;
import com.uem.ambulancias.emergencias.repository.AlertaRepository;
import com.uem.ambulancias.emergencias.repository.AlertasPorIncidente;
import com.uem.ambulancias.emergencias.repository.AtencionRepository;
import com.uem.ambulancias.emergencias.repository.IncidenteRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consulta de incidentes del panel del administrador. Es solo lectura: los incidentes los crea el sistema y cambian
 * solo por ME-1.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConsultaIncidentesService {

	/** Tamaño máximo de una página: uno mayor se recorta. */
	public static final int TAMANO_MAXIMO = 100;

	/** Del más reciente al más antiguo. El id desempata, para que las páginas no repitan ni salteen incidentes. */
	private static final Sort RECIENTES_PRIMERO = Sort.by(Sort.Order.desc("fechaHoraCreacion"), Sort.Order.desc("id"));

	private final IncidenteRepository incidentes;
	private final AlertaRepository alertas;
	private final AtencionRepository atenciones;

	/**
	 * Incidentes en esos estados, del más reciente al más antiguo. Una página negativa se toma como la primera y el
	 * tamaño queda entre 1 y {@link #TAMANO_MAXIMO}. Los conteos de alertas y las atenciones de toda la página salen de
	 * dos consultas agrupadas, no de una por incidente.
	 */
	public Page<IncidenteConAtenciones> listar(Collection<EstadoIncidente> estados, int pagina, int tamano) {
		PageRequest pedido = PageRequest.of(Math.max(pagina, 0), Math.clamp(tamano, 1, TAMANO_MAXIMO),
				RECIENTES_PRIMERO);
		Page<Incidente> encontrados = incidentes.findByEstadoIn(estados, pedido);
		if (!encontrados.hasContent()) {
			return encontrados.map(incidente -> new IncidenteConAtenciones(incidente, 0, List.of()));
		}

		List<Long> ids = encontrados.getContent().stream().map(Incidente::getId).toList();
		Map<Long, Long> alertasPorIncidente = alertas.contarPorIncidentes(ids).stream()
				.collect(Collectors.toMap(AlertasPorIncidente::incidenteId, AlertasPorIncidente::cantidad));
		Map<Long, List<Atencion>> atencionesPorIncidente = atenciones.buscarPorIncidentes(ids).stream()
				.collect(Collectors.groupingBy(atencion -> atencion.getIncidente().getId()));

		return encontrados.map(incidente -> new IncidenteConAtenciones(
				incidente,
				alertasPorIncidente.getOrDefault(incidente.getId(), 0L),
				atencionesPorIncidente.getOrDefault(incidente.getId(), List.of())));
	}

	/** El incidente con todas sus alertas y atenciones. */
	public IncidenteConAlertasYAtenciones detalle(Long incidenteId) {
		Incidente incidente = incidentes.findById(incidenteId)
				.orElseThrow(() -> new NoEncontradoException("No existe el incidente " + incidenteId + "."));
		return new IncidenteConAlertasYAtenciones(incidente, alertas.buscarPorIncidente(incidenteId),
				atenciones.buscarPorIncidentes(List.of(incidenteId)));
	}

}
