package com.uem.ambulancias.flota.domain;

import java.util.List;

/**
 * Clasificación de la Norma Nacional de Ambulancias Terrestres (N° 430, 2017). El orden declarado es el orden de
 * capacidad: cada tipo cubre lo que cubre el anterior. De ahí sale la regla de despacho, que es siempre el tipo
 * más bajo que alcance para lo que el paciente necesita.
 */
public enum TipoUnidad {

	/** Transporte simple: paciente estable que camina o va en silla, sin atención durante el viaje. */
	IA,
	/** Rescate y salvataje (Cruz Roja, bomberos). Queda fuera de la escalera: rescata, no traslada. */
	IB,
	/** Soporte vital básico: camilla, oxígeno y monitoreo simple. */
	II,
	/** Soporte vital avanzado: medicación, monitor y vía para pacientes inestables. */
	III;

	/** Los tipos que hacen traslados, de menor a mayor capacidad. */
	public static final List<TipoUnidad> ESCALERA = List.of(IA, II, III);

	/** Si esta unidad alcanza para lo que se pidió. Una IB no sustituye a ninguna de la escalera. */
	public boolean cubreA(TipoUnidad pedido) {
		int propio = ESCALERA.indexOf(this);
		int requerido = ESCALERA.indexOf(pedido);
		return propio >= 0 && requerido >= 0 && propio >= requerido;
	}

	/** El mayor de dos tipos de la escalera: así se combinan varias necesidades en un solo requerimiento. */
	public static TipoUnidad elMayor(TipoUnidad uno, TipoUnidad otro) {
		return ESCALERA.indexOf(uno) >= ESCALERA.indexOf(otro) ? uno : otro;
	}

}
