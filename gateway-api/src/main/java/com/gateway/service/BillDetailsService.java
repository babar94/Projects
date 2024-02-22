package com.gateway.service;

import com.gateway.request.paymentinquiry.PaymentInquiryRequest;
import com.gateway.response.billerlistresponse.BillerListResponse;
import com.gateway.response.paymentinquiryresponse.PaymentInquiryResponse;

import jakarta.servlet.http.HttpServletRequest;

public interface BillDetailsService {

	public PaymentInquiryResponse paymentInquiry(HttpServletRequest httpRequestData, PaymentInquiryRequest reques);

	public BillerListResponse getBillerList(HttpServletRequest httpRequestData);

}