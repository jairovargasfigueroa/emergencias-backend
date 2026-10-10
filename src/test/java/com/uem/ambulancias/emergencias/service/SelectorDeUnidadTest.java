package com.uem.ambulancias.emergencias.service;

import static com.uem.ambulancias.soporte.Conflictos.rechazaCon;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.uem.ambulancias.comun.error.CodigoError;
import com.uem.ambulancias.emergencias.domain.Movilidad;
import com.uem.ambulancias.emergencias.domain.Necesidades;
import com.uem.ambulancias.flota.domain.TipoUnidad;

@Tag("PB-25")
class SelectorDeUnidadTest {

	private final SelectorDeUnidad selector = new SelectorDeUnidad(EstimadorDeTiemposTest.CONFIG);

	@ParameterizedTest(name = "{0}, oxígeno {1}, equipo {2} → {3}")
	@CsvSource({
			"CAMINA_CON_AYUDA, false, false, IA",
			"SILLA_DE_RUEDAS, false, false, IA",
			"CAMILLA, false, false, II",
			"CAMINA_CON_AYUDA, true, false, II",
			"CAMINA_CON_AYUDA, false, true, III",
			"CAMILLA, true, true, III" })
	@DisplayName("PB-25 · el tipo de unidad sale de lo que necesita el paciente: la más chica que alcanza")
	void sugerido(Movilidad movilidad, boolean oxigeno, boolean equipo, TipoUnidad esperado) {
		assertThat(selector.sugerirPara(movilidad, oxigeno, equipo)).isEqualTo(esperado);
	}

	@Test
	@DisplayName("PB-25 · se puede pedir una unidad mayor que la necesaria, nunca una menor")
	void elegido() {
		Necesidades enCamilla = necesidades(Movilidad.CAMILLA);

		assertThat(selector.resolver(enCamilla, null)).isEqualTo(TipoUnidad.II);
		assertThat(selector.resolver(enCamilla, TipoUnidad.III)).isEqualTo(TipoUnidad.III);
		rechazaCon(CodigoError.UNIDAD_INSUFICIENTE, () -> selector.resolver(enCamilla, TipoUnidad.IA));
	}

	@Test
	@DisplayName("PB-25 · una IB no entra en la escalera de traslados: no cubre ni la necesidad más chica")
	void unidadIb() {
		assertThat(TipoUnidad.IB.cubreA(TipoUnidad.IA)).isFalse();
		rechazaCon(CodigoError.UNIDAD_INSUFICIENTE,
				() -> selector.resolver(necesidades(Movilidad.CAMINA_CON_AYUDA), TipoUnidad.IB));
	}

	private static Necesidades necesidades(Movilidad movilidad) {
		return new Necesidades(movilidad, false, false, false, null, 0, null);
	}

}
