package com.uem.ambulancias.seguridad.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Entrada del ciudadano a su app: el ID token que Firebase le dio al verificar su número por SMS. El nombre y el
 * aviso de privacidad solo hacen falta la primera vez, cuando ese número todavía no tiene cuenta.
 */
public record IngresoCiudadanoRequest(

		@NotBlank(message = "La verificación del teléfono es obligatoria.")
		String idToken,

		@Size(max = 255, message = "El nombre es demasiado largo.")
		String nombreCompleto,

		Boolean aceptaPrivacidad) {
}
