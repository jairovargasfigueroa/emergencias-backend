package com.uem.ambulancias.emergencias.service;

import com.uem.ambulancias.emergencias.domain.Atencion;
import com.uem.ambulancias.emergencias.repository.AtencionRepository;
import com.uem.ambulancias.flota.repository.AmbulanciaRepository;
import com.uem.ambulancias.flota.service.UnidadActualizada;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Publica el estado de cada unidad después del commit: es lo que escucha la app de su tripulación. El cambio no
 * siempre lo hace ella. La central la libera o la saca de servicio, el sistema le asigna un traslado, el compañero
 * marca un hito desde su teléfono. Sin esto, la app se enteraba recién al volver a preguntar.
 *
 * <p>La unidad se lee de nuevo en vez de viajar en el evento: así se publica cómo quedó de verdad.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DifusionDeUnidades {

	private final AmbulanciaRepository ambulancias;
	private final AtencionRepository atenciones;
	private final PublicadorDeUnidades publicador;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
	public void difundir(UnidadActualizada evento) {
		publicar(evento.ambulanciaId());
	}

	/**
	 * Lo que cambia en un incidente también cambia lo que ven las unidades que van para allá: quien avisó retiró su
	 * pedido, o llegaron más datos. Con eso el paramédico decide si sigue o se vuelve.
	 */
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
	public void alActualizarseElIncidente(IncidenteActualizado evento) {
		try {
			atenciones.buscarActivasPorIncidente(evento.incidenteId())
					.forEach(atencion -> publicar(atencion.getAmbulancia().getId()));
		} catch (RuntimeException e) {
			log.error("No se pudo difundir a sus unidades el cambio del incidente {}.", evento.incidenteId(), e);
		}
	}

	private void publicar(Long ambulanciaId) {
		try {
			ambulancias.findById(ambulanciaId).ifPresent(ambulancia -> publicador.publicarUnidad(new UnidadPublicada(
					ambulanciaId, ambulancia.getEstado(),
					atenciones.buscarQueOcupaAmbulancia(ambulanciaId).map(Atencion::getId).orElse(null))));
		} catch (RuntimeException e) {
			// El cambio ya está confirmado: la app lo ve igual en su próxima consulta.
			log.error("No se pudo difundir el estado de la ambulancia {}.", ambulanciaId, e);
		}
	}

}
