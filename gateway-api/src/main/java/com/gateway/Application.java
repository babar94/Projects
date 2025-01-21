
package com.gateway;

import org.jasypt.encryption.StringEncryptor;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.gateway.utils.Constants;
import com.ulisesbocchio.jasyptspringboot.annotation.EnableEncryptableProperties;

@SpringBootApplication(scanBasePackages = { "com.gateway" })
@EnableEncryptableProperties

public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@Bean("jasyptStringEncryptor")
	public StringEncryptor stringEncryptor() {
		StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
		encryptor.setPassword(Constants.Key.SECRET_KEY); // Replace with your encryption key or passphrase
		encryptor.setAlgorithm(Constants.Key.ALGORITHM);
		encryptor.setIvGenerator(new org.jasypt.iv.NoIvGenerator());
		return encryptor;

	}

}
