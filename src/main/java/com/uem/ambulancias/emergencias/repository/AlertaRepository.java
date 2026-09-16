package com.uem.ambulancias.emergencias.repository;

import java.util.List;
import java.util.Optional;

import com.uem.ambulancias.emergencias.domain.Alerta;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AlertaRepository extends JpaRepository<Alerta, Long> {

	/** Incidente al que está vinculada la alerta: se bloquea antes de tocar nada. */
	@Query("select a.incidente.id from Alerta a where a.id = :id")
	Optional<Long> buscarIncidenteId(@Param("id") Long id);

	/** Descripciones que dejaron los emisores del incidente, en orden de emisión. */
	@Query("""
			select a.descripcion from Alerta a
			where a.incidente.id = :incidenteId and a.descripcion is not null
			order by a.fechaHora
			""")
	List<String> buscarDescripciones(@Param("incidenteId") Long incidenteId);

}
