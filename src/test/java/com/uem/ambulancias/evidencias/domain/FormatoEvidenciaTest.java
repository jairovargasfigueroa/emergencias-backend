package com.uem.ambulancias.evidencias.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

@Tag("PB-18")
class FormatoEvidenciaTest {

	@ParameterizedTest(name = "{0}")
	@CsvSource({
			"image/jpeg, IMAGEN",
			"image/png, IMAGEN",
			"image/webp, IMAGEN",
			"audio/mp4, AUDIO",
			"audio/mpeg, AUDIO",
			"audio/wav, AUDIO",
			"video/mp4, VIDEO" })
	@DisplayName("PB-18 · cada tipo admitido sabe si es foto, audio o video")
	void modalidad(String mimeType, Modalidad modalidad) {
		assertThat(FormatoEvidencia.deMime(mimeType)).get()
				.extracting(FormatoEvidencia::modalidad)
				.isEqualTo(modalidad);
	}

	@Test
	@DisplayName("PB-18 · el tipo se reconoce sin importar mayúsculas ni parámetros")
	void normaliza() {
		assertThat(FormatoEvidencia.deMime("Image/JPEG; charset=binary")).contains(FormatoEvidencia.JPEG);
		assertThat(FormatoEvidencia.deMime("  audio/x-m4a  ")).contains(FormatoEvidencia.X_M4A);
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = { "application/pdf", "text/plain", "image/gif", "" })
	@DisplayName("PB-18 · un tipo que no se puede analizar no se reconoce")
	void noAdmitido(String mimeType) {
		assertThat(FormatoEvidencia.deMime(mimeType)).isEmpty();
	}

	@Test
	@DisplayName("PB-18 · sin tipo no hay formato")
	void sinTipo() {
		assertThat(FormatoEvidencia.deMime(null)).isEmpty();
	}

}
