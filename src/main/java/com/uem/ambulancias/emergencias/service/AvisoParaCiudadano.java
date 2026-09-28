package com.uem.ambulancias.emergencias.service;

import java.util.Map;

/**
 * Un push para un ciudadano, ya armado: a qué teléfono, qué dice y qué abre la app al tocarlo.
 */
public record AvisoParaCiudadano(String tokenPush, String titulo, String cuerpo, Map<String, String> datos) {
}
