package com.uem.ambulancias.comun.config;

import java.util.List;

import com.uem.ambulancias.comun.web.RolActualResolver;
import com.uem.ambulancias.comun.web.UsuarioActualResolver;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registra los resolvedores que inyectan el id y el rol del usuario autenticado en los controladores.
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

	private final UsuarioActualResolver usuarioActualResolver;
	private final RolActualResolver rolActualResolver;

	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvedores) {
		resolvedores.add(usuarioActualResolver);
		resolvedores.add(rolActualResolver);
	}

}
