package com.gateway.service;

import com.gateway.request.billpayment.BillPaymentRequest;
import com.gateway.response.billpaymentresponse.BillPaymentResponse;

import jakarta.servlet.http.HttpServletRequest;

public interface BillPaymentService {

	public BillPaymentResponse billPayment(HttpServletRequest httpRequestData, BillPaymentRequest request);

	public BillPaymentResponse billPaymentOffline(BillPaymentRequest request, HttpServletRequest httpRequestData,
			String parentBiller, String subBiller);

	public BillPaymentResponse billPaymentPta(BillPaymentRequest request);

	public BillPaymentResponse billPaymentPral(BillPaymentRequest request);

	public BillPaymentResponse billPaymentFbr(BillPaymentRequest request,HttpServletRequest httpRequestData);
}