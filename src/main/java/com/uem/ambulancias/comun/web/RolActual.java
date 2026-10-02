package com.uem.ambulancias.comun.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Rol del usuario que hace la petición, sacado de su token. Para los endpoints que comparten el panel y la app del
 * paramédico pero no muestran lo mismo a los dos.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface RolActual {
}
