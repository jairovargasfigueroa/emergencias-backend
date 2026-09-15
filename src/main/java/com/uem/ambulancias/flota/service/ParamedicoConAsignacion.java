package com.uem.ambulancias.flota.service;

import com.uem.ambulancias.flota.domain.Asignacion;
import com.uem.ambulancias.usuarios.domain.Usuario;

/**
 * Un paramédico junto a su asignación vigente ({@code null} si no tiene).
 */
public record ParamedicoConAsignacion(Usuario paramedico, Asignacion asignacionVigente) {
}
