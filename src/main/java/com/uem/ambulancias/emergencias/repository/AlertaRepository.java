package com.uem.ambulancias.emergencias.repository;

import com.uem.ambulancias.emergencias.domain.Alerta;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertaRepository extends JpaRepository<Alerta, Long> {
}
