package com.uem.ambulancias.emergencias.dto;

import java.time.Instant;

import com.uem.ambulancias.emergencias.domain.Atencion;
import com.uem.ambulancias.emergencias.domain.EstadoAtencion;
import com.uem.ambulancias.emergencias.domain.MotivoCancelacionAtencion;
import com.uem.ambulancias.emergencias.domain.MotivoSinTraslado;
import com.uem.ambulancias.usuarios.domain.Usuario;

/**
 * Atención en el detalle de un incidente: el paramédico que responde por ella ({@code null} si no tiene), cada hito
 * con su hora y su ubicación ({@code null} si no ocurrió), los datos del paciente y el destino de la entrega.
 */
public record AtencionDeIncidenteResponse(
		Long id,
		Long ambulanciaId,
		String placa,
		Responsable paramedicoResponsable,
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

	/** Paramédico que responde por la atención. */
	public record Responsable(Long id, String nombreCompleto, String telefono) {
	}

	/** Centro de salud del catálogo donde se entregó al paciente. */
	public record Centro(Long id, String nombre) {
	}

	/** {@code atencion} con su ambulancia, su paramédico responsable y su centro de salud. */
	public static AtencionDeIncidenteResponse de(Atencion atencion) {
		Usuario paramedico = atencion.getParamedicoResponsable();
		return new AtencionDeIncidenteResponse(
				atencion.getId(),
				atencion.getAmbulancia().getId(),
				atencion.getAmbulancia().getPlaca(),
				paramedico == null ? null
						: new Responsable(paramedico.getId(), paramedico.getNombreCompleto(), paramedico.getTelefono()),
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
