package com.gateway.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gateway.entity.PendingPayment;

public interface PendingPaymentRepository extends JpaRepository<PendingPayment, Integer> {

	PendingPayment findFirstByVoucherIdOrderByPaymentIdDesc(String voucherId);
}
