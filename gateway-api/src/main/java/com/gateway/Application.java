
package com.gateway;

import org.jasypt.encryption.StringEncryptor;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.ulisesbocchio.jasyptspringboot.annotation.EnableEncryptableProperties;

import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.io.Encoders;
import javax.crypto.SecretKey;

@SpringBootApplication(scanBasePackages = { "com.gateway" })
@EnableEncryptableProperties

public class Application implements CommandLineRunner {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@Bean("jasyptStringEncryptor")
	public StringEncryptor stringEncryptor() {
		StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
		encryptor.setPassword("dbsecretkey"); // Replace with your encryption key or passphrase
		encryptor.setAlgorithm("PBEWithMD5AndDES");
		encryptor.setIvGenerator(new org.jasypt.iv.NoIvGenerator());
		return encryptor;
	}

	@Override
	public void run(String... args) throws Exception {
		SecretKey key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
		String secretString = Encoders.BASE64.encode(key.getEncoded());
		System.out.println("Secret key: " + secretString);

	}

}
