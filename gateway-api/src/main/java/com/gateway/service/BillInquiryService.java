package com.gateway.service;

import org.apache.catalina.servlet4preview.http.HttpServletRequest;

import com.gateway.request.billinquiry.BillInquiryRequest;
import com.gateway.request.billinquiry.OneLinkBillInquiryRequest;
import com.gateway.response.billinquiryresponse.BillInquiryResponse;
import com.gateway.response.billinquiryresponse.OneLinkBillInquiryResponse;

public interface BillInquiryService {

	public BillInquiryResponse billInquiry(HttpServletRequest httpRequestData,BillInquiryRequest request);
	public OneLinkBillInquiryResponse oneLinkBillInquiry(HttpServletRequest httpRequestData,OneLinkBillInquiryRequest request);

}