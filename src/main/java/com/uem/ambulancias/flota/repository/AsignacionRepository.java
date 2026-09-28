package com.uem.ambulancias.flota.repository;

import java.util.List;
import java.util.Optional;

import com.uem.ambulancias.flota.domain.Asignacion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AsignacionRepository extends JpaRepository<Asignacion, Long> {

	@Query("""
			select a from Asignacion a join fetch a.ambulancia
			where a.paramedico.id = :paramedicoId and a.fechaFin is null
			""")
	Optional<Asignacion> buscarVigentePorParamedico(@Param("paramedicoId") Long paramedicoId);

	@Query("select a from Asignacion a join fetch a.ambulancia where a.fechaFin is null")
	List<Asignacion> buscarVigentes();

	@Query("""
			select a from Asignacion a join fetch a.ambulancia join fetch a.paramedico
			where a.paramedico.id = :paramedicoId
			order by a.fechaInicio desc
			""")
	List<Asignacion> buscarPorParamedico(@Param("paramedicoId") Long paramedicoId);

	@Query("""
			select a from Asignacion a join fetch a.ambulancia join fetch a.paramedico
			where a.ambulancia.id = :ambulanciaId
			order by a.fechaInicio desc
			""")
	List<Asignacion> buscarPorAmbulancia(@Param("ambulanciaId") Long ambulanciaId);

}
