package com.gateway.service;

import java.math.BigDecimal;
import java.util.Date;

import com.gateway.utils.Constants;

public interface PaymentLoggingService {

	public void paymentLog(Date requestDatetime, Date responsetDatetime, String rrn, String stan, String responseCode,
			String responseDescription, String cnic, String mobile, String name, String consumerNumber, String billerId,
			BigDecimal amount, BigDecimal amountwithinduedate, BigDecimal amountafterduedate, double charges,
			String activity, String paymentRefNo, String billerNumber, String transactionStatus, String address,
			double transactionFees, double taxAmount, double total, String channel, String billStatus, String tranDate,
			String tranTime, String province, String tranAuthId) throws Exception;

	public void paymentLog(Date requestDatetime, Date responsetDatetime, String rrn, String stan, String responseCode,
			String responseDescription, String cnic, String mobile, String name, String consumerNumber, String billerId,
			BigDecimal amount, double charges, String activity, String paymentRefNo, String billerNumber,
			String transactionStatus, String address, double transactionFees, double taxAmount, double total,
			String channel, String billStatus, String tranDate, String tranTime, String province, String tranAuthId)
			throws Exception;

}
