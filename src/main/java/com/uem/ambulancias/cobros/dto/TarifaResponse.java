package com.uem.ambulancias.cobros.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.uem.ambulancias.cobros.domain.TarifaReferencia;
import com.uem.ambulancias.cobros.domain.TipoServicio;

public record TarifaResponse(Long id, TipoServicio tipoServicio, BigDecimal monto, LocalDate vigenteDesde,
		String registradaPor, Instant fechaRegistro) {

	public static TarifaResponse de(TarifaReferencia tarifa) {
		return tarifa == null ? null
				: new TarifaResponse(tarifa.getId(), tarifa.getTipoServicio(), tarifa.getMonto(),
						tarifa.getVigenteDesde(), tarifa.getRegistradaPor().getNombreCompleto(),
						tarifa.getFechaRegistro());
	}

}
