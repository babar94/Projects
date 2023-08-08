package com.gateway.service;

import org.apache.catalina.servlet4preview.http.HttpServletRequest;

import com.gateway.request.paymentinquiry.PaymentInquiryRequest;
import com.gateway.response.billerlistresponse.BillerListResponse;
import com.gateway.response.paymentinquiryresponse.PaymentInquiryResponse;

public interface BillDetailsService {

	public PaymentInquiryResponse paymentInquiry(HttpServletRequest httpRequestData, PaymentInquiryRequest request);

	public BillerListResponse getBillerList(HttpServletRequest httpRequestData);

}