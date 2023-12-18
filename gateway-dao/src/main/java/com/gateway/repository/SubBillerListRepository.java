package com.gateway.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.gateway.entity.BillerConfiguration;
import com.gateway.entity.SubBillersList;

public interface SubBillerListRepository extends JpaRepository<SubBillersList, Long> {

	Optional<SubBillersList> findBySubBillerIdAndBillerConfiguration(String subBillerId, BillerConfiguration biller);
//	  @Query("SELECT sbl FROM SubBillersList sbl WHERE sbl.billerConfiguration.id=:billerId And sbl.subBillerId =:SubBillerID")
//	  List<SubBillersList> findAllByBillerIdAndSubBillerId(@Param("billerId") Long billerId,@Param("SubBillerID") String SubBillerID);	 
//	
//	@Query("SELECT s FROM SubBillersList s WHERE s.subBillerId = :subBillerId AND s.isActive = true AND s.billerConfiguration = :billerConfiguration")
//	Optional<SubBillersList> findBySubBillerIdAndIsActiveTrueAndBillerConfiguration(
//			@Param("subBillerId") String subBillerId,
//			@Param("billerConfiguration") BillerConfiguration billerConfiguration);
	// Optional<SubBillersList> findByBillerIdAndSubBillerId(String parentBillerId,
	// String subBillerId);

//	 @Query(value = "SELECT * FROM pg_billers", nativeQuery = true)
//	    public ArrayList<BillersList> getBillerList();

//	@Query(value = "SELECT * FROM pg_billers a WHERE a.biller_id = :billerId", nativeQuery = true)

}
