package com.uem.ambulancias.flota.repository;

import java.util.List;
import java.util.Optional;

import com.uem.ambulancias.flota.domain.Turno;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TurnoRepository extends JpaRepository<Turno, Long> {

	/** El turno abierto del paramédico. Solo puede haber uno: iniciar otro se rechaza. */
	@Query("select t from Turno t join fetch t.ambulancia where t.paramedico.id = :paramedicoId and t.fin is null")
	Optional<Turno> buscarAbiertoPorParamedico(@Param("paramedicoId") Long paramedicoId);

	/** Cuántos siguen adentro de la unidad. La ambulancia cae a SIN_TURNO recién cuando sale el último. */
	@Query("select count(t) from Turno t where t.ambulancia.id = :ambulanciaId and t.fin is null")
	long contarAbiertosPorAmbulancia(@Param("ambulanciaId") Long ambulanciaId);

	/** Quiénes están de turno en cada una de esas unidades, para que el panel diga quién está adentro. */
	@Query("""
			select t from Turno t join fetch t.paramedico
			where t.ambulancia.id in :ambulanciaIds and t.fin is null
			order by t.inicio
			""")
	List<Turno> buscarAbiertosPorAmbulancias(@Param("ambulanciaIds") List<Long> ambulanciaIds);

}
