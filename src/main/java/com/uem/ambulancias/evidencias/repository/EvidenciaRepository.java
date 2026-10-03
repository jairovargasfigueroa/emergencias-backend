package com.uem.ambulancias.evidencias.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.uem.ambulancias.evidencias.domain.Evidencia;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
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

	/** Las evidencias del incidente cuyo archivo llegó al almacén, en el orden en que llegaron. */
	@Query("""
			select e from Evidencia e join fetch e.alerta a
			where a.incidente.id = :incidenteId and e.estado in ('SUBIDA', 'ANALIZADA', 'FALLIDA')
			order by e.subidaEn, e.id
			""")
	List<Evidencia> buscarSubidasPorIncidente(@Param("incidenteId") Long incidenteId);

	/** Las que cuentan para el máximo por alerta: todas menos las descartadas. */
	@Query("select count(e) from Evidencia e where e.alerta.id = :alertaId and e.estado <> 'DESCARTADA'")
	long contarVigentesPorAlerta(@Param("alertaId") Long alertaId);

	/**
	 * Las que se firmaron antes de {@code limite} y nunca se confirmaron, con su fila bloqueada: si la app confirma
	 * justo en ese momento, una de las dos espera a la otra y no queda una evidencia subida y descartada a la vez.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select e from Evidencia e where e.estado = 'PENDIENTE_SUBIDA' and e.registradaEn < :limite")
	List<Evidencia> buscarPendientesRegistradasAntesDe(@Param("limite") Instant limite);

	/**
	 * Descarta de una vez las registradas antes de {@code limite}. Es una actualización masiva y no una por una porque
	 * no hay nada que decidir: vencida la retención, se descartan todas. Los archivos los borra S3 por su cuenta.
	 */
	@Modifying
	@Query("update Evidencia e set e.estado = 'DESCARTADA' where e.estado <> 'DESCARTADA' and e.registradaEn < :limite")
	int descartarRegistradasAntesDe(@Param("limite") Instant limite);

	/** Las que cuentan para el máximo por incidente: las de todas sus alertas, menos las descartadas. */
	@Query("select count(e) from Evidencia e where e.alerta.incidente.id = :incidenteId and e.estado <> 'DESCARTADA'")
	long contarVigentesPorIncidente(@Param("incidenteId") Long incidenteId);

}
