package com.uem.ambulancias.seguridad.service;

/**
 * Prueba que quien entra a la app del ciudadano tiene en la mano el teléfono que dice tener. Lo hace Firebase
 * Authentication, mandándole un SMS con un código: la app confirma ese código y recibe un ID token, que es lo que
 * llega acá. Sin esto, cualquiera que escribiera un número ajeno entraría a la cuenta de otro.
 */
public interface VerificadorDeTelefono {

	/**
	 * El número que Firebase verificó, en formato E.164 (por ejemplo, {@code +59171234567}).
	 *
	 * <p>Si el token no vale, venció o no trae un teléfono, lanza un
	 * {@link com.uem.ambulancias.comun.error.ConflictoException} con {@code VERIFICACION_TELEFONO_INVALIDA}; si
	 * ahora no se puede verificar, con {@code VERIFICACION_NO_DISPONIBLE}.
	 */
	String telefonoVerificado(String idToken);

}
