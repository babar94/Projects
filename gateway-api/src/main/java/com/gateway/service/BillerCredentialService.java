package com.gateway.service;

import java.util.Optional;

import com.gateway.request.BillerCredentialRequest;
import com.gateway.response.BillerCredentialResponse;

public interface BillerCredentialService {

	BillerCredentialResponse saveCredential(BillerCredentialRequest request);

	BillerCredentialResponse updateCredential(String billerId, BillerCredentialRequest request);

	Optional<BillerCredentialResponse> getDecryptedCredentials(String billerId);

}
