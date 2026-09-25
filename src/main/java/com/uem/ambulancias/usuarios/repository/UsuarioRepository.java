package com.uem.ambulancias.usuarios.repository;

import java.util.List;
import java.util.Optional;

import com.uem.ambulancias.usuarios.domain.RolUsuario;
import com.uem.ambulancias.usuarios.domain.Usuario;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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
	 * El teléfono identifica a un usuario solo entre los que se registraron solos. Los dependientes suelen llevar
	 * el número de quien los cargó, así que si no se excluyeran, registrar a la mamá devolvería a la hija.
	 */
	Optional<Usuario> findFirstByTelefonoAndRolAndRegistradoPorIsNullOrderByIdAsc(String telefono, RolUsuario rol);

	Optional<Usuario> findFirstByTelefonoAndRolAndActivoTrueAndRegistradoPorIsNullOrderByIdAsc(String telefono,
			RolUsuario rol);

	/** Las personas de un ciudadano: a quienes traslada y sus contactos de confianza. */
	List<Usuario> findByRegistradoPorIdAndActivoTrueOrderByNombreCompletoAsc(Long registradoPorId);

	/** Para que nadie use como pasajero a una persona que registró otro. */
	boolean existsByIdAndRegistradoPorId(Long id, Long registradoPorId);

	/** Lectura con bloqueo pesimista: serializa las operaciones sobre el mismo usuario. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select u from Usuario u where u.id = :id and u.rol = :rol")
	Optional<Usuario> buscarParaActualizar(@Param("id") Long id, @Param("rol") RolUsuario rol);

}
