package com.uem.ambulancias.emergencias.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.uem.ambulancias.emergencias.domain.Atencion;
import com.uem.ambulancias.emergencias.domain.EstadoAtencion;
import com.uem.ambulancias.emergencias.domain.MotivoSinTraslado;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AtencionRepository extends JpaRepository<Atencion, Long> {

	/** SEC-B.1: hay al menos una atención EN_CAMINO, EN_EL_LUGAR o PACIENTE_RECOGIDO en el incidente. */
	default boolean existeActivaPorIncidente(Long idIncidente) {
		return existsByIncidenteIdAndEstadoIn(idIncidente, EstadoAtencion.ACTIVOS);
	}

	/**
	 * Si alguna unidad del incidente llegó al lugar alguna vez. Se pregunta por el hito, que queda congelado, y no
	 * por el estado, que sigue avanzando: la unidad que ya está en el hospital, o la que canceló después de llegar,
	 * igual estuvo ahí y vio lo que había.
	 */
	@Query("select count(a) > 0 from Atencion a where a.incidente.id = :incidenteId and a.horaLlegada is not null")
	boolean existeLlegadaPorIncidente(@Param("incidenteId") Long incidenteId);

	/** Atenciones activas del incidente con su ambulancia, en orden de toma. */
	default List<Atencion> buscarActivasPorIncidente(Long idIncidente) {
		return buscarPorIncidenteYEstados(idIncidente, EstadoAtencion.ACTIVOS);
	}

	/** La atención activa de la ambulancia. ME-1 garantiza que hay a lo sumo una. */
	default Optional<Atencion> buscarActivaPorAmbulancia(Long ambulanciaId) {
		return buscarPorAmbulanciaYEstados(ambulanciaId, EstadoAtencion.ACTIVOS).stream().findFirst();
	}

	/**
	 * Lo mismo para varias unidades de una vez. El centro de control se refresca solo cada pocos segundos: una
	 * consulta por unidad sería la misma pregunta multiplicada por el tamaño de la flota, cada vez.
	 */
	default List<Atencion> buscarActivasPorAmbulancias(Collection<Long> ambulanciaIds) {
		return buscarPorAmbulanciasYEstados(ambulanciaIds, EstadoAtencion.ACTIVOS);
	}

	/**
	 * Las atenciones que tienen tomadas a esas unidades: en curso, o ya resueltas y sin liberar. Es el mismo
	 * criterio que deja a la ambulancia en EN_ATENCION, asi que preguntando por aca nunca aparece una unidad
	 * ocupada sin nada que mostrar.
	 */
	default List<Atencion> buscarQueOcupanAmbulancias(Collection<Long> ambulanciaIds) {
		return buscarOcupandoAmbulancias(ambulanciaIds, EstadoAtencion.ACTIVOS, EstadoAtencion.RESUELTOS);
	}

	/** La atención que tiene tomada a la ambulancia: la que está en curso, o una ya resuelta que no se liberó. */
	default Optional<Atencion> buscarQueOcupaAmbulancia(Long ambulanciaId) {
		return buscarOcupandoAmbulancia(ambulanciaId, EstadoAtencion.ACTIVOS, EstadoAtencion.RESUELTOS).stream()
				.findFirst();
	}

	@Query("""
			select a from Atencion a join fetch a.ambulancia left join fetch a.centroSalud
			left join fetch a.traslado t left join fetch t.pasajero left join fetch t.centroSaludDestino
			where a.ambulancia.id = :ambulanciaId
			  and (a.estado in :activos or (a.estado in :resueltos and a.horaLiberacion is null))
			order by a.horaToma desc
			""")
	List<Atencion> buscarOcupandoAmbulancia(@Param("ambulanciaId") Long ambulanciaId,
			@Param("activos") Collection<EstadoAtencion> activos,
			@Param("resueltos") Collection<EstadoAtencion> resueltos);

	/** Cómo terminaron las salidas del incidente que no trasladaron a nadie: de ahí sale el desenlace del incidente. */
	@Query("select a.motivoSinTraslado from Atencion a where a.incidente.id = :incidenteId and a.estado = 'SIN_TRASLADO'")
	List<MotivoSinTraslado> buscarMotivosSinTraslado(@Param("incidenteId") Long incidenteId);

	boolean existsByIncidenteIdAndEstadoIn(Long incidenteId, Collection<EstadoAtencion> estados);

	boolean existsByIncidenteIdAndEstado(Long incidenteId, EstadoAtencion estado);

	/**
	 * Las unidades que ya rechazaron este traslado. Sin esto, el barrido le vuelve a ofrecer el mismo traslado a
	 * la misma unidad —que suele ser la más cercana— y el rechazo entra en bucle hasta que el pedido se vence.
	 */
	@Query("""
			select a.ambulancia.id from Atencion a
			where a.traslado.id = :trasladoId and a.motivoCancelacion = 'RECHAZADA_POR_PARAMEDICO'
			""")
	List<Long> buscarAmbulanciasQueRechazaron(@Param("trasladoId") Long trasladoId);

	/** Las atenciones vivas de varios traslados, en una sola consulta: es lo que la tabla del panel necesita. */
	@Query("""
			select a from Atencion a join fetch a.ambulancia left join fetch a.paramedicoResponsable
			where a.traslado.id in :trasladoIds and a.horaCancelacion is null
			""")
	List<Atencion> buscarPorTraslados(@Param("trasladoIds") Collection<Long> trasladoIds);

	/** Los traslados que hizo un paramédico, del más reciente al más viejo. Es su historial en la app. */
	@Query("""
			select a from Atencion a join fetch a.ambulancia
			join fetch a.traslado t left join fetch t.pasajero left join fetch t.centroSaludDestino
			where a.paramedicoResponsable.id = :paramedicoId and a.traslado is not null
			order by a.horaToma desc
			""")
	List<Atencion> buscarTrasladosDeParamedico(@Param("paramedicoId") Long paramedicoId);

	/** La atención en curso de un traslado, para cuando el solicitante lo cancela con la unidad ya en camino. */
	@Query("""
			select a from Atencion a
			where a.traslado.id = :trasladoId
			  and a.estado in ('EN_CAMINO', 'EN_EL_LUGAR', 'PACIENTE_RECOGIDO', 'EN_HOSPITAL')
			""")
	Optional<Atencion> buscarActivaPorTraslado(@Param("trasladoId") Long trasladoId);

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

	/**
	 * Se traen también el incidente y el traslado: las entidades mapean el padre como asociación y no como id
	 * suelto, así que sin esto pedirle el id a cada uno despierta su proxy y cae una consulta por atención.
	 */
	@Query("""
			select a from Atencion a join fetch a.ambulancia
			left join fetch a.incidente left join fetch a.traslado
			where a.ambulancia.id in :ambulanciaIds and a.estado in :estados
			order by a.horaToma desc
			""")
	List<Atencion> buscarPorAmbulanciasYEstados(@Param("ambulanciaIds") Collection<Long> ambulanciaIds,
			@Param("estados") Collection<EstadoAtencion> estados);

	@Query("""
			select a from Atencion a join fetch a.ambulancia
			left join fetch a.incidente left join fetch a.traslado
			where a.ambulancia.id in :ambulanciaIds
			  and (a.estado in :activos or (a.estado in :resueltos and a.horaLiberacion is null))
			order by a.horaToma desc
			""")
	List<Atencion> buscarOcupandoAmbulancias(@Param("ambulanciaIds") Collection<Long> ambulanciaIds,
			@Param("activos") Collection<EstadoAtencion> activos,
			@Param("resueltos") Collection<EstadoAtencion> resueltos);

	/**
	 * Las atenciones que tuvieron algún hito dentro de la ventana. Es la bitácora del centro de control: no hay
	 * tabla de eventos porque cada hito ya quedó congelado en su columna, así que se pregunta por las columnas y
	 * la lista de eventos se arma en memoria.
	 *
	 * <p>Se miran todas y no solo {@code horaToma}: una unidad que salió anteayer y recién ahora entregó tiene
	 * novedades de hoy aunque su atención sea vieja.
	 *
	 * <p>Viene todo lo que cada evento nombra —unidad, destino y de qué cuelga la atención— porque si no, armar
	 * la lista dispara una consulta por evento.
	 */
	@Query("""
			select a from Atencion a join fetch a.ambulancia left join fetch a.centroSalud
			left join fetch a.incidente left join fetch a.traslado
			where a.horaToma >= :desde or a.horaLlegada >= :desde or a.horaRecogida >= :desde
			   or a.horaLlegadaHospital >= :desde or a.horaEntrega >= :desde or a.horaSinTraslado >= :desde
			   or a.horaLiberacion >= :desde or a.horaCancelacion >= :desde or a.horaAvisoNoListo >= :desde
			""")
	List<Atencion> buscarConHitosDesde(@Param("desde") Instant desde);

	/**
	 * Todas las atenciones de esos incidentes, con su ambulancia, su paramédico responsable y su centro de salud, en
	 * orden de toma.
	 */
	@Query("""
			select a from Atencion a join fetch a.ambulancia left join fetch a.centroSalud
			left join fetch a.paramedicoResponsable
			where a.incidente.id in :incidenteIds
			order by a.horaToma, a.id
			""")
	List<Atencion> buscarPorIncidentes(@Param("incidenteIds") Collection<Long> incidenteIds);

}
