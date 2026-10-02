package com.uem.ambulancias.evidencias.service;

import java.util.List;

/**
 * La propuesta de resumen del incidente. {@code resumenJson} es el objeto {@code summary} tal como vino. Las listas
 * dicen qué fuentes usó: con ellas se decide si la propuesta reemplaza a la versión vigente.
 */
public record ResumenRecibido(String resumenJson, List<Long> evidenciasUsadas, List<Long> alertasUsadas,
		Procedencia procedencia) {
}
