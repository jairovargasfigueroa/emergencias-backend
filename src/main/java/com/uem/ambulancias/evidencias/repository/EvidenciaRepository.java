package com.uem.ambulancias.evidencias.repository;

import java.util.Optional;

import com.uem.ambulancias.evidencias.domain.Evidencia;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EvidenciaRepository extends JpaRepository<Evidencia, Long> {

	/** La evidencia con su fila bloqueada: confirmar dos veces a la vez no puede crear dos análisis. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select e from Evidencia e where e.id = :id")
	Optional<Evidencia> buscarParaActualizar(@Param("id") Long id);

	/** La evidencia con su alerta, su emisor y su incidente, que es lo que se mira antes de firmar. */
	@Query("""
			select e from Evidencia e join fetch e.alerta a join fetch a.emisor join fetch a.incidente
			where e.id = :id
			""")
	Optional<Evidencia> buscarConAlerta(@Param("id") Long id);

	/** Las que cuentan para el máximo por alerta: todas menos las descartadas. */
	@Query("select count(e) from Evidencia e where e.alerta.id = :alertaId and e.estado <> 'DESCARTADA'")
	long contarVigentesPorAlerta(@Param("alertaId") Long alertaId);

}
