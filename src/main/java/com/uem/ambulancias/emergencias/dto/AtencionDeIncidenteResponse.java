package com.uem.ambulancias.emergencias.dto;

import java.time.Instant;

import com.uem.ambulancias.emergencias.domain.Atencion;
import com.uem.ambulancias.emergencias.domain.EstadoAtencion;
import com.uem.ambulancias.emergencias.domain.MotivoCancelacionAtencion;
import com.uem.ambulancias.emergencias.domain.MotivoSinTraslado;

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
		Instant horaLlegadaHospital,
		Instant horaEntrega,
		Instant horaSinTraslado,
		MotivoSinTraslado motivoSinTraslado,
		Instant horaLiberacion,
		Instant horaCancelacion,
		MotivoCancelacionAtencion motivoCancelacion,
		UbicacionResponse ubicacionLlegada,
		UbicacionResponse ubicacionRecogida,
		UbicacionResponse ubicacionLlegadaHospital,
		UbicacionResponse ubicacionEntrega,
		UbicacionResponse ubicacionSinTraslado,
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
				atencion.getHoraLlegadaHospital(),
				atencion.getHoraEntrega(),
				atencion.getHoraSinTraslado(),
				atencion.getMotivoSinTraslado(),
				atencion.getHoraLiberacion(),
				atencion.getHoraCancelacion(),
				atencion.getMotivoCancelacion(),
				UbicacionResponse.de(atencion.getUbicacionLlegada()),
				UbicacionResponse.de(atencion.getUbicacionRecogida()),
				UbicacionResponse.de(atencion.getUbicacionLlegadaHospital()),
				UbicacionResponse.de(atencion.getUbicacionEntrega()),
				UbicacionResponse.de(atencion.getUbicacionSinTraslado()),
				atencion.getNombrePaciente(),
				atencion.getDocumentoPaciente(),
				atencion.getCentroSalud() == null ? null
						: new Centro(atencion.getCentroSalud().getId(), atencion.getCentroSalud().getNombre()),
				atencion.getDestinoDescripcion());
	}

}
