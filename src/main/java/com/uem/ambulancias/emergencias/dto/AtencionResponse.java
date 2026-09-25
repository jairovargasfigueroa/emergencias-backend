package com.uem.ambulancias.emergencias.dto;

import java.time.Instant;

import com.uem.ambulancias.emergencias.domain.Atencion;
import com.uem.ambulancias.emergencias.domain.EstadoAtencion;
import com.uem.ambulancias.emergencias.domain.MotivoCancelacionAtencion;
import com.uem.ambulancias.emergencias.domain.MotivoSinTraslado;

/**
 * La atención como la ve la app del paramédico. Cuelga de un incidente o de un traslado, nunca de los dos: el
 * campo que corresponde viene con valor y el otro nulo.
 */
public record AtencionResponse(
		Long id,
		Long incidenteId,
		/** Todo lo que el paramédico necesita saber antes de salir, cuando la atención es un traslado. */
		TrasladoResponse traslado,
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
		/** Solo en traslados: la unidad llegó y el paciente no estaba listo. */
		Instant horaAvisoNoListo,
		Instant horaCancelacion,
		MotivoCancelacionAtencion motivoCancelacion,
		String nombrePaciente,
		String documentoPaciente,
		Long centroSaludId,
		String destinoDescripcion,
		/** Todos los que pidieron esta ambulancia retiraron su pedido: la unidad decide si sigue o se vuelve. */
		boolean emisoresCancelaron) {

	public static AtencionResponse de(Atencion atencion, boolean emisoresCancelaron) {
		return new AtencionResponse(
				atencion.getId(),
				atencion.getIncidente() == null ? null : atencion.getIncidente().getId(),
				atencion.getTraslado() == null ? null : TrasladoResponse.de(atencion.getTraslado()),
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
				atencion.getHoraAvisoNoListo(),
				atencion.getHoraCancelacion(),
				atencion.getMotivoCancelacion(),
				atencion.getNombrePaciente(),
				atencion.getDocumentoPaciente(),
				atencion.getCentroSalud() == null ? null : atencion.getCentroSalud().getId(),
				atencion.getDestinoDescripcion(),
				emisoresCancelaron);
	}

}
