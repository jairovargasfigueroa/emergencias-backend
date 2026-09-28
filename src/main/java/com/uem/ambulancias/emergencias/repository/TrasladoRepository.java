package com.uem.ambulancias.emergencias.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.uem.ambulancias.emergencias.domain.Traslado;
import com.uem.ambulancias.flota.domain.TipoUnidad;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TrasladoRepository extends JpaRepository<Traslado, Long> {

	/** "Mis traslados" del ciudadano: los próximos arriba y el historial abajo, todo en una sola lista. */
	List<Traslado> findBySolicitanteIdOrderByFechaHoraCreacionDesc(Long solicitanteId);

	/**
	 * El traslado con su fila bloqueada hasta que termine la transacción. Todo el que lo cambia pasa por acá:
	 * Hibernate guarda la fila entera, así que dos cambios que se cruzan —el barrido que asigna y el administrador
	 * que asigna a mano, o el ciudadano que cancela— se pisarían sin avisar y el último borraría lo del otro.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select t from Traslado t where t.id = :id")
	Optional<Traslado> buscarParaActualizar(@Param("id") Long id);

	/**
	 * Los programados a los que ya les llegó la hora de salir. El job los pasa a buscar unidad; hasta acá nadie
	 * reservó nada. Con la fila bloqueada: si justo alguien lo está cancelando, se espera a que termine, y el
	 * traslado ya cancelado deja de cumplir la condición.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select t from Traslado t
			where t.estado = 'PROGRAMADO' and t.horaSalidaEstimada <= :hasta
			order by t.horaSalidaEstimada asc
			""")
	List<Traslado> buscarPorSalir(@Param("hasta") Instant hasta);

	/**
	 * Los que están esperando unidad, en orden de prioridad: cuando queda una sola ambulancia se la lleva el que
	 * primero se cae, no el que primero pidió. Antes que todos, los que una unidad devolvió: esa familia ya estaba
	 * esperando y el viaje ya viene tarde.
	 */
	@Query("""
			select t from Traslado t
			where t.estado = 'BUSCANDO_UNIDAD'
			order by case when t.horaDevolucion is null then 1 else 0 end, t.horaLimiteSalida asc
			""")
	List<Traslado> buscarEsperandoUnidad();

	/**
	 * La bandeja del administrador: los que tienen una unidad que ya debería haber llegado, los que esperan unidad
	 * y los que se vencieron sin ella mientras nadie le haya avisado a la familia. Primero lo que todavía se puede
	 * salvar: la familia que está esperando en la puerta, y después los que se pueden asignar a mano.
	 */
	@Query("""
			select t from Traslado t
			where t.estado = 'BUSCANDO_UNIDAD'
			   or (t.estado = 'NO_CUBIERTO' and t.horaFamiliaAvisada is null)
			   or (t.estado = 'ASIGNADO' and t.horaRecogidaHasta < :ahora and exists (
			         select a.id from Atencion a where a.traslado = t and a.estado = 'EN_CAMINO'))
			order by case when t.estado = 'ASIGNADO' then 0 when t.estado = 'BUSCANDO_UNIDAD' then 1 else 2 end,
			         case when t.horaDevolucion is null then 1 else 0 end, t.horaLimiteSalida asc
			""")
	List<Traslado> buscarProblemas(@Param("ahora") Instant ahora);

	/**
	 * Lo mismo, pero solo los que una unidad de cierto tipo puede cubrir. Se usa cuando se libera una ambulancia:
	 * en vez de esperar al próximo barrido, se revisa en el momento si hay alguien esperándola.
	 *
	 * <p>Solo los ids: quien asigna vuelve a leer cada traslado con la fila bloqueada, y si el traslado ya
	 * estuviera cargado en la transacción, Hibernate devolvería esa copia vieja en vez de la recién bloqueada.
	 */
	@Query("""
			select t.id from Traslado t
			where t.estado = 'BUSCANDO_UNIDAD'
			  and coalesce(t.tipoUnidadCorregido, t.tipoUnidadPedido) in :tipos
			order by case when t.horaDevolucion is null then 1 else 0 end, t.horaLimiteSalida asc
			""")
	List<Long> buscarIdsEsperandoUnidadDeTipo(@Param("tipos") Collection<TipoUnidad> tipos);

	/**
	 * A los que ya se les pasó la última salida posible: no llegan a tiempo y hay que avisar a la familia. Con la
	 * fila bloqueada, por si el administrador lo está asignando a mano justo en ese momento.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
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
