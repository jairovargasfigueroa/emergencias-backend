package com.uem.ambulancias.evidencias.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Lo que la app sabe del archivo antes de subirlo. Con esto se firma la subida: el almacén va a rechazar un archivo
 * de otro tipo, otro tamaño u otro contenido.
 */
public record RegistrarEvidenciaRequest(

		@NotBlank(message = "El tipo de archivo es obligatorio.")
		@Size(max = 100, message = "El tipo de archivo es demasiado largo.")
		String mimeType,

		@NotNull(message = "El tamaño es obligatorio.")
		@Positive(message = "El archivo no puede estar vacío.")
		Long tamanoBytes,

		@NotBlank(message = "El SHA-256 es obligatorio.")
		@Pattern(regexp = "^[0-9a-fA-F]{64}$", message = "El SHA-256 tiene que venir en hexadecimal (64 caracteres).")
		String sha256) {
}
