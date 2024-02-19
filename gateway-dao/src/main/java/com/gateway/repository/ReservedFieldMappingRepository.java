package com.gateway.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gateway.entity.ReservedFieldsMapping;

public interface ReservedFieldMappingRepository extends JpaRepository<ReservedFieldsMapping, Long> {

	// ReservedFieldsMapping findByBillerConfigurationBillerId(String billerId);

	List<ReservedFieldsMapping> findByBillerConfigurationBillerId(String billerId);

}
