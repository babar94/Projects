package com.gateway.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;


import com.gateway.entity.TransactionParams;


@Repository
public interface TransactionParamsDao extends JpaRepository<TransactionParams, Integer> {
	
//	@Query(value = "SELECT * FROM api_gw_transaction_params a", nativeQuery = true)
//	public List<TransactionParams> getTransactionParams();
//	
	
	public List<TransactionParams> findAll();
	
}