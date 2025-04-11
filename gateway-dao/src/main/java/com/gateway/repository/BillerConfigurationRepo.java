package com.gateway.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gateway.entity.BillerConfiguration;

public interface BillerConfigurationRepo extends JpaRepository<BillerConfiguration, Long> {

	Optional<BillerConfiguration> findByBillerId(String ParentBillerId);

	List<BillerConfiguration> findByIsActiveTrue();

}
