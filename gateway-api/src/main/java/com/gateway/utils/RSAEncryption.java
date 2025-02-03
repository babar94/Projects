package com.gateway.utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

import javax.crypto.Cipher;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;

@Component
public class RSAEncryption {

	private static final Logger LOGGER = LogManager.getLogger(RSAEncryption.class);

	@Value("${server.ssl.key-store}")
	private String Paypak_JKS;

	@Value("${server.ssl.key-store-password}")
	private String KEY_STORE_PSWD;

	@Value("${server.ssl.key-password}")
	private String PRIVATE_KEY_PSWD;

	@Value("${server.ssl.key-alias}")
	private String ALIAS;

	private PrivateKey privatekey;
	private PublicKey publickey;
	private String publickeyPlain;

	public void SetRSAEncryption() throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {

		if (privatekey == null || publickeyPlain == null) {
			String filePath = ResourceUtils.getFile(Paypak_JKS).getPath();
			privatekey = initPrivateKeys(filePath, KEY_STORE_PSWD, PRIVATE_KEY_PSWD, ALIAS);
			publickeyPlain = initPublicKeys(filePath, KEY_STORE_PSWD, ALIAS);

		}
	}

	public String getPublicKey() {
		return this.publickeyPlain;
	}

	public String RSADecrypt(String KeyValue) throws Exception {
		// Generate RSA key pair
		// Convert public and private keys to byte arrays
		String decryptedPlaintextOneLink = "";

		if (!KeyValue.isEmpty()) {

			byte[] Value = Base64.getDecoder().decode(KeyValue);

			// Decrypt ciphertext using private key
			Cipher decryptCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");// RSA/ECB/OAEPWithSHA-1AndMGF1Padding
			decryptCipher.init(Cipher.DECRYPT_MODE, privatekey);
			byte[] decryptedOneLink = decryptCipher.doFinal(Value);
			decryptedPlaintextOneLink = new String(decryptedOneLink, "UTF-8");
			System.out.println("Decrypted plaintext: " + decryptedPlaintextOneLink);

		}
		return decryptedPlaintextOneLink;

	}

	public static PrivateKey initPrivateKeys(String jksFilePath, String keystorePswd, String privateKeyPswd,
			String alias) {
		PrivateKey key = null;
		try {
			FileInputStream is = new FileInputStream(jksFilePath);
			KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
			keystore.load(is, keystorePswd.toCharArray());
			key = (PrivateKey) keystore.getKey(alias, privateKeyPswd.toCharArray());
			LOGGER.info("Private key generate successfully ");

		} catch (Exception e) {
			LOGGER.error("Exception occured to generate the private key : {} ", e.getMessage().toString());
		}
		return key;
	}

	public String initPublicKeys(String jksFilePath, String keystorePswd, String alias) {
		PublicKey publicKey = null;
		String publickeyString = null;
		try {
			FileInputStream is = new FileInputStream(jksFilePath);
			KeyStore keystore = KeyStore.getInstance("JKS");
			keystore.load(is, keystorePswd.toCharArray());

			// Get certificate and extract public key
			Certificate cert = keystore.getCertificate(alias);
			if (cert != null) {
				publicKey = cert.getPublicKey();
				publickeyString = convertPublicKeyToString(publicKey);
				LOGGER.info("Public key retrieved successfully.");
			} else {
				LOGGER.error("Certificate not found for alias: {}", alias);
			}

		} catch (Exception e) {
			LOGGER.error("Exception occurred while retrieving the public key: {}", e.getMessage(), e);
		}
		return publickeyString;
	}

	private static String convertPublicKeyToString(PublicKey publicKey) {
		byte[] encodedKey = publicKey.getEncoded();
		String base64Key = Base64.getEncoder().encodeToString(encodedKey);

		// Format the key as a PEM-style string
		return "-----BEGIN PUBLIC KEY----- " + base64Key.replaceAll("(.{64})", "$1 ") + " -----END PUBLIC KEY-----";
	}

	public String Base64Decode(String value) {
		return new String(Base64.getDecoder().decode(value));
	}

}
