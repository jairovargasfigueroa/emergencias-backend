package com.uem.ambulancias.emergencias.repository;

import java.time.Instant;

/** Cuántas falsas alarmas verificadas tiene un ciudadano y cuándo fue la última. */
public record FalsasAlarmasDeCiudadano(Long ciudadanoId, long cantidad, Instant ultima) {
}
