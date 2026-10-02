package com.uem.ambulancias.evidencias.service;

import java.time.Duration;
import java.util.Optional;

/**
 * Dónde viven los archivos de las evidencias. El servidor nunca los recibe ni los entrega: firma URLs temporales para
 * que la app suba y el personal o el análisis lean directo del almacén, y comprueba lo subido sin descargarlo.
 */
public interface AlmacenDeEvidencias {

	/**
	 * URL para subir un archivo con un PUT. La firma fija el tipo, el tamaño y el SHA-256: el almacén rechaza cualquier
	 * otro archivo. Las cabeceras que devuelve son las que la app tiene que mandar tal cual.
	 */
	SubidaFirmada firmarSubida(String claveObjeto, String mimeType, long tamanoBytes, String sha256Base64,
			Duration vigencia);

	/** Lo que el almacén dice del objeto, o vacío si no existe. */
	Optional<ObjetoAlmacenado> verificarObjeto(String claveObjeto);

	/** URL para leer el archivo con un GET. */
	LecturaFirmada firmarLectura(String claveObjeto, Duration vigencia);

	/** Borra el archivo. Si ya no estaba, no pasa nada. */
	void borrar(String claveObjeto);

}
