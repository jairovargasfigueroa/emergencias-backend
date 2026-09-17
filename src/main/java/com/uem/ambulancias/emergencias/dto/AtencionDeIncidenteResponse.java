package com.uem.ambulancias.emergencias.dto;

import java.time.Instant;

import com.uem.ambulancias.emergencias.domain.Atencion;
import com.uem.ambulancias.emergencias.domain.EstadoAtencion;
import com.uem.ambulancias.emergencias.domain.MotivoCancelacionAtencion;

/**
 * Atención en el detalle de un incidente: cada hito con su hora y su ubicación ({@code null} si no ocurrió), los datos
 * del paciente y el destino de la entrega.
 */
public record AtencionDeIncidenteResponse(
		Long id,
		Long ambulanciaId,
		String placa,
		EstadoAtencion estado,
		Instant horaToma,
		Instant horaLlegada,
		Instant horaRecogida,
		Instant horaEntrega,
		Instant horaCancelacion,
		MotivoCancelacionAtencion motivoCancelacion,
		UbicacionResponse ubicacionLlegada,
		UbicacionResponse ubicacionRecogida,
		UbicacionResponse ubicacionEntrega,
		String nombrePaciente,
		String documentoPaciente,
		Centro centroSalud,
		String destinoDescripcion) {

	/** Centro de salud del catálogo donde se entregó al paciente. */
	public record Centro(Long id, String nombre) {
	}

	/** {@code atencion} con su ambulancia y su centro de salud. */
	public static AtencionDeIncidenteResponse de(Atencion atencion) {
		return new AtencionDeIncidenteResponse(
				atencion.getId(),
				atencion.getAmbulancia().getId(),
				atencion.getAmbulancia().getPlaca(),
				atencion.getEstado(),
				atencion.getHoraToma(),
				atencion.getHoraLlegada(),
				atencion.getHoraRecogida(),
				atencion.getHoraEntrega(),
				atencion.getHoraCancelacion(),
				atencion.getMotivoCancelacion(),
				UbicacionResponse.de(atencion.getUbicacionLlegada()),
				UbicacionResponse.de(atencion.getUbicacionRecogida()),
				UbicacionResponse.de(atencion.getUbicacionEntrega()),
				atencion.getNombrePaciente(),
				atencion.getDocumentoPaciente(),
				atencion.getCentroSalud() == null ? null
						: new Centro(atencion.getCentroSalud().getId(), atencion.getCentroSalud().getNombre()),
				atencion.getDestinoDescripcion());
	}

}
