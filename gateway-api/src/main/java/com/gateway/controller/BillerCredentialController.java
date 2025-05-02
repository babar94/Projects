package com.gateway.controller;

import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gateway.api.ApiController;
import com.gateway.entity.BillerCredential;
import com.gateway.request.BillerCredentialRequest;
import com.gateway.response.BillerCredentialResponse;
import com.gateway.service.BillerCredentialService;

import jakarta.validation.Valid;

@RestController
@RequestMapping(path = ApiController.BILLER_CRENDENTIALS)
public class BillerCredentialController {

	private final BillerCredentialService credentialService;

	public BillerCredentialController(BillerCredentialService credentialService) {
		this.credentialService = credentialService;
	}

	@PostMapping("/save")
	public ResponseEntity<BillerCredentialResponse> saveCredential(
			@Valid @RequestBody BillerCredentialRequest request) {

		BillerCredentialResponse response = credentialService.saveCredential(request);
		return ResponseEntity.ok(response);
	}

	@PutMapping("/update/{billerId}")
	public ResponseEntity<BillerCredentialResponse> updateCredential(@PathVariable String billerId,
			@RequestBody BillerCredentialRequest request) {

		BillerCredentialResponse updatedCredential = credentialService.updateCredential(billerId, request);
		return ResponseEntity.ok(updatedCredential);
	}

	@GetMapping("/get/{billerId}")
	public ResponseEntity<BillerCredentialResponse> getDecryptedCredentials(@PathVariable String billerId) {
		Optional<BillerCredentialResponse> credential = credentialService.getDecryptedCredentials(billerId);
		return credential.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
	}
}
