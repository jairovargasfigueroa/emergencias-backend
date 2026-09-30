package com.uem.ambulancias.usuarios.repository;

import java.util.List;
import java.util.Optional;

import com.uem.ambulancias.usuarios.domain.RolUsuario;
import com.uem.ambulancias.usuarios.domain.Usuario;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

	List<Usuario> findByRolOrderByNombreCompletoAsc(RolUsuario rol);

	Optional<Usuario> findByIdAndRol(Long id, RolUsuario rol);

	/** Entrada al panel: el correo identifica al administrador. */
	Optional<Usuario> findByCorreoAndRol(String correo, RolUsuario rol);

	/** Si ya hay un administrador, no se siembra el inicial. */
	boolean existsByRol(RolUsuario rol);

	/**
	 * La cuenta de ciudadano de ese número, la más antigua si hubiera más de una. Solo entre los que se registraron
	 * solos: los dependientes suelen llevar el número de quien los cargó, y si no se excluyeran, la hija entraría a
	 * la cuenta de su mamá.
	 *
	 * <p>Los teléfonos de antes se guardaron como la persona los escribió —con espacios, guiones o el +591
	 * adelante—, así que se comparan normalizados igual que el número verificado: solo dígitos y sin el 591 de un
	 * número boliviano completo. No se reescriben en la base: el índice único de teléfono y rol haría chocar a dos
	 * cuentas que hoy conviven con formatos distintos.
	 */
	@Query(value = """
			select u.* from usuario u
			where u.rol = 'CIUDADANO' and u.registrado_por_id is null
			  and case
			        when regexp_replace(u.telefono, '[^0-9]', '', 'g') ~ '^591[0-9]{8}$'
			          then substr(regexp_replace(u.telefono, '[^0-9]', '', 'g'), 4)
			        else regexp_replace(u.telefono, '[^0-9]', '', 'g')
			      end = :telefonoNacional
			order by u.id
			limit 1
			""", nativeQuery = true)
	Optional<Usuario> buscarCiudadanoPorTelefonoNacional(@Param("telefonoNacional") String telefonoNacional);

	/**
	 * Si ese número ya identifica a alguien de ese rol. Mira exactamente lo mismo que el ingreso a la app: solo
	 * cuentas propias y activas. Un dependiente no cuenta porque no inicia sesión, y una persona dada de baja
	 * tampoco, así que su número queda libre para quien venga después.
	 */
	boolean existsByTelefonoAndRolAndActivoTrueAndRegistradoPorIsNull(String telefono, RolUsuario rol);

	/** Lo mismo, sin contarse a sí mismo: al editar, dejar el teléfono como estaba no puede ser un conflicto. */
	boolean existsByTelefonoAndRolAndActivoTrueAndRegistradoPorIsNullAndIdNot(String telefono, RolUsuario rol, Long id);

	/**
	 * Quién entra a la app con ese teléfono: cuentas propias y activas de ese rol. Devuelve solo los ids porque
	 * quien lo busca bloquea después la fila, y si la entidad ya estuviera cargada, Hibernate le devolvería esta
	 * copia sin releerla y se le escaparía un intento contado a la vez por otra petición.
	 */
	@Query("""
			select u.id from Usuario u
			where u.telefono = :telefono and u.rol = :rol and u.activo = true and u.registradoPor is null
			order by u.id
			""")
	List<Long> buscarIdsActivosPorTelefono(@Param("telefono") String telefono, @Param("rol") RolUsuario rol);

	/** Las personas de un ciudadano: a quienes traslada y sus contactos de confianza. */
	List<Usuario> findByRegistradoPorIdAndActivoTrueOrderByNombreCompletoAsc(Long registradoPorId);

	/** Para que nadie use como pasajero a una persona que registró otro. */
	boolean existsByIdAndRegistradoPorId(Long id, Long registradoPorId);

	/** Lectura con bloqueo pesimista: serializa las operaciones sobre el mismo usuario. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select u from Usuario u where u.id = :id and u.rol = :rol")
	Optional<Usuario> buscarParaActualizar(@Param("id") Long id, @Param("rol") RolUsuario rol);

	/**
	 * Le quita ese token de push a cualquier otra cuenta que lo tenga. El token es del teléfono, no de la persona: si
	 * Beto entra en el teléfono que usaba Ana, los avisos de Ana no pueden seguir llegando ahí.
	 */
	@Modifying
	@Query("update Usuario u set u.tokenPush = null where u.tokenPush = :tokenPush and u.id <> :usuarioId")
	int liberarTokenPush(@Param("tokenPush") String tokenPush, @Param("usuarioId") Long usuarioId);

}
