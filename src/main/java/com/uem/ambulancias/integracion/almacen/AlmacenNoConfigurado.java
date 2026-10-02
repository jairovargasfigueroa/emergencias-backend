package com.uem.ambulancias.integracion.almacen;

import java.time.Duration;
import java.util.Optional;

import com.uem.ambulancias.comun.error.CodigoError;
import com.uem.ambulancias.comun.error.ConflictoException;
import com.uem.ambulancias.evidencias.service.AlmacenDeEvidencias;
import com.uem.ambulancias.evidencias.service.LecturaFirmada;
import com.uem.ambulancias.evidencias.service.ObjetoAlmacenado;
import com.uem.ambulancias.evidencias.service.SubidaFirmada;

/**
 * Reemplazo mientras no haya almacén configurado. Rechaza con un mensaje claro en vez de firmar URLs que no llevan a
 * ningún lado: la app muestra que no se pueden adjuntar archivos y la alerta sigue igual.
 */
public class AlmacenNoConfigurado implements AlmacenDeEvidencias {

	@Override
	public SubidaFirmada firmarSubida(String claveObjeto, String mimeType, long tamanoBytes, String sha256Base64,
			Duration vigencia) {
		throw noDisponible();
	}

	@Override
	public Optional<ObjetoAlmacenado> verificarObjeto(String claveObjeto) {
		throw noDisponible();
	}

	@Override
	public LecturaFirmada firmarLectura(String claveObjeto, Duration vigencia) {
		throw noDisponible();
	}

	@Override
	public void borrar(String claveObjeto) {
		throw noDisponible();
	}

	private static ConflictoException noDisponible() {
		return new ConflictoException(CodigoError.ALMACENAMIENTO_NO_DISPONIBLE,
				"Por ahora no se pueden adjuntar archivos: el almacén de evidencias no está configurado.");
	}

}
