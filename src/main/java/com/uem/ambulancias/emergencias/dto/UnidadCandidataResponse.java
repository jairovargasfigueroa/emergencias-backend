package com.uem.ambulancias.emergencias.dto;

import java.time.Instant;

import com.uem.ambulancias.emergencias.service.UnidadCandidata;
import com.uem.ambulancias.flota.domain.TipoUnidad;

/**
 * Una unidad que el administrador puede mandar a mano, a un traslado o a una emergencia. Las que el sistema no
 * elegiría solo vienen igual, marcadas: quien manda a mano puede saber algo que el sistema no.
 */
public record UnidadCandidataResponse(

		Long ambulanciaId,
		String placa,
		TipoUnidad tipoUnidad,
		/** En línea recta hasta el origen del traslado o el lugar de la emergencia. Nula si nunca reportó posición. */
		Double distanciaMetros,
		/** Cuándo reportó su posición por última vez. */
		Instant posicionEn,
		/** Reportó su posición hace poco. Sin eso el barrido no le asigna nada solo. */
		boolean posicionReciente,
		/** Ya estuvo en este caso y lo dejó. En un traslado, el barrido no se lo vuelve a ofrecer. */
		boolean yaLoTuvo) {

	public static UnidadCandidataResponse de(UnidadCandidata candidata) {
		return new UnidadCandidataResponse(
				candidata.ambulancia().getId(),
				candidata.ambulancia().getPlaca(),
				candidata.ambulancia().getTipoUnidad(),
				candidata.distanciaMetros(),
				candidata.ambulancia().getUltimaPosicionEn(),
				candidata.posicionReciente(),
				candidata.yaLoTuvo());
	}

}
