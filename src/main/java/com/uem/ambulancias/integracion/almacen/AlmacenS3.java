package com.uem.ambulancias.integracion.almacen;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import com.uem.ambulancias.comun.error.CodigoError;
import com.uem.ambulancias.comun.error.ConflictoException;
import com.uem.ambulancias.evidencias.service.AlmacenDeEvidencias;
import com.uem.ambulancias.evidencias.service.LecturaFirmada;
import com.uem.ambulancias.evidencias.service.ObjetoAlmacenado;
import com.uem.ambulancias.evidencias.service.SubidaFirmada;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ChecksumMode;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

/**
 * Las evidencias en un bucket de S3. Firmar no llama a S3: la URL se arma y se firma acá. Solo verificar y borrar van
 * a S3, y si S3 no responde se avisa que el almacén no está disponible para que la app reintente.
 */
@Slf4j
public class AlmacenS3 implements AlmacenDeEvidencias {

	private final S3Client s3;
	private final S3Presigner firmante;
	private final String bucket;

	public AlmacenS3(S3Client s3, S3Presigner firmante, String bucket) {
		this.s3 = s3;
		this.firmante = firmante;
		this.bucket = bucket;
	}

	/**
	 * La firma cubre el tipo, el tamaño y el SHA-256: S3 rechaza el PUT si la app manda otra cabecera, y también si el
	 * cuerpo no da ese SHA-256. Se devuelven las cabeceras que quedaron firmadas, menos {@code host}, que la pone el
	 * cliente HTTP.
	 */
	@Override
	public SubidaFirmada firmarSubida(String claveObjeto, String mimeType, long tamanoBytes, String sha256Base64,
			Duration vigencia) {
		PutObjectRequest subida = PutObjectRequest.builder()
				.bucket(bucket)
				.key(claveObjeto)
				.contentType(mimeType)
				.contentLength(tamanoBytes)
				.checksumSHA256(sha256Base64)
				.build();
		PresignedPutObjectRequest firmada = firmante.presignPutObject(pedido -> pedido
				.signatureDuration(vigencia)
				.putObjectRequest(subida));

		Map<String, String> cabeceras = new LinkedHashMap<>();
		firmada.signedHeaders().forEach((nombre, valores) -> {
			if (!"host".equalsIgnoreCase(nombre)) {
				cabeceras.put(nombre, String.join(",", valores));
			}
		});
		return new SubidaFirmada(firmada.url().toString(), cabeceras, firmada.expiration());
	}

	/** Con el modo de checksum encendido, S3 informa el SHA-256 que calculó al recibir el archivo. */
	@Override
	public Optional<ObjetoAlmacenado> verificarObjeto(String claveObjeto) {
		try {
			HeadObjectResponse objeto = s3.headObject(pedido -> pedido
					.bucket(bucket)
					.key(claveObjeto)
					.checksumMode(ChecksumMode.ENABLED));
			return Optional.of(new ObjetoAlmacenado(objeto.contentLength(), objeto.checksumSHA256()));
		} catch (NoSuchKeyException e) {
			return Optional.empty();
		} catch (S3Exception e) {
			if (e.statusCode() == 404) {
				return Optional.empty();
			}
			throw noDisponible(e);
		} catch (SdkException e) {
			throw noDisponible(e);
		}
	}

	@Override
	public LecturaFirmada firmarLectura(String claveObjeto, Duration vigencia) {
		GetObjectRequest lectura = GetObjectRequest.builder()
				.bucket(bucket)
				.key(claveObjeto)
				.build();
		PresignedGetObjectRequest firmada = firmante.presignGetObject(pedido -> pedido
				.signatureDuration(vigencia)
				.getObjectRequest(lectura));
		return new LecturaFirmada(firmada.url().toString(), firmada.expiration());
	}

	/** S3 responde igual si el objeto ya no estaba. */
	@Override
	public void borrar(String claveObjeto) {
		try {
			s3.deleteObject(pedido -> pedido.bucket(bucket).key(claveObjeto));
		} catch (SdkException e) {
			throw noDisponible(e);
		}
	}

	private static ConflictoException noDisponible(SdkException causa) {
		log.warn("S3 no respondió como se esperaba: {}", causa.getMessage());
		return new ConflictoException(CodigoError.ALMACENAMIENTO_NO_DISPONIBLE,
				"El almacén de evidencias no responde. Intenta de nuevo en unos minutos.");
	}

}
