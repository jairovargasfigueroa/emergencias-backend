package com.uem.ambulancias.soporte;

import com.uem.ambulancias.comun.error.CodigoError;
import com.uem.ambulancias.comun.error.ConflictoException;
import com.uem.ambulancias.seguridad.service.VerificadorDeTelefono;

/**
 * Reemplaza la verificación por SMS de Firebase. Un token {@code "telefono:+59170000001"} prueba ese número; cualquier
 * otro es un token que no vale, como uno vencido o falsificado.
 */
public class VerificadorDeTelefonoFalso implements VerificadorDeTelefono {

	private static final String PREFIJO = "telefono:";

	public static String tokenDe(String telefonoE164) {
		return PREFIJO + telefonoE164;
	}

	@Override
	public String telefonoVerificado(String idToken) {
		if (idToken == null || !idToken.startsWith(PREFIJO)) {
			throw new ConflictoException(CodigoError.VERIFICACION_TELEFONO_INVALIDA,
					"No se pudo verificar el teléfono.");
		}
		return idToken.substring(PREFIJO.length());
	}

}
