package com.gateway.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gateway.entity.PgPaymentLog;

public interface PgPaymentLogRepository extends JpaRepository<PgPaymentLog, Long>{

	//public List<PgPaymentLog> findByBillerIdAndBillerNumberAndBillStatusAndActivityAndResponseCode(Long billerId, String billerNumber,String billStatus,String Activity,String responseCode);
	//public PgPaymentLog findFirstByBillerNumberAndBillStatus(String billNumber,String billStatus);
	public PgPaymentLog findFirstByVoucherIdAndBillerIdAndBillStatus(String voucherId,String BillerId,String billStatus);
	
}
