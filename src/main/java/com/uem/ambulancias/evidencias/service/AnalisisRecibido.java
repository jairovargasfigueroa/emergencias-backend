package com.uem.ambulancias.evidencias.service;

/**
 * El análisis de una evidencia. {@code analisisJson} es el objeto {@code analysis} tal como vino: se guarda sin tocar
 * y se reenvía igual al pedir el resumen.
 *
 * @param modalidad la que informa el servicio: {@code image}, {@code audio} o {@code video}
 */
public record AnalisisRecibido(String modalidad, String versionEsquema, String analisisJson, Procedencia procedencia) {
}
