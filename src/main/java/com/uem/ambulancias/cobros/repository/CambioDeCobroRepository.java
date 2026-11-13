package com.uem.ambulancias.cobros.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.uem.ambulancias.cobros.domain.CambioDeCobro;

public interface CambioDeCobroRepository extends JpaRepository<CambioDeCobro, Long> {

	/** Lo más reciente primero. */
	@Query("""
			select c from CambioDeCobro c join fetch c.responsable
			where c.cobro.id = :cobroId
			order by c.fechaHora desc, c.id desc
			""")
	List<CambioDeCobro> buscarPorCobro(@Param("cobroId") Long cobroId);

}
