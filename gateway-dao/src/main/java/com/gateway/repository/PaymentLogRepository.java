package com.gateway.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.gateway.entity.PaymentLog;

@Repository
public interface PaymentLogRepository extends JpaRepository<PaymentLog, Long> {

	
	PaymentLog findFirstByBillerIdAndBillerNumberAndBillStatusIgnoreCaseAndActivityAndResponseCodeOrderByIDDesc(
			String billerId, String billerNumber, String billStatus, String activity, String responseCode);

	public List<PaymentLog> findByRrn(String rrn);

	public List<PaymentLog> findByTranAuthIdAndActivity(String tranAuthId, String activity);

	public PaymentLog findFirstByBillerNumberAndBillStatus(String billNumber, String billStatus);
	
    public PaymentLog findByBillerNumber(String billNumber);
    
    
    @Query("Select u FROM PaymentLog u WHERE u.rrn = :rrn")
    PaymentLog findByRrnValue(@Param("rrn") String rrn);
	
}
