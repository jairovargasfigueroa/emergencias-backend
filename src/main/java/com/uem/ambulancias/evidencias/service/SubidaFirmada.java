package com.uem.ambulancias.evidencias.service;

import java.time.Instant;
import java.util.Map;

/** URL de subida con las cabeceras que la app tiene que mandar sin cambiarlas, y hasta cuándo sirve. */
public record SubidaFirmada(String url, Map<String, String> cabeceras, Instant venceEn) {
}
