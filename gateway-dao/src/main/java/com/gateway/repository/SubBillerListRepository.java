package com.gateway.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gateway.entity.BillerList;
import com.gateway.entity.SubBillersList;



public interface SubBillerListRepository extends JpaRepository<SubBillersList, Long> {
	
	
	
	Optional<SubBillersList> findBySubBillerIdAndBiller(String subBillerId, BillerList biller);

	
	
	
	
//	 @Query(value = "SELECT * FROM pg_billers", nativeQuery = true)
//	    public ArrayList<BillersList> getBillerList();
	
//	@Query(value = "SELECT * FROM pg_billers a WHERE a.biller_id = :billerId", nativeQuery = true)
	
	 

}
