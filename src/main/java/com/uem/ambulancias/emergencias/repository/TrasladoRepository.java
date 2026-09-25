package com.uem.ambulancias.emergencias.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import com.uem.ambulancias.emergencias.domain.Traslado;
import com.uem.ambulancias.flota.domain.TipoUnidad;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TrasladoRepository extends JpaRepository<Traslado, Long> {

	/** "Mis traslados" del ciudadano: los próximos arriba y el historial abajo, todo en una sola lista. */
	List<Traslado> findBySolicitanteIdOrderByFechaHoraCreacionDesc(Long solicitanteId);

	/**
	 * Los programados a los que ya les llegó la hora de salir. El job los pasa a buscar unidad; hasta acá nadie
	 * reservó nada.
	 */
	@Query("""
			select t from Traslado t
			where t.estado = 'PROGRAMADO' and t.horaSalidaEstimada <= :hasta
			order by t.horaSalidaEstimada asc
			""")
	List<Traslado> buscarPorSalir(@Param("hasta") Instant hasta);

	/**
	 * Los que están esperando unidad, el más urgente primero. Ese orden es la regla de prioridad: cuando queda
	 * una sola ambulancia se la lleva el que primero se cae, no el que primero pidió.
	 */
	@Query("""
			select t from Traslado t
			where t.estado = 'BUSCANDO_UNIDAD'
			order by t.horaLimiteSalida asc
			""")
	List<Traslado> buscarEsperandoUnidad();

	/**
	 * Lo mismo, pero solo los que una unidad de cierto tipo puede cubrir. Se usa cuando se libera una ambulancia:
	 * en vez de esperar al próximo barrido, se revisa en el momento si hay alguien esperándola.
	 */
	@Query("""
			select t from Traslado t
			where t.estado = 'BUSCANDO_UNIDAD'
			  and coalesce(t.tipoUnidadCorregido, t.tipoUnidadPedido) in :tipos
			order by t.horaLimiteSalida asc
			""")
	List<Traslado> buscarEsperandoUnidadDeTipo(@Param("tipos") Collection<TipoUnidad> tipos);

	/** A los que ya se les pasó la última salida posible: no llegan a tiempo y hay que avisar a la familia. */
	@Query("""
			select t from Traslado t
			where t.estado = 'BUSCANDO_UNIDAD' and t.horaLimiteSalida < :ahora
			""")
	List<Traslado> buscarVencidos(@Param("ahora") Instant ahora);

	/** La tabla del panel: todo lo que se mueve en una franja del día, esté asignado o no. */
	@Query("""
			select t from Traslado t
			where t.horaSalidaEstimada >= :desde and t.horaSalidaEstimada < :hasta
			order by t.horaSalidaEstimada asc
			""")
	List<Traslado> buscarEntre(@Param("desde") Instant desde, @Param("hasta") Instant hasta);

}
