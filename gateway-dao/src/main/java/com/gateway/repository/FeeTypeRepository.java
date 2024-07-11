package com.gateway.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gateway.entity.FeeType;

public interface FeeTypeRepository extends JpaRepository<FeeType, Long> {

}
