package com.gateway.service.impl;

import java.time.LocalDateTime;
import java.util.Optional;

import org.jasypt.encryption.StringEncryptor;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.gateway.controller.BillController;
import com.gateway.entity.BillerCredential;
import com.gateway.exception.BillerAlreadyExistsException;
import com.gateway.exception.BillerNotFoundException;
import com.gateway.exception.InvalidCredentialException;
import com.gateway.repository.BillerCredentialRepository;
import com.gateway.request.BillerCredentialRequest;
import com.gateway.response.BillerCredentialResponse;
import com.gateway.service.BillerCredentialService;

@Service
public class BillerCredentialServiceImpl implements BillerCredentialService {

	private final BillerCredentialRepository repository;
	private static final Logger LOG = LoggerFactory.getLogger(BillController.class);

	@Autowired
	@Qualifier("jasyptStringEncryptor")
	private StringEncryptor encryptor;

	private final ModelMapper modelMapper;

	public BillerCredentialServiceImpl(BillerCredentialRepository repository, ModelMapper modelMapper) {
		this.repository = repository;
		this.modelMapper = modelMapper;

	}

	@Override
	public BillerCredentialResponse saveCredential(BillerCredentialRequest request) {

		repository.findByBillerIdAndStatusTrue(request.getBillerId()).ifPresent(b -> {
			throw new BillerAlreadyExistsException("Biller with ID '" + request.getBillerId() + "' already exists.");
		});

		BillerCredential credential = new BillerCredential();
		credential.setBillerId(request.getBillerId());
		credential.setBillerName(request.getBillerName());
		credential.setUsername(encryptor.encrypt(request.getUsername()));
		credential.setPassword(encryptor.encrypt(request.getPassword()));
		credential.setStatus(true);
		credential.setCreatedAt(LocalDateTime.now());
		credential.setModifiedAt(LocalDateTime.now());

		BillerCredential saved = repository.save(credential);
		return BillerCredentialResponse.builder().billerId(saved.getBillerId()).billerName(saved.getBillerName())
				.status(saved.getStatus()).createdAt(saved.getCreatedAt()).build();
	}

	@Override
	public BillerCredentialResponse updateCredential(String billerId, BillerCredentialRequest request) {

		BillerCredential existing = repository.findByBillerIdAndStatusTrue(request.getBillerId()).orElseThrow(
				() -> new BillerAlreadyExistsException("No biller found with ID: " + request.getBillerId()));

		existing.setUsername(encryptor.encrypt(request.getUsername()));
		existing.setPassword(encryptor.encrypt(request.getPassword()));
		existing.setModifiedAt(LocalDateTime.now());

		BillerCredential saved = repository.save(existing);

		return BillerCredentialResponse.builder().billerId(saved.getBillerId()).billerName(saved.getBillerName())
				.status(saved.getStatus()).modifiedAt(saved.getCreatedAt()).build();

	}

	@Override
	public Optional<BillerCredentialResponse> getDecryptedCredentials(String billerId) {

		return repository.findByBillerIdAndStatusTrue(billerId).flatMap(credential -> {

			if (credential.getUsername() == null || credential.getPassword() == null) {
				LOG.info("Username or password is null for biller: {}", billerId);
				return Optional.empty();
			}

			String decryptedUsername = encryptor.decrypt(credential.getUsername());
			String decryptedPassword = encryptor.decrypt(credential.getPassword());

			return Optional.of(
					BillerCredentialResponse.builder().username(decryptedUsername).password(decryptedPassword).build());
		});
	}

}
