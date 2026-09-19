package com.uem.ambulancias.comun.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Id del usuario que hace la petición, sacado de su token. Reemplaza a la cabecera que antes cada cliente escribía a
 * mano: ahora el dato no viaja en la petición, se deduce de la firma.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface UsuarioActual {
}
