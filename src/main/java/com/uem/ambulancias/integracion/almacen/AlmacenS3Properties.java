package com.uem.ambulancias.integracion.almacen;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * El bucket de S3 donde viven las evidencias ({@code sga.evidencias.s3.*}). Sin bucket, las evidencias se rechazan con
 * un error claro. Las credenciales no van acá: las da el rol de la instancia, o el entorno en una máquina de
 * desarrollo. Cuánto sirven las URLs se configura en {@code sga.evidencias.minutos-*}.
 */
@ConfigurationProperties(prefix = "sga.evidencias.s3")
public record AlmacenS3Properties(

		/** Nombre del bucket, privado y sin acceso público. */
		String bucket,

		/** Región del bucket, por ejemplo us-east-1. Las URLs firmadas apuntan a esa región. */
		String region) {
}
