package com.uem.ambulancias.integracion.firebase;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.FirebaseDatabase;
import com.uem.ambulancias.emergencias.service.PublicadorTiempoReal;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "sga.firebase", name = "habilitado", havingValue = "true")
public class FirebaseConfig {

	@Bean
	FirebaseDatabase firebaseDatabase(FirebaseProperties propiedades) throws IOException {
		try (InputStream credenciales = Files.newInputStream(Path.of(propiedades.credenciales()))) {
			FirebaseOptions opciones = FirebaseOptions.builder()
					.setCredentials(GoogleCredentials.fromStream(credenciales))
					.setDatabaseUrl(propiedades.urlBaseDatos())
					.build();
			return FirebaseDatabase.getInstance(FirebaseApp.initializeApp(opciones, "sga"));
		}
	}

	@Bean
	PublicadorTiempoReal publicadorFirebase(FirebaseDatabase baseDatos) {
		return new PublicadorFirebase(baseDatos);
	}

}
