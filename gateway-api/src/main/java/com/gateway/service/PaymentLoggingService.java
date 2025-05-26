package com.gateway.service;

import java.math.BigDecimal;
import java.util.Date;

import com.gateway.entity.PaymentLog;

public interface PaymentLoggingService {

	public PaymentLog paymentLog(Date requestDatetime, Date responsetDatetime, String rrn, String stan, String responseCode,
			String responseDescription, String cnic, String mobile, String name, String consumerNumber, String billerId,
			BigDecimal amount, BigDecimal amountwithinduedate, BigDecimal amountafterduedate, double charges,
			String activity, String paymentRefNo, String billerNumber, String transactionStatus, String address,
			double total, String channel, String billStatus, String tranDate, String tranTime, String province,
			String tranAuthId, String bankName, String bankCode, String branchName, String branchCode, String username,
			String feeDetail,String dueDate,String billingMonth) throws Exception;


	
	public PaymentLog paymentLogBppra(Date requestedDate, Date responsedate, String rrn, String stan, String responseCode,
			String responseDesc, String studentName, String billNumber, String billerId, BigDecimal bigDecimal,
			BigDecimal bigDecimal2, String billinquiry, String transactionStatus, String channel, String billstatus,
			String tranDate, String tranTime, String transAuthId, BigDecimal amountpaid, String duedate,
			String billingMonth, String paymentRefno, String bankName, String bankCode, String branchName,
			String branchCode, String thirdPartyAuthId, String username,String feeDetail);
	
	public void paymentLog(Date requestedDate, Date responsedate, String rrn, String stan, String responseCode,
			String responseDesc, String studentName, String billNumber, String billerId, BigDecimal bigDecimal,
			BigDecimal bigDecimal2, String billinquiry, String transactionStatus, String channel, String billstatus,
			String tranDate, String tranTime, String transAuthId, BigDecimal amountpaid, String duedate,
			String billingMonth, String paymentRefno, String bankName, String bankCode, String branchName,
			String branchCode, String thirdPartyAuthId, String username,String feeDetail);

	
	
	
}
