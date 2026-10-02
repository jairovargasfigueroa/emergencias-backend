package com.uem.ambulancias.evidencias.service;

/**
 * Lo que el almacén informa de un objeto sin descargarlo.
 *
 * @param sha256Base64 el SHA-256 que el almacén calculó al recibirlo, o {@code null} si no lo tiene
 */
public record ObjetoAlmacenado(long tamanoBytes, String sha256Base64) {
}
