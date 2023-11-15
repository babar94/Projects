package com.gateway.service;

import org.apache.catalina.servlet4preview.http.HttpServletRequest;

import com.gateway.request.billpayment.BillPaymentRequest;
import com.gateway.response.billpaymentresponse.BillPaymentResponse;

public interface BillPaymentService {

	public BillPaymentResponse billPayment(HttpServletRequest httpRequestData, BillPaymentRequest request);

}