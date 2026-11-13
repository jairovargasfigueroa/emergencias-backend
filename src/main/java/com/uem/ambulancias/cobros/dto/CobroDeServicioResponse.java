package com.uem.ambulancias.cobros.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.uem.ambulancias.cobros.domain.CambioDeCobro;
import com.uem.ambulancias.cobros.domain.Cobro;
import com.uem.ambulancias.cobros.domain.EstadoCobro;
import com.uem.ambulancias.cobros.domain.ModalidadPago;
import com.uem.ambulancias.cobros.domain.TipoCambioDeCobro;
import com.uem.ambulancias.cobros.domain.TipoServicio;
import com.uem.ambulancias.emergencias.domain.EstadoAtencion;

/**
 * El cobro de un servicio tal como lo ve la central: si ya se puede cobrar, la tarifa de referencia del día en que se
 * hizo, el cobro (nulo si todavía no se registró) y sus cambios, el más reciente primero.
 */
public record CobroDeServicioResponse(
		Long atencionId,
		TipoServicio tipoServicio,
		EstadoAtencion estadoAtencion,
		/** Solo una atención que terminó (paciente entregado o sin traslado) se cobra. */
		boolean terminado,
		String paciente,
		TarifaResponse tarifaDeReferencia,
		CobroResponse cobro,
		List<CambioResponse> cambios) {

	public record CobroResponse(Long id, ModalidadPago modalidad, BigDecimal importe, EstadoCobro estado,
			String registradoPor, Instant fechaRegistro) {

		public static CobroResponse de(Cobro cobro) {
			return new CobroResponse(cobro.getId(), cobro.getModalidad(), cobro.getImporte(), cobro.getEstado(),
					cobro.getRegistradoPor().getNombreCompleto(), cobro.getFechaRegistro());
		}
	}

	public record CambioResponse(TipoCambioDeCobro tipo, ModalidadPago modalidadAnterior, BigDecimal importeAnterior,
			EstadoCobro estadoAnterior, ModalidadPago modalidadNueva, BigDecimal importeNuevo, EstadoCobro estadoNuevo,
			String motivo, Instant fechaHora, String responsable) {

		public static CambioResponse de(CambioDeCobro c) {
			return new CambioResponse(c.getTipo(), c.getModalidadAnterior(), c.getImporteAnterior(),
					c.getEstadoAnterior(), c.getModalidadNueva(), c.getImporteNuevo(), c.getEstadoNuevo(),
					c.getMotivo(), c.getFechaHora(), c.getResponsable().getNombreCompleto());
		}
	}

}
