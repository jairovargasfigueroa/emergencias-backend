package com.uem.ambulancias.integracion.firebase;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.common.util.concurrent.MoreExecutors;

import lombok.extern.slf4j.Slf4j;

/**
 * Las escrituras en Firebase son asíncronas. Si fallan, solo queda registro: el cambio en la base ya está
 * confirmado.
 */
@Slf4j
final class EscriturasFirebase {

	private EscriturasFirebase() {
	}

	static <T> void registrarFallo(ApiFuture<T> escritura, String descripcion) {
		ApiFutures.addCallback(escritura, new ApiFutureCallback<T>() {

			@Override
			public void onFailure(Throwable error) {
				log.error("Falló en Firebase: {}.", descripcion, error);
			}

			@Override
			public void onSuccess(T resultado) {
				// Nada que hacer: las apps reciben el cambio por su listener.
			}

		}, MoreExecutors.directExecutor());
	}

}
