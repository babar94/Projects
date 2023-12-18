package com.gateway.service;

import javax.servlet.http.HttpServletRequest;

import com.gateway.request.billpayment.BillPaymentRequest;
import com.gateway.response.billpaymentresponse.BillPaymentResponse;

public interface BillPaymentService {

	public BillPaymentResponse billPayment(HttpServletRequest httpRequestData, BillPaymentRequest request);

}