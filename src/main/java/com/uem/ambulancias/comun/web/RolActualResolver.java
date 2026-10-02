package com.uem.ambulancias.comun.web;

import com.uem.ambulancias.seguridad.service.CredencialesInvalidasException;
import com.uem.ambulancias.seguridad.service.TokenService;
import com.uem.ambulancias.usuarios.domain.RolUsuario;

import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/** Resuelve los parámetros anotados con {@link RolActual} leyendo el rol del token ya verificado. */
@Component
public class RolActualResolver implements HandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parametro) {
		return parametro.hasParameterAnnotation(RolActual.class)
				&& RolUsuario.class.equals(parametro.getParameterType());
	}

	@Override
	public Object resolveArgument(MethodParameter parametro, ModelAndViewContainer modelo, NativeWebRequest peticion,
			WebDataBinderFactory fabrica) {
		Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
		if (autenticacion == null || !(autenticacion.getPrincipal() instanceof Jwt token)
				|| token.getClaimAsString(TokenService.CLAVE_ROL) == null) {
			throw new CredencialesInvalidasException();
		}
		return RolUsuario.valueOf(token.getClaimAsString(TokenService.CLAVE_ROL));
	}

}
