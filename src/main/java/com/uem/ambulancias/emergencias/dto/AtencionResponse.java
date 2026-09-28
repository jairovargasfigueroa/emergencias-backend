package com.uem.ambulancias.emergencias.dto;

import java.time.Instant;
import java.util.List;

import com.uem.ambulancias.emergencias.domain.Atencion;
import com.uem.ambulancias.emergencias.domain.EstadoAtencion;
import com.uem.ambulancias.emergencias.domain.Incidente;
import com.uem.ambulancias.emergencias.domain.MotivoCancelacionAtencion;
import com.uem.ambulancias.emergencias.domain.MotivoSinTraslado;

/**
 * La atención como la ve la app del paramédico. Cuelga de un incidente o de un traslado, nunca de los dos: el
 * campo que corresponde viene con valor y el otro nulo.
 */
public record AtencionResponse(
		Long id,
		Long incidenteId,
		/**
		 * Lo que el paramédico necesita saber del incidente. Llega también cuando el incidente ya se cerró y Firebase lo
		 * retiró, que es el rato entre entregar y liberarse. Nulo en traslados.
		 */
		DatosDelIncidente incidente,
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
		/** Solo en traslados: hasta cuándo espera la tripulación a ese paciente antes de poder retirarse. */
		Instant esperaHasta,
		Instant horaCancelacion,
		MotivoCancelacionAtencion motivoCancelacion,
		String nombrePaciente,
		String documentoPaciente,
		Long centroSaludId,
		String destinoDescripcion,
		/** Todos los que pidieron esta ambulancia retiraron su pedido: la unidad decide si sigue o se vuelve. */
		boolean emisoresCancelaron) {

	/** Dónde es y qué se sabe del incidente. */
	public record DatosDelIncidente(double latitud, double longitud, Instant fechaHoraCreacion,
			Integer cantidadAfectados, List<String> descripciones) {
	}

	public static AtencionResponse de(Atencion atencion, boolean emisoresCancelaron, List<String> descripciones) {
		Incidente incidente = atencion.getIncidente();
		return new AtencionResponse(
				atencion.getId(),
				incidente == null ? null : incidente.getId(),
				incidente == null ? null : new DatosDelIncidente(incidente.getUbicacion().getY(),
						incidente.getUbicacion().getX(), incidente.getFechaHoraCreacion(),
						incidente.getCantidadAfectados(), descripciones),
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
				atencion.getEsperaHasta(),
				atencion.getHoraCancelacion(),
				atencion.getMotivoCancelacion(),
				atencion.getNombrePaciente(),
				atencion.getDocumentoPaciente(),
				atencion.getCentroSalud() == null ? null : atencion.getCentroSalud().getId(),
				atencion.getDestinoDescripcion(),
				emisoresCancelaron);
	}

}
