package com.gateway.utils;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gateway.entity.PaymentLog;
import com.gateway.repository.PaymentLogRepository;

@Component
public class ValidationUtil {

	@Autowired
	private PaymentLogRepository paymentLogRepository;

	public boolean isDuplicateRRN(String rrn) {

		List<PaymentLog> paymentHistory = paymentLogRepository.findByRrn(rrn);
		return paymentHistory != null && !paymentHistory.isEmpty();
	}

	public boolean isNullOrEmpty(String value) {
		return value == null || value.trim().isEmpty();
	}

}
