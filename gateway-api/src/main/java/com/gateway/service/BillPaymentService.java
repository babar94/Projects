package com.gateway.service;

import com.gateway.request.billpayment.BillPaymentRequest;
import com.gateway.response.billpaymentresponse.BillPaymentResponse;

import jakarta.servlet.http.HttpServletRequest;

public interface BillPaymentService {

	public BillPaymentResponse billPayment(HttpServletRequest httpRequestData, BillPaymentRequest request);

	public BillPaymentResponse billPaymentOffline(BillPaymentRequest request, HttpServletRequest httpRequestData,
			String parentBiller, String subBiller);

	public BillPaymentResponse billPaymentPta(BillPaymentRequest request, HttpServletRequest httpRequestData);

	public BillPaymentResponse billPaymentPral(BillPaymentRequest request, HttpServletRequest httpRequestData);

	public BillPaymentResponse billPaymentFbr(BillPaymentRequest request, HttpServletRequest httpRequestData);

	public BillPaymentResponse billPaymentAiou(BillPaymentRequest request, HttpServletRequest httpRequestData);
	
	public BillPaymentResponse billPaymentPitham(BillPaymentRequest request, HttpServletRequest httpRequestData);

	public BillPaymentResponse billPaymentThardeep(BillPaymentRequest request, HttpServletRequest httpRequestData);
	
	public BillPaymentResponse billPaymentUom(BillPaymentRequest request, HttpServletRequest httpRequestData);
		
	public BillPaymentResponse billPaymentDls(BillPaymentRequest request, HttpServletRequest httpRequestData);

	public BillPaymentResponse billPaymentBzu(BillPaymentRequest request, HttpServletRequest httpRequestData);
	

	

}