package com.uem.ambulancias.evidencias.service;

import com.uem.ambulancias.evidencias.domain.Evidencia;

/** Una evidencia con la URL firmada para subir su archivo. */
public record EvidenciaConSubida(Evidencia evidencia, SubidaFirmada subida) {
}
