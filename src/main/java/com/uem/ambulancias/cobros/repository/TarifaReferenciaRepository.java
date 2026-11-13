package com.uem.ambulancias.cobros.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.uem.ambulancias.cobros.domain.TarifaReferencia;
import com.uem.ambulancias.cobros.domain.TipoServicio;

public interface TarifaReferenciaRepository extends JpaRepository<TarifaReferencia, Long> {

	/** Todas, las que empiezan más tarde primero; a igual vigencia, la registrada después. */
	@Query("""
			select t from TarifaReferencia t join fetch t.registradaPor
			order by t.vigenteDesde desc, t.id desc
			""")
	List<TarifaReferencia> buscarHistorial();

	/** La que rige ese día: la primera de esta lista. */
	@Query("""
			select t from TarifaReferencia t join fetch t.registradaPor
			where t.tipoServicio = :tipo and t.vigenteDesde <= :dia
			order by t.vigenteDesde desc, t.id desc
			""")
	List<TarifaReferencia> buscarVigentesAl(@Param("tipo") TipoServicio tipo, @Param("dia") LocalDate dia);

}
