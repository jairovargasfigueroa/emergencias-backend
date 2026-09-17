package com.uem.ambulancias.emergencias.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.uem.ambulancias.emergencias.domain.Atencion;
import com.uem.ambulancias.emergencias.domain.EstadoAtencion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AtencionRepository extends JpaRepository<Atencion, Long> {

	/** SEC-B.1: hay al menos una atención EN_CAMINO, EN_EL_LUGAR o PACIENTE_RECOGIDO en el incidente. */
	default boolean existeActivaPorIncidente(Long idIncidente) {
		return existsByIncidenteIdAndEstadoIn(idIncidente, EstadoAtencion.ACTIVOS);
	}

	/** Alguna unidad del incidente ya llegó al lugar: está EN_EL_LUGAR o con el paciente recogido. */
	default boolean existeEnEscenaPorIncidente(Long idIncidente) {
		return existsByIncidenteIdAndEstadoIn(idIncidente, EstadoAtencion.EN_ESCENA);
	}

	/** Atenciones activas del incidente con su ambulancia, en orden de toma. */
	default List<Atencion> buscarActivasPorIncidente(Long idIncidente) {
		return buscarPorIncidenteYEstados(idIncidente, EstadoAtencion.ACTIVOS);
	}

	/** La atención activa de la ambulancia. ME-1 garantiza que hay a lo sumo una. */
	default Optional<Atencion> buscarActivaPorAmbulancia(Long ambulanciaId) {
		return buscarPorAmbulanciaYEstados(ambulanciaId, EstadoAtencion.ACTIVOS).stream().findFirst();
	}

	boolean existsByIncidenteIdAndEstadoIn(Long incidenteId, Collection<EstadoAtencion> estados);

	boolean existsByIncidenteIdAndEstado(Long incidenteId, EstadoAtencion estado);

	@Query("select a.incidente.id from Atencion a where a.id = :id")
	Optional<Long> buscarIncidenteId(@Param("id") Long id);

	@Query("""
			select a from Atencion a join fetch a.ambulancia
			where a.incidente.id = :incidenteId and a.estado in :estados
			order by a.horaToma
			""")
	List<Atencion> buscarPorIncidenteYEstados(@Param("incidenteId") Long incidenteId,
			@Param("estados") Collection<EstadoAtencion> estados);

	@Query("""
			select a from Atencion a join fetch a.ambulancia
			where a.ambulancia.id = :ambulanciaId and a.estado in :estados
			order by a.horaToma desc
			""")
	List<Atencion> buscarPorAmbulanciaYEstados(@Param("ambulanciaId") Long ambulanciaId,
			@Param("estados") Collection<EstadoAtencion> estados);

	/** Todas las atenciones de esos incidentes, con su ambulancia y su centro de salud, en orden de toma. */
	@Query("""
			select a from Atencion a join fetch a.ambulancia left join fetch a.centroSalud
			where a.incidente.id in :incidenteIds
			order by a.horaToma, a.id
			""")
	List<Atencion> buscarPorIncidentes(@Param("incidenteIds") Collection<Long> incidenteIds);

}
