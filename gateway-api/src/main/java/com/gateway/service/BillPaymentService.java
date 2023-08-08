package com.gateway.service;

import org.apache.catalina.servlet4preview.http.HttpServletRequest;

import com.gateway.request.billpayment.BillPaymentRequest;
import com.gateway.request.billpayment.OneLinkBillPaymentRequest;
import com.gateway.response.billpaymentresponse.BillPaymentResponse;
import com.gateway.response.billpaymentresponse.OneLinkBillPaymentResponse;

public interface BillPaymentService {

	public BillPaymentResponse billPayment(HttpServletRequest httpRequestData, BillPaymentRequest request);
	public OneLinkBillPaymentResponse oneLinkBillPayment(HttpServletRequest httpRequestData, OneLinkBillPaymentRequest oneLinkBillPaymentRequest);

}