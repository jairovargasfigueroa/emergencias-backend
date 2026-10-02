package com.uem.ambulancias.evidencias.service;

import java.time.Instant;

/**
 * Con qué se generó un resultado: proveedor, modelo, versión del prompt y método ({@code model}, o
 * {@code single_evidence} cuando el resumen se armó sin llamar al modelo). Se guarda junto al resultado.
 */
public record Procedencia(String proveedor, String modelo, String versionPrompt, Instant generadoEn, String metodo) {
}
