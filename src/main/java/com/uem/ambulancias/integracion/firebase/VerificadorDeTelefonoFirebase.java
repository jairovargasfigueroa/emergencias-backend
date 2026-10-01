package com.uem.ambulancias.integracion.firebase;

import com.google.firebase.auth.AuthErrorCode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.uem.ambulancias.comun.error.CodigoError;
import com.uem.ambulancias.comun.error.ConflictoException;
import com.uem.ambulancias.seguridad.service.VerificadorDeTelefono;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Verifica con Firebase Authentication el ID token que la app del ciudadano recibió al confirmar el código del SMS.
 * La firma, el vencimiento y que sea de este proyecto los revisa el SDK; de acá sale solo el número, que es lo que se
 * verificó.
 */
@Slf4j
@RequiredArgsConstructor
public class VerificadorDeTelefonoFirebase implements VerificadorDeTelefono {

	/** El dato del token con el número verificado, en formato E.164. */
	private static final String CLAIM_TELEFONO = "phone_number";

	private final FirebaseAuth autenticacion;

	@Override
	public String telefonoVerificado(String idToken) {
		FirebaseToken token;
		try {
			token = autenticacion.verifyIdToken(idToken);
		} catch (FirebaseAuthException e) {
			// Si no se pudieron traer las claves de Google, el token puede estar bien: no hay que hacerle repetir el SMS.
			if (e.getAuthErrorCode() == AuthErrorCode.CERTIFICATE_FETCH_FAILED) {
				log.error("No se pudieron traer las claves de Firebase para verificar el número de un ciudadano.", e);
				throw new ConflictoException(CodigoError.VERIFICACION_NO_DISPONIBLE,
						"No se pudo verificar tu número en este momento. Intenta de nuevo en un rato.");
			}
			throw verificacionInvalida();
		} catch (IllegalArgumentException e) {
			throw verificacionInvalida();
		}
		// Un token de otro método de ingreso (correo, Google) es válido, pero no prueba ningún número.
		if (token.getClaims().get(CLAIM_TELEFONO) instanceof String telefono && !telefono.isBlank()) {
			return telefono;
		}
		throw verificacionInvalida();
	}

	private static ConflictoException verificacionInvalida() {
		return new ConflictoException(CodigoError.VERIFICACION_TELEFONO_INVALIDA,
				"No se pudo confirmar tu número. Vuelve a verificarlo con el código del SMS.");
	}

}
