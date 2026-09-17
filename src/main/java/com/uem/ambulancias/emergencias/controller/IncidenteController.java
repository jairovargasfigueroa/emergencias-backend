package com.uem.ambulancias.emergencias.controller;

import com.uem.ambulancias.comun.error.ManejadorErrores;
import com.uem.ambulancias.comun.web.Cabeceras;
import com.uem.ambulancias.comun.web.PaginaResponse;
import com.uem.ambulancias.emergencias.domain.Incidente;
import com.uem.ambulancias.emergencias.dto.AtencionResponse;
import com.uem.ambulancias.emergencias.dto.FiltroEstadoIncidente;
import com.uem.ambulancias.emergencias.dto.IncidenteDetalleResponse;
import com.uem.ambulancias.emergencias.dto.IncidenteResumenResponse;
import com.uem.ambulancias.emergencias.dto.UnidadAcudiendoResponse;
import com.uem.ambulancias.emergencias.exception.IncidenteYaTomadoException;
import com.uem.ambulancias.emergencias.service.ConsultaIncidentesService;
import com.uem.ambulancias.emergencias.service.IncidenteConAlertasYAtenciones;
import com.uem.ambulancias.emergencias.service.IncidenteService;
import com.uem.ambulancias.flota.service.ServicioParamedicoService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * SEC-B. Tomar y sumarse son intenciones distintas: la primera unidad toma y las siguientes se suman conociendo el
 * contexto. La ambulancia sale de la asignación vigente del paramédico. El panel del administrador además consulta los
 * incidentes, solo lectura.
 */
@RestController
@RequestMapping("/incidentes")
@RequiredArgsConstructor
public class IncidenteController {

	private final IncidenteService incidenteService;
	private final ConsultaIncidentesService consultaIncidentesService;
	private final ServicioParamedicoService servicioParamedicoService;

	/** Incidentes del filtro, del más reciente al más antiguo. La página empieza en 0 y el tamaño máximo es 100. */
	@GetMapping
	public PaginaResponse<IncidenteResumenResponse> listar(
			@RequestParam(defaultValue = "TODOS") FiltroEstadoIncidente estado,
			@RequestParam(defaultValue = "0") int pagina,
			@RequestParam(defaultValue = "20") int tamano) {
		return PaginaResponse.de(consultaIncidentesService.listar(estado.estados(), pagina, tamano)
				.map(resumen -> IncidenteResumenResponse.de(resumen.incidente(), resumen.cantidadAlertas(),
						resumen.atenciones())));
	}

	/** El incidente con sus alertas y sus atenciones. 404 si no existe. */
	@GetMapping("/{id}")
	public IncidenteDetalleResponse detalle(@PathVariable("id") Long idIncidente) {
		IncidenteConAlertasYAtenciones detalle = consultaIncidentesService.detalle(idIncidente);
		return IncidenteDetalleResponse.de(detalle.incidente(), detalle.alertas(), detalle.atenciones());
	}

	@PostMapping("/{id}/tomar")
	@ResponseStatus(HttpStatus.CREATED)
	public AtencionResponse tomar(@PathVariable("id") Long idIncidente,
			@RequestHeader(Cabeceras.USUARIO_ID) Long paramedicoId) {
		Long idAmbulancia = servicioParamedicoService.ambulanciaAsignada(paramedicoId);
		return AtencionResponse.de(incidenteService.tomar(idIncidente, idAmbulancia));
	}

	@PostMapping("/{id}/sumarse")
	@ResponseStatus(HttpStatus.CREATED)
	public AtencionResponse sumarse(@PathVariable("id") Long idIncidente,
			@RequestHeader(Cabeceras.USUARIO_ID) Long paramedicoId) {
		Long idAmbulancia = servicioParamedicoService.ambulanciaAsignada(paramedicoId);
		return AtencionResponse.de(incidenteService.sumarse(idIncidente, idAmbulancia));
	}

	/** Nunca se rechaza en silencio: 409 con lo necesario para decidir si sumarse sin otra consulta. */
	@ExceptionHandler(IncidenteYaTomadoException.class)
	ProblemDetail incidenteYaTomado(IncidenteYaTomadoException e) {
		Incidente incidente = e.getIncidente();
		ProblemDetail problema = ManejadorErrores.problema(HttpStatus.CONFLICT, e.getCodigo(), e.getMessage());
		problema.setProperty("incidenteId", incidente.getId());
		problema.setProperty("cantidadAfectados", incidente.getCantidadAfectados());
		problema.setProperty("unidadesAcudiendo", incidenteService.unidadesAcudiendo(incidente.getId()).stream()
				.map(UnidadAcudiendoResponse::de)
				.toList());
		return problema;
	}

}
