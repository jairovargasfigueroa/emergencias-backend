package com.uem.ambulancias.seguridad.dto;

/**
 * Lo que manda el paramédico para renovar su sesión: la clave de su teléfono vinculado. El ciudadano no manda nada,
 * así que el cuerpo entero es opcional.
 */
public record RenovacionSesionRequest(String claveDispositivo) {
}
