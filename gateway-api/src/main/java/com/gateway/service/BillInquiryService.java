package com.gateway.service;

import com.gateway.request.billinquiry.BillInquiryRequest;
import com.gateway.response.BillInquiryValidationResponse;
import com.gateway.response.billinquiryresponse.BillInquiryResponse;

import jakarta.servlet.http.HttpServletRequest;

public interface BillInquiryService {

	public BillInquiryResponse billInquiry(HttpServletRequest httpRequestData, BillInquiryRequest request);

	public BillInquiryResponse billInquiryOffline(HttpServletRequest httpRequestData, BillInquiryRequest request,
			String parentBiller, String subBiller);

	public BillInquiryResponse billInquiryVms(HttpServletRequest httpRequestData, BillInquiryRequest request,
			String parentBiller, String subBiller);
	
	public BillInquiryResponse billInquiryBEOE(BillInquiryRequest request, HttpServletRequest httpRequestData);

	public BillInquiryResponse billInquiryKppsc(BillInquiryRequest request,
			BillInquiryValidationResponse billInquiryValidationResponse, HttpServletRequest httpRequestData);

	public BillInquiryResponse billInquiryFbr(BillInquiryRequest request, HttpServletRequest httpRequestData);

	public BillInquiryResponse billInquiryPta(BillInquiryRequest request, HttpServletRequest httpRequestData);

	public BillInquiryResponse billInquiryAiou(BillInquiryRequest request, HttpServletRequest httpRequestData);

	public BillInquiryResponse billInquiryDls(BillInquiryRequest request, HttpServletRequest httpRequestData);

	public BillInquiryResponse billInquiryPitham(BillInquiryRequest request, HttpServletRequest httpRequestData);

	public BillInquiryResponse billInquiryThardeep(BillInquiryRequest request, HttpServletRequest httpRequestData);

	public BillInquiryResponse billInquiryUom(BillInquiryRequest request, HttpServletRequest httpRequestData);

	public BillInquiryResponse billInquiryBzu(BillInquiryRequest request, HttpServletRequest httpRequestData);

	public BillInquiryResponse billInquirySlic(BillInquiryRequest request, HttpServletRequest httpRequestData);

	public BillInquiryResponse billInquiryBprra(BillInquiryRequest request, HttpServletRequest httpRequestData);

	public BillInquiryResponse billInquiryBiseKohat(BillInquiryRequest request, HttpServletRequest httpRequestData);

	public BillInquiryResponse billInquiryLesco(BillInquiryRequest request, HttpServletRequest httpRequestData);
	
	public BillInquiryResponse billInquiryWasa(BillInquiryRequest request, HttpServletRequest httpRequestData);

	public BillInquiryResponse billInquiryPU(BillInquiryRequest request, HttpServletRequest httpRequestData);

	

}