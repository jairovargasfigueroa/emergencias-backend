package com.uem.ambulancias.flota.service;

/**
 * El administrador le cerró el turno a un paramédico. Se le avisa por push: puede no estar mirando la app, y sin el
 * aviso sigue creyendo que está trabajando.
 */
public record TurnoCerradoPorLaCentral(Long paramedicoId) {
}
