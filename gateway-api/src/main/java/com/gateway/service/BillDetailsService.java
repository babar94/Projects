package com.gateway.service;

import com.gateway.request.paymentinquiry.PaymentInquiryRequest;
import com.gateway.response.billerlistresponse.BillerListResponse;
import com.gateway.response.paymentinquiryresponse.PaymentInquiryResponse;

import jakarta.servlet.http.HttpServletRequest;

public interface BillDetailsService {

	public PaymentInquiryResponse paymentInquiry(HttpServletRequest httpRequestData, PaymentInquiryRequest request);
	
	public PaymentInquiryResponse paymentInquiryBeoe(PaymentInquiryRequest request,HttpServletRequest httpRequestData);
	
	public PaymentInquiryResponse paymentInquiryPral(PaymentInquiryRequest request,HttpServletRequest httpRequestData);
	
	public PaymentInquiryResponse paymentInquiryPitham(PaymentInquiryRequest request,HttpServletRequest httpRequestData);
	
	public PaymentInquiryResponse paymentInquiryThardeep(PaymentInquiryRequest request,HttpServletRequest httpRequestData);
	
	public PaymentInquiryResponse paymentInquiryUom(PaymentInquiryRequest request,HttpServletRequest httpRequestData);
	
	public PaymentInquiryResponse paymentInquiryDls(PaymentInquiryRequest request,HttpServletRequest httpRequestData);
	
	public PaymentInquiryResponse paymentInquiryBzu(PaymentInquiryRequest request,HttpServletRequest httpRequestData);
	
	public BillerListResponse getBillerList(HttpServletRequest httpRequestData);

}