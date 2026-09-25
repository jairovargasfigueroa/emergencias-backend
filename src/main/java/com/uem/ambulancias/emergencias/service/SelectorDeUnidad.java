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

	public TipoUnidad sugerirPara(Necesidades necesidades) {
		return sugerirPara(necesidades.movilidad(), necesidades.oxigeno(), necesidades.equipo());
	}

	/**
	 * El mayor de los mínimos que apliquen. Sin ninguna necesidad especial alcanza un transporte simple. Los
	 * demás datos del pedido —peso, acompañantes, aislamiento— no cambian el tipo: cambian cómo se prepara la
	 * tripulación.
	 */
	public TipoUnidad sugerirPara(Movilidad movilidad, boolean oxigeno, boolean equipo) {
		TipoUnidad tipo = TipoUnidad.IA;
		if (movilidad == Movilidad.CAMILLA) {
			tipo = TipoUnidad.elMayor(tipo, config.minimoParaCamilla());
		}
		if (oxigeno) {
			tipo = TipoUnidad.elMayor(tipo, config.minimoParaOxigeno());
		}
		if (equipo) {
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
