package com.uem.ambulancias.emergencias.service;

import com.uem.ambulancias.comun.error.CodigoError;
import com.uem.ambulancias.comun.error.ConflictoException;
import com.uem.ambulancias.emergencias.domain.Movilidad;
import com.uem.ambulancias.emergencias.domain.Necesidades;
import com.uem.ambulancias.flota.domain.TipoUnidad;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Qué unidad hace falta según lo que el paciente necesita. El ciudadano no elige un vehículo: responde sobre su
 * paciente y el sistema deriva el tipo, tomando siempre el más bajo que alcance.
 */
@Service
@RequiredArgsConstructor
public class SelectorDeUnidad {

	private final TrasladoProperties config;

	/** El mayor de los mínimos que apliquen. Sin ninguna necesidad especial alcanza un transporte simple. */
	public TipoUnidad sugerirPara(Necesidades necesidades) {
		TipoUnidad tipo = TipoUnidad.IA;
		if (necesidades.movilidad() == Movilidad.CAMILLA) {
			tipo = TipoUnidad.elMayor(tipo, config.minimoParaCamilla());
		}
		if (necesidades.oxigeno()) {
			tipo = TipoUnidad.elMayor(tipo, config.minimoParaOxigeno());
		}
		if (necesidades.equipo()) {
			tipo = TipoUnidad.elMayor(tipo, config.minimoParaEquipo());
		}
		return tipo;
	}

	/**
	 * El ciudadano puede pedir una unidad mejor que la sugerida, nunca peor. Con eso, que la unidad no alcance
	 * deja de poder pasar por elegir barato y solo puede pasar porque el dato estaba mal, que es otro problema.
	 */
	public TipoUnidad resolver(Necesidades necesidades, TipoUnidad elegido) {
		TipoUnidad sugerido = sugerirPara(necesidades);
		if (elegido == null) {
			return sugerido;
		}
		if (!elegido.cubreA(sugerido)) {
			throw new ConflictoException(CodigoError.UNIDAD_INSUFICIENTE,
					"Para lo que necesita el paciente hace falta al menos una unidad " + sugerido + ".");
		}
		return elegido;
	}

}
