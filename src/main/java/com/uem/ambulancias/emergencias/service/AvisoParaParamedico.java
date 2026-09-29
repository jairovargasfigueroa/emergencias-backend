package com.uem.ambulancias.emergencias.service;

import java.util.Map;

/**
 * Un push para un paramédico, ya armado: a qué teléfono, qué dice y de qué tipo es, que es lo que la app mira para
 * saber qué volver a pedir.
 */
public record AvisoParaParamedico(String tokenPush, String titulo, String cuerpo, Map<String, String> datos) {
}
