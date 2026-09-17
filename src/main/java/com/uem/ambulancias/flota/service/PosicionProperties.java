package com.uem.ambulancias.flota.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Frecuencia con la que se guarda la última posición de cada ambulancia ({@code sga.posicion.persistir-cada-seg}).
 * La posición en vivo se publica siempre; en la base solo se guarda una foto reciente para la consulta de cercanía.
 */
@ConfigurationProperties(prefix = "sga.posicion")
public record PosicionProperties(@DefaultValue("30") int persistirCadaSeg) {
}
