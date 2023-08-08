package com.gateway.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.gateway.entity.ProvinceTransaction;


@Repository
public interface ProvinceTransactionDao extends JpaRepository<ProvinceTransaction, Integer> {
	
//	@Query(value = "SELECT * FROM api_gw_province_transactions a WHERE a.province_code = :provinceCode", nativeQuery = true)
//	public ProvinceTransaction getProvinceTransaction(String provinceCode);
	
	public ProvinceTransaction findByProvinceCode(String provinceCode);
}