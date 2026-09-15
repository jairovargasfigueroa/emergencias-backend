package com.uem.ambulancias.emergencias.repository;

import java.util.List;

import com.uem.ambulancias.emergencias.domain.Alerta;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AlertaRepository extends JpaRepository<Alerta, Long> {

	/** Descripciones que dejaron los emisores del incidente, en orden de emisión. */
	@Query("""
			select a.descripcion from Alerta a
			where a.incidente.id = :incidenteId and a.descripcion is not null
			order by a.fechaHora
			""")
	List<String> buscarDescripciones(@Param("incidenteId") Long incidenteId);

}
