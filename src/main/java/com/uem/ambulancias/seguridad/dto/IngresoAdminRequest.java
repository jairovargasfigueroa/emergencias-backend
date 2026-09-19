package com.uem.ambulancias.seguridad.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Credenciales del administrador para entrar al panel.
 */
public record IngresoAdminRequest(
		@NotBlank(message = "El correo es obligatorio.")
		@Email(message = "El correo no tiene un formato válido.")
		String correo,
		@NotBlank(message = "La clave es obligatoria.")
		String clave) {
}
