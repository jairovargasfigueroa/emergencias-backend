package com.uem.ambulancias.emergencias.service;

import com.uem.ambulancias.flota.domain.Ambulancia;

/**
 * Una unidad que el administrador puede mandar a mano, con lo que necesita saber para elegir bien: a qué distancia
 * está y si el sistema la habría descartado.
 *
 * @param distanciaMetros en línea recta hasta el punto; nula si la unidad nunca reportó su posición.
 * @param posicionReciente si reportó su posición hace poco: sin eso el barrido no le asigna nada.
 * @param yaLoTuvo si ya estuvo en este caso y lo dejó.
 */
public record UnidadCandidata(Ambulancia ambulancia, Double distanciaMetros, boolean posicionReciente,
		boolean yaLoTuvo) {
}
