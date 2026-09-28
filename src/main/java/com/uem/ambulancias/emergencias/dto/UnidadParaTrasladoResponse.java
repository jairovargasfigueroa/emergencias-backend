package com.uem.ambulancias.emergencias.dto;

import java.time.Instant;

import com.uem.ambulancias.emergencias.service.UnidadCandidata;
import com.uem.ambulancias.flota.domain.TipoUnidad;

/**
 * Una unidad que se puede asignar a mano a un traslado. Las que el barrido descartaría vienen igual, marcadas:
 * quien asigna a mano puede saber algo que el sistema no.
 */
public record UnidadParaTrasladoResponse(

		Long ambulanciaId,
		String placa,
		TipoUnidad tipoUnidad,
		/** En línea recta hasta el origen del traslado. Nula si la unidad nunca reportó su posición. */
		Double distanciaMetros,
		/** Cuándo reportó su posición por última vez. */
		Instant posicionEn,
		/** Reportó su posición hace poco. Sin eso el barrido no le asigna nada solo. */
		boolean posicionReciente,
		/** Ya tuvo este traslado y lo dejó. El barrido no se lo vuelve a ofrecer. */
		boolean yaLoTuvo) {

	public static UnidadParaTrasladoResponse de(UnidadCandidata candidata) {
		return new UnidadParaTrasladoResponse(
				candidata.ambulancia().getId(),
				candidata.ambulancia().getPlaca(),
				candidata.ambulancia().getTipoUnidad(),
				candidata.distanciaMetros(),
				candidata.ambulancia().getUltimaPosicionEn(),
				candidata.posicionReciente(),
				candidata.yaLoTuvo());
	}

}
