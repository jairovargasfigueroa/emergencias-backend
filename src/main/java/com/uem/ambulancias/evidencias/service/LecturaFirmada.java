package com.uem.ambulancias.evidencias.service;

import java.time.Instant;

/** URL temporal para leer un archivo, y hasta cuándo sirve. */
public record LecturaFirmada(String url, Instant venceEn) {
}
