package com.gateway.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gateway.entity.BillerCredential;

public interface BillerCredentialRepository extends JpaRepository<BillerCredential, Long> {

	Optional<BillerCredential> findByBillerIdAndStatusTrue(String billerId);
}
