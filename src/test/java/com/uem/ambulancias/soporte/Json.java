package com.uem.ambulancias.soporte;

import java.nio.charset.StandardCharsets;

import com.jayway.jsonpath.JsonPath;

/** Arma los cuerpos de las peticiones y lee las respuestas sin depender de cómo serializa la app. */
public final class Json {

	private Json() {
	}

	/** {@code objeto("placa", "ABC123", "tipoUnidad", "II")} da {@code {"placa":"ABC123","tipoUnidad":"II"}}. */
	public static String objeto(Object... clavesYValores) {
		if (clavesYValores.length % 2 != 0) {
			throw new IllegalArgumentException("Faltan valores: van de a pares clave, valor.");
		}
		StringBuilder json = new StringBuilder("{");
		for (int i = 0; i < clavesYValores.length; i += 2) {
			if (i > 0) {
				json.append(',');
			}
			json.append(valor(clavesYValores[i])).append(':').append(valor(clavesYValores[i + 1]));
		}
		return json.append('}').toString();
	}

	public static long numero(String json, String ruta) {
		return ((Number) JsonPath.read(json, ruta)).longValue();
	}

	public static String texto(String json, String ruta) {
		return JsonPath.read(json, ruta);
	}

	public static <T> T leer(String json, String ruta) {
		return JsonPath.read(json, ruta);
	}

	private static String valor(Object valor) {
		if (valor == null) {
			return "null";
		}
		if (valor instanceof Number || valor instanceof Boolean) {
			return valor.toString();
		}
		String texto = valor instanceof Enum<?> enumerado ? enumerado.name() : valor.toString();
		return '"' + texto.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
	}

	static String utf8(byte[] bytes) {
		return new String(bytes, StandardCharsets.UTF_8);
	}

}
