package com.gateway.service;

import java.util.Date;

import com.gateway.utils.Constants;

public interface PaymentLoggingService {

	public void paymentLog(Date requestDatetime, Date responsetDatetime, String rrn, String stan, String responseCode,
			String responseDescription, String cnic, String mobile, String name, String consumerNumber, String billerId,
			double amount, double charges, String activity, String paymentRefNo, String billerNumber,
			String transactionStatus, String address, double transactionFees, double taxAmount, double total, String channel,
			String billStatus, String tranDate, String tranTime, String province,String tranAuthId) throws Exception;

	
	
	
}
