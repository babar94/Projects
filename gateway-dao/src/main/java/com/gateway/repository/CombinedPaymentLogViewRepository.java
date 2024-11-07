package com.gateway.repository;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.gateway.entity.CombinedPaymentLogView;

public interface CombinedPaymentLogViewRepository extends JpaRepository<CombinedPaymentLogView, Long> {

	
	@Query("SELECT c FROM CombinedPaymentLogView c WHERE c.billerNumber = :billerNumber AND c.billStatus = :billStatus AND (c.activity = :BillPayment OR c.activity = :RbtsFundTransfer OR c.activity = :CreditDebitCard) And c.billerId=:billerId ORDER BY c.requestDateTime DESC")
	List<CombinedPaymentLogView> findTopByBillerNumberAndBillStatusAndActivitiesAndBillerIdOrderByRequestDateTimeDesc(
			@Param("billerNumber") String billerNumber, @Param("billStatus") String billStatus,
			@Param("BillPayment") String BillPayment, @Param("RbtsFundTransfer") String RbtsFundTransfer,
			@Param("CreditDebitCard") String CreditDebitCard,
			@Param("billerId") String billerId
			, Pageable pageable);
	
	default CombinedPaymentLogView findFirstByBillerNumberAndBillStatusAndActivitiesBillerIdOrderByRequestDateTimeDesc(
			String billerNumber, String billStatus, String BillPayment, String RbtsFundTransfer,
			String CreditDebitCard,String billerId) {
		Pageable pageable = PageRequest.of(0, 1);
		List<CombinedPaymentLogView> results = findTopByBillerNumberAndBillStatusAndActivitiesAndBillerIdOrderByRequestDateTimeDesc(
				billerNumber, billStatus, BillPayment, RbtsFundTransfer, CreditDebitCard,billerId, pageable);
		return results.isEmpty() ? null : results.get(0);
	}

}
