package com.uem.ambulancias.flota.dto;

import java.time.Instant;

import com.uem.ambulancias.seguridad.service.AccesoParamedicoService;

/**
 * El código que la central le entrega en persona al paramédico, con el formato {@code XXXX-XXXX}, y hasta cuándo
 * sirve. Se ve esta única vez: el servidor solo lo guarda cifrado.
 */
public record CodigoActivacionResponse(String codigo, Instant venceEn) {

	public static CodigoActivacionResponse de(AccesoParamedicoService.CodigoDeActivacion codigo) {
		return new CodigoActivacionResponse(codigo.codigo(), codigo.venceEn());
	}

}
