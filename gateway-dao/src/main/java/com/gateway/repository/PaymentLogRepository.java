package com.gateway.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gateway.entity.PaymentLog;

public interface PaymentLogRepository extends JpaRepository<PaymentLog, Long> {

	

	PaymentLog findFirstByBillerIdAndBillerNumberAndBillStatusIgnoreCaseAndActivityAndResponseCodeOrderByIDDesc(
			String billerId, String billerNumber, String billStatus, String activity, String responseCode);

	public List<PaymentLog> findByRrn(String rrn);

	public List<PaymentLog> findByTranAuthIdAndActivity(String tranAuthId, String activity);

	public PaymentLog findFirstByBillerNumberAndBillStatus(String billNumber, String billStatus);
	

}
