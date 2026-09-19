package com.uem.ambulancias.comun.web;

import com.uem.ambulancias.seguridad.service.CredencialesInvalidasException;

import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Resuelve los parámetros anotados con {@link UsuarioActual} leyendo el token ya verificado. Si no hay token válido
 * no se llega hasta acá, porque la cadena de seguridad corta antes; la comprobación queda igual por si alguien
 * anotara un endpoint abierto.
 */
@Component
public class UsuarioActualResolver implements HandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parametro) {
		return parametro.hasParameterAnnotation(UsuarioActual.class) && Long.class.equals(parametro.getParameterType());
	}

	@Override
	public Object resolveArgument(MethodParameter parametro, ModelAndViewContainer modelo, NativeWebRequest peticion,
			WebDataBinderFactory fabrica) {
		Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
		if (autenticacion == null || !(autenticacion.getPrincipal() instanceof Jwt token)) {
			throw new CredencialesInvalidasException();
		}
		return Long.valueOf(token.getSubject());
	}

}
