package com.uem.ambulancias.evidencias.repository;

import java.util.List;

import com.uem.ambulancias.evidencias.domain.AnalisisEvidencia;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AnalisisEvidenciaRepository extends JpaRepository<AnalisisEvidencia, Long> {

	boolean existsByEvidenciaId(Long evidenciaId);

	/** Los análisis vigentes del incidente, con su evidencia y su alerta, en el orden en que llegaron los archivos. */
	@Query("""
			select a from AnalisisEvidencia a join fetch a.evidencia e join fetch e.alerta al
			where al.incidente.id = :incidenteId and e.estado = 'ANALIZADA'
			order by e.subidaEn, e.id
			""")
	List<AnalisisEvidencia> buscarVigentesPorIncidente(@Param("incidenteId") Long incidenteId);

	/** Si alguna evidencia del incidente ya tiene análisis: sin ninguno, no hay resumen que rehacer. */
	@Query("select count(a) > 0 from AnalisisEvidencia a where a.evidencia.alerta.incidente.id = :incidenteId")
	boolean existenEnIncidente(@Param("incidenteId") Long incidenteId);

}
