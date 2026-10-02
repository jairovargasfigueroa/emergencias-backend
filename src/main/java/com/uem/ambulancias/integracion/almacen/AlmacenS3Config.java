package com.uem.ambulancias.integracion.almacen;

import com.uem.ambulancias.evidencias.service.AlmacenDeEvidencias;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * Con un bucket configurado, las evidencias van a S3. Las credenciales salen de la cadena por defecto de AWS: en el
 * servidor, el rol de la instancia EC2; nunca claves escritas en un archivo del proyecto.
 */
@Configuration
@ConditionalOnExpression("!'${sga.evidencias.s3.bucket:}'.isBlank()")
public class AlmacenS3Config {

	/** Con bucket y sin región, el servidor no arranca: firmaría URLs de otra región que S3 rechaza. */
	@Bean
	Region regionS3(AlmacenS3Properties propiedades) {
		if (propiedades.region() == null || propiedades.region().isBlank()) {
			throw new IllegalStateException("sga.evidencias.s3.region es obligatoria con sga.evidencias.s3.bucket.");
		}
		return Region.of(propiedades.region().trim());
	}

	@Bean
	AwsCredentialsProvider credencialesAws() {
		return DefaultCredentialsProvider.builder().build();
	}

	@Bean
	S3Client clienteS3(Region regionS3, AwsCredentialsProvider credencialesAws) {
		return S3Client.builder()
				.region(regionS3)
				.credentialsProvider(credencialesAws)
				.build();
	}

	@Bean
	S3Presigner firmanteS3(Region regionS3, AwsCredentialsProvider credencialesAws) {
		return S3Presigner.builder()
				.region(regionS3)
				.credentialsProvider(credencialesAws)
				.build();
	}

	@Bean
	AlmacenDeEvidencias almacenS3(S3Client clienteS3, S3Presigner firmanteS3, AlmacenS3Properties propiedades) {
		return new AlmacenS3(clienteS3, firmanteS3, propiedades.bucket().trim());
	}

}
