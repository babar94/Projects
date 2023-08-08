package com.gateway.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gateway.entity.BillersList;



public interface BillerListRepository extends JpaRepository<BillersList, Long> {
	
//	 @Query(value = "SELECT * FROM pg_billers", nativeQuery = true)
//	    public ArrayList<BillersList> getBillerList();
	
//	@Query(value = "SELECT * FROM pg_billers a WHERE a.biller_id = :billerId", nativeQuery = true)
//    public BillersList getBiller(String billerId);
//	
	
	public List<BillersList> findAll();
	public BillersList findByBillerId(String billerId); 
	 

}
