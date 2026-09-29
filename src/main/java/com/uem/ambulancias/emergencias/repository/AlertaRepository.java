package com.uem.ambulancias.emergencias.repository;

import java.util.Collection;
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

	/**
	 * Si al incidente le queda algún pedido en pie. Cuando todos los emisores retiraron el suyo, la unidad que va en
	 * camino tiene que saberlo, y si no va ninguna el incidente ya no tiene por qué seguir abierto.
	 */
	@Query("select count(a) > 0 from Alerta a where a.incidente.id = :incidenteId and a.estado <> 'CANCELADA'")
	boolean existeAlgunaVigente(@Param("incidenteId") Long incidenteId);

	/**
	 * A quiénes avisarles de un incidente: los que pidieron la ambulancia, no retiraron su pedido y tienen un teléfono
	 * registrado. Cada uno con su alerta, porque en un incidente puede haber avisos de varias personas.
	 */
	@Query("""
			select new com.uem.ambulancias.emergencias.repository.EmisorConAviso(a.id, e.tokenPush)
			from Alerta a join a.emisor e
			where a.incidente.id = :incidenteId and a.estado <> 'CANCELADA'
			  and e.activo = true and e.tokenPush is not null
			""")
	List<EmisorConAviso> buscarEmisoresConAviso(@Param("incidenteId") Long incidenteId);

	/** Descripciones que dejaron los emisores del incidente, en orden de emisión. */
	@Query("""
			select a.descripcion from Alerta a
			where a.incidente.id = :incidenteId and a.descripcion is not null
			order by a.fechaHora
			""")
	List<String> buscarDescripciones(@Param("incidenteId") Long incidenteId);

	/** Alertas del incidente con su emisor, en orden de emisión. */
	@Query("""
			select a from Alerta a join fetch a.emisor
			where a.incidente.id = :incidenteId
			order by a.fechaHora, a.id
			""")
	List<Alerta> buscarPorIncidente(@Param("incidenteId") Long incidenteId);

	/**
	 * Lo mismo para varios incidentes de una vez, en orden de emisión. Un incidente no tiene nombre propio: lo
	 * único que lo hace reconocible en una lista es lo que escribió el que avisó, y es la primera descripción la
	 * que vale, porque la escribió quien vio el hecho primero.
	 */
	@Query("""
			select new com.uem.ambulancias.emergencias.repository.ReferenciaDeIncidente(a.incidente.id, a.descripcion)
			from Alerta a
			where a.incidente.id in :incidenteIds and a.descripcion is not null
			order by a.fechaHora, a.id
			""")
	List<ReferenciaDeIncidente> buscarDescripcionesPorIncidentes(@Param("incidenteIds") Collection<Long> incidenteIds);

	/** Cantidad de alertas de cada uno de esos incidentes, en una sola consulta agrupada. */
	@Query("""
			select new com.uem.ambulancias.emergencias.repository.AlertasPorIncidente(a.incidente.id, count(a))
			from Alerta a
			where a.incidente.id in :incidenteIds
			group by a.incidente.id
			""")
	List<AlertasPorIncidente> contarPorIncidentes(@Param("incidenteIds") Collection<Long> incidenteIds);

}
