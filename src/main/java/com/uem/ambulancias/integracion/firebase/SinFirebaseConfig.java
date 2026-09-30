package com.uem.ambulancias.integracion.firebase;

import com.uem.ambulancias.comun.error.CodigoError;
import com.uem.ambulancias.comun.error.ConflictoException;
import com.uem.ambulancias.seguridad.service.VerificadorDeTelefono;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "sga.firebase", name = "habilitado", havingValue = "false", matchIfMissing = true)
public class SinFirebaseConfig {

	@Bean
	PublicadorEnRegistro publicadorEnRegistro() {
		return new PublicadorEnRegistro();
	}

	/**
	 * Sin Firebase no hay quién verifique por SMS el número del ciudadano. Se rechaza el ingreso con un mensaje claro
	 * antes que dejar pasar un número sin verificar, que es entrar a la cuenta de quien uno quiera.
	 */
	@Bean
	VerificadorDeTelefono verificadorDeTelefono() {
		return idToken -> {
			throw new ConflictoException(CodigoError.VERIFICACION_NO_DISPONIBLE,
					"El ingreso de ciudadanos necesita Firebase habilitado (sga.firebase.habilitado=true).");
		};
	}

}
