package com.gateway.repository;


import org.springframework.data.jpa.repository.JpaRepository;

import com.gateway.entity.ReservedFieldAttributes;

public interface ReservedAttributesRepository extends JpaRepository<ReservedFieldAttributes,Long> {

	ReservedFieldAttributes findByBillerId(String billerId);
}
