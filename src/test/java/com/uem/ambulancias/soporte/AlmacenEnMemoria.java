package com.uem.ambulancias.soporte;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.uem.ambulancias.evidencias.service.AlmacenDeEvidencias;
import com.uem.ambulancias.evidencias.service.LecturaFirmada;
import com.uem.ambulancias.evidencias.service.ObjetoAlmacenado;
import com.uem.ambulancias.evidencias.service.SubidaFirmada;

/**
 * Reemplaza a S3 en las pruebas. Firma URLs que no llevan a ningún lado y guarda en memoria lo que la prueba dice
 * haber subido, así el servidor comprueba el archivo igual que contra el almacén real.
 */
public class AlmacenEnMemoria implements AlmacenDeEvidencias {

	private static final String RAIZ = "https://almacen.prueba/";

	private final Map<String, ObjetoAlmacenado> objetos = new ConcurrentHashMap<>();
	private final List<String> borrados = new CopyOnWriteArrayList<>();

	@Override
	public SubidaFirmada firmarSubida(String claveObjeto, String mimeType, long tamanoBytes, String sha256Base64,
			Duration vigencia) {
		return new SubidaFirmada(RAIZ + claveObjeto + "?firma=subida",
				Map.of("Content-Type", mimeType, "x-amz-checksum-sha256", sha256Base64),
				Instant.now().plus(vigencia));
	}

	@Override
	public Optional<ObjetoAlmacenado> verificarObjeto(String claveObjeto) {
		return Optional.ofNullable(objetos.get(claveObjeto));
	}

	@Override
	public LecturaFirmada firmarLectura(String claveObjeto, Duration vigencia) {
		return new LecturaFirmada(RAIZ + claveObjeto + "?firma=lectura", Instant.now().plus(vigencia));
	}

	@Override
	public void borrar(String claveObjeto) {
		objetos.remove(claveObjeto);
		borrados.add(claveObjeto);
	}

	/** Lo que haría la app con la URL firmada: deja el archivo en el almacén. */
	public void subir(String urlSubida, long tamanoBytes, String sha256Base64) {
		objetos.put(claveDe(urlSubida), new ObjetoAlmacenado(tamanoBytes, sha256Base64));
	}

	public boolean fueBorrado(String claveObjeto) {
		return borrados.contains(claveObjeto);
	}

	/** La clave del objeto que hay detrás de una URL firmada por este almacén. */
	public static String claveDe(String url) {
		return url.substring(RAIZ.length(), url.indexOf('?'));
	}

	/** Cada prueba empieza con el almacén vacío. */
	public void olvidar() {
		objetos.clear();
		borrados.clear();
	}

}
