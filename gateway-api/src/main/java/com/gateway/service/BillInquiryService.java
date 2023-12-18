package com.gateway.service;

import javax.servlet.http.HttpServletRequest;

import com.gateway.request.billinquiry.BillInquiryRequest;
import com.gateway.response.billinquiryresponse.BillInquiryResponse;

public interface BillInquiryService {

	public BillInquiryResponse billInquiry(HttpServletRequest httpRequestData,BillInquiryRequest request);
//	public OneLinkBillInquiryResponse oneLinkBillInquiry(HttpServletRequest httpRequestData,OneLinkBillInquiryRequest request);
	public BillInquiryResponse billInquiryOffline(HttpServletRequest httpRequestData,BillInquiryRequest request,String parentBiller,String subBiller);
}