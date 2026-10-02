package com.uem.ambulancias.evidencias.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.uem.ambulancias.evidencias.domain.EstadoTrabajoIa;
import com.uem.ambulancias.evidencias.domain.TipoTrabajoIa;
import com.uem.ambulancias.evidencias.domain.TrabajoIa;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TrabajoIaRepository extends JpaRepository<TrabajoIa, Long> {

	/**
	 * Los próximos trabajos de ese tipo que se pueden tomar: los pendientes a los que ya les llegó la hora y los que un
	 * worker dejó a medias y ya vencieron. {@code skip locked} hace que dos workers a la vez se repartan la cola en vez
	 * de esperarse o tomar el mismo.
	 */
	@Query(value = """
			select * from trabajo_ia
			where tipo = :tipo
			  and ((estado = 'PENDIENTE' and proximo_intento <= :ahora)
			    or (estado = 'EN_CURSO' and bloqueado_hasta < :ahora))
			order by proximo_intento, id
			limit :limite
			for update skip locked
			""", nativeQuery = true)
	List<TrabajoIa> tomarDisponibles(@Param("tipo") String tipo, @Param("ahora") Instant ahora,
			@Param("limite") int limite);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select t from TrabajoIa t where t.id = :id")
	Optional<TrabajoIa> buscarParaActualizar(@Param("id") Long id);

	boolean existsByIncidenteIdAndTipoAndEstado(Long incidenteId, TipoTrabajoIa tipo, EstadoTrabajoIa estado);

}
