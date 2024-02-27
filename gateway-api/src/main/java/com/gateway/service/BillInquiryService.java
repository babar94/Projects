package com.gateway.service;

import com.gateway.request.billinquiry.BillInquiryRequest;
import com.gateway.response.BillInquiryValidationResponse;
import com.gateway.response.billinquiryresponse.BillInquiryResponse;

import jakarta.servlet.http.HttpServletRequest;

public interface BillInquiryService {

	public BillInquiryResponse billInquiry(HttpServletRequest httpRequestData, BillInquiryRequest request);

//	public OneLinkBillInquiryResponse oneLinkBillInquiry(HttpServletRequest httpRequestData,OneLinkBillInquiryRequest request);
	public BillInquiryResponse billInquiryOffline(HttpServletRequest httpRequestData, BillInquiryRequest request,
			String parentBiller, String subBiller);

	public BillInquiryResponse billInquiryFbr(BillInquiryRequest request, HttpServletRequest httpRequestData);

	public BillInquiryResponse billInquiryPta(BillInquiryRequest request, HttpServletRequest httpRequestData);

	public BillInquiryResponse billInquiryAiou(BillInquiryRequest request, HttpServletRequest httpRequestData);

	public BillInquiryResponse billInquiryPithm(BillInquiryRequest request, HttpServletRequest httpRequestData);

}