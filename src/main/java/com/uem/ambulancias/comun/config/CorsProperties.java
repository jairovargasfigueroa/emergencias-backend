package com.uem.ambulancias.comun.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Orígenes del navegador que pueden llamar a la API ({@code sga.cors.origenes-permitidos}), por ejemplo el dominio
 * del panel web. En local queda vacío porque el panel usa el proxy de Vite. Las apps móviles no pasan por CORS.
 */
@ConfigurationProperties(prefix = "sga.cors")
public record CorsProperties(@DefaultValue List<String> origenesPermitidos) {
}
