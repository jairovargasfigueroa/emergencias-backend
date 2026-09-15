package com.uem.ambulancias.integracion.firebase;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.FirebaseDatabase;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "sga.firebase", name = "habilitado", havingValue = "true")
public class FirebaseConfig {

	@Bean
	FirebaseApp firebaseApp(FirebaseProperties propiedades) throws IOException {
		try (InputStream credenciales = Files.newInputStream(Path.of(propiedades.credenciales()))) {
			FirebaseOptions opciones = FirebaseOptions.builder()
					.setCredentials(GoogleCredentials.fromStream(credenciales))
					.setDatabaseUrl(propiedades.urlBaseDatos())
					.build();
			return FirebaseApp.initializeApp(opciones, "sga");
		}
	}

	@Bean
	PublicadorFirebase publicadorFirebase(FirebaseApp firebaseApp) {
		return new PublicadorFirebase(FirebaseDatabase.getInstance(firebaseApp));
	}

}
