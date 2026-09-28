package com.uem.ambulancias.emergencias.service;

import com.uem.ambulancias.flota.domain.Ambulancia;

/**
 * Una unidad que el administrador puede elegir para un traslado, con lo que necesita saber para elegir bien: a
 * qué distancia está del origen y si el barrido la habría descartado.
 *
 * @param distanciaMetros en línea recta hasta el origen; nula si la unidad nunca reportó su posición.
 * @param posicionReciente si reportó su posición hace poco: sin eso el barrido no le asigna nada.
 * @param yaLoTuvo si ya tuvo este traslado y lo dejó: el barrido no se lo vuelve a ofrecer.
 */
public record UnidadCandidata(Ambulancia ambulancia, Double distanciaMetros, boolean posicionReciente,
		boolean yaLoTuvo) {
}
