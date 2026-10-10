package com.uem.ambulancias.soporte;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;

import com.uem.ambulancias.comun.error.CodigoError;
import com.uem.ambulancias.comun.error.ConflictoException;

/** Para las unitarias: que una regla rechace con el código exacto, que es lo que la app le muestra a la gente. */
public final class Conflictos {

	private Conflictos() {
	}

	public static void rechazaCon(CodigoError codigo, ThrowingCallable accion) {
		assertThatThrownBy(accion)
				.isInstanceOf(ConflictoException.class)
				.extracting("codigo")
				.isEqualTo(codigo);
	}

}
