package com.uem.ambulancias.evidencias.domain;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * Los formatos que se aceptan, los mismos que admite el servicio de análisis. Fuera de esta lista el archivo no se
 * podría analizar, así que ni se firma su subida.
 */
public enum FormatoEvidencia {

	JPEG("image/jpeg", "jpg", Modalidad.IMAGEN),
	PNG("image/png", "png", Modalidad.IMAGEN),
	WEBP("image/webp", "webp", Modalidad.IMAGEN),

	AUDIO_MP4("audio/mp4", "m4a", Modalidad.AUDIO),
	M4A("audio/m4a", "m4a", Modalidad.AUDIO),
	X_M4A("audio/x-m4a", "m4a", Modalidad.AUDIO),
	MPEG("audio/mpeg", "mp3", Modalidad.AUDIO),
	AAC("audio/aac", "aac", Modalidad.AUDIO),
	WAV("audio/wav", "wav", Modalidad.AUDIO),
	X_WAV("audio/x-wav", "wav", Modalidad.AUDIO),
	OGG("audio/ogg", "ogg", Modalidad.AUDIO),

	MP4("video/mp4", "mp4", Modalidad.VIDEO),
	QUICKTIME("video/quicktime", "mov", Modalidad.VIDEO),
	WEBM("video/webm", "webm", Modalidad.VIDEO);

	private final String mimeType;
	private final String extension;
	private final Modalidad modalidad;

	FormatoEvidencia(String mimeType, String extension, Modalidad modalidad) {
		this.mimeType = mimeType;
		this.extension = extension;
		this.modalidad = modalidad;
	}

	public String mimeType() {
		return mimeType;
	}

	public String extension() {
		return extension;
	}

	public Modalidad modalidad() {
		return modalidad;
	}

	/**
	 * El formato de un MIME tal como lo manda la app. Se ignoran mayúsculas y parámetros ({@code ;codecs=...}): lo
	 * que se firma y lo que tiene que mandar la app después es el MIME limpio de la lista.
	 */
	public static Optional<FormatoEvidencia> deMime(String mimeType) {
		if (mimeType == null) {
			return Optional.empty();
		}
		String limpio = mimeType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
		return Arrays.stream(values()).filter(formato -> formato.mimeType.equals(limpio)).findFirst();
	}

}
