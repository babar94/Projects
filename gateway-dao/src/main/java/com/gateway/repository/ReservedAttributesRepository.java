package com.gateway.repository;


import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gateway.entity.ReservedFieldAttributes;

public interface ReservedAttributesRepository extends JpaRepository<ReservedFieldAttributes,Long> {

	Optional<ReservedFieldAttributes> findByBillerId(String billerId);
	Optional<ReservedFieldAttributes> findByBillerName(String billerName);

}
