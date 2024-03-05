package com.gateway.service.impl;

import java.math.BigDecimal;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.gateway.entity.PaymentLog;
import com.gateway.repository.PaymentLogRepository;
import com.gateway.service.PaymentLoggingService;

@Service
public class PaymentLoggingServiceImpl implements PaymentLoggingService {

	private static final Logger LOG = LoggerFactory.getLogger(PaymentLoggingServiceImpl.class);

	@Autowired
	private PaymentLogRepository transactionDetialsLogRepository;

	@Async("paymentLoggingExecutor")
	public void paymentLog(Date requestDatetime, Date responsetDatetime, String rrn, String stan, String responseCode,
			String responseDescription, String cnic, String mobile, String name, String consumerNumber, String billerId,
			BigDecimal amountPaid, BigDecimal amountwithinduedate, BigDecimal amountafterduedate, double charges,
			String activity, String paymentRefNo, String billerNumber, String transactionStatus, String address,
			double transactionFees, double taxAmount, double total, String channel, String billStatus, String tranDate,
			String tranTime, String province, String tranAuthId) throws Exception {

		PaymentLog paymentLog = new PaymentLog();
		LOG.info("Inserting in table (paymentLog audit)");

		paymentLog.setRequestDatetime(requestDatetime);
		paymentLog.setResponsetDatetime(responsetDatetime);
		paymentLog.setRrn(rrn);
		paymentLog.setStan(stan);
		paymentLog.setResponseCode(responseCode);
		paymentLog.setResponseDescription(responseDescription);
		paymentLog.setCnic(cnic);
		paymentLog.setMobile(mobile);
		paymentLog.setName(name);
		paymentLog.setConsumerNumber(consumerNumber);
		paymentLog.setBillerId(billerId);
		paymentLog.setAmountPaid(amountPaid);
		paymentLog.setAmountwithinduedate(amountwithinduedate);
		paymentLog.setAmountafterduedate(amountafterduedate);
		paymentLog.setCharges(charges);
		paymentLog.setActivity(activity);
		paymentLog.setPaymentRefNo(paymentRefNo);
		paymentLog.setBillerNumber(billerNumber);
		paymentLog.setTransactionStatus(transactionStatus);
		paymentLog.setAddress(address);
		paymentLog.setTransactionFees(transactionFees);
		paymentLog.setTaxAmount(taxAmount);
		paymentLog.setTotal(total);
		paymentLog.setChannel(channel);
		paymentLog.setBillStatus(billStatus);
		paymentLog.setTranDate(tranDate);
		paymentLog.setTranTime(tranTime);
		paymentLog.setProvince(province);
		paymentLog.setTranAuthId(tranAuthId);

		transactionDetialsLogRepository.save(paymentLog);

		LOG.info("Inserted in table (audit)");

	}

	@Async("paymentLoggingExecutor")
	@Override
	public void paymentLog(Date requestDatetime, Date responsetDatetime, String rrn, String stan, String responseCode,
			String responseDescription, String cnic, String mobile, String name, String consumerNumber, String billerId,
			BigDecimal amountPaid, double charges, String activity, String paymentRefNo, String billerNumber,
			String transactionStatus, String address, double transactionFees, double taxAmount, double total,
			String channel, String billStatus, String tranDate, String tranTime, String province, String tranAuthId)
			throws Exception {

		PaymentLog paymentLog = new PaymentLog();
		LOG.info("Inserting in table (paymentLog audit)");

		paymentLog.setRequestDatetime(requestDatetime);
		paymentLog.setResponsetDatetime(responsetDatetime);
		paymentLog.setRrn(rrn);
		paymentLog.setStan(stan);
		paymentLog.setResponseCode(responseCode);
		paymentLog.setResponseDescription(responseDescription);
		paymentLog.setCnic(cnic);
		paymentLog.setMobile(mobile);
		paymentLog.setName(name);
		paymentLog.setConsumerNumber(consumerNumber);
		paymentLog.setBillerId(billerId);
		paymentLog.setAmountPaid(amountPaid);
		paymentLog.setCharges(charges);
		paymentLog.setActivity(activity);
		paymentLog.setPaymentRefNo(paymentRefNo);
		paymentLog.setBillerNumber(billerNumber);
		paymentLog.setTransactionStatus(transactionStatus);
		paymentLog.setAddress(address);
		paymentLog.setTransactionFees(transactionFees);
		paymentLog.setTaxAmount(taxAmount);
		paymentLog.setTotal(total);
		paymentLog.setChannel(channel);
		paymentLog.setBillStatus(billStatus);
		paymentLog.setTranDate(tranDate);
		paymentLog.setTranTime(tranTime);
		paymentLog.setProvince(province);
		paymentLog.setTranAuthId(tranAuthId);

		transactionDetialsLogRepository.save(paymentLog);

		LOG.info("Inserted in table (audit)");

	}
	
	@Async("paymentLoggingExecutor")
	@Override
	public void paymentLog(Date requestedDate, Date responsedate,String rrn,String stan,String responseCode,
			String responseDesc,String studentName,String billNumber,String billerId,
			BigDecimal amountInDueDate,BigDecimal amountAfterDueDate,String billinquiry,
			String transactionStatus,String channel,String billstatus,String tranDate,String tranTime,String transAuthId) {

	
		PaymentLog paymentLog = new PaymentLog();
		LOG.info("Inserting in table (paymentLog audit)");

		paymentLog.setRequestDatetime(requestedDate);
		paymentLog.setResponsetDatetime(responsedate);
		paymentLog.setRrn(rrn);
		paymentLog.setStan(stan);
		paymentLog.setResponseCode(responseCode);
		paymentLog.setResponseDescription(responseDesc);
		paymentLog.setConsumerNumber(billNumber);
		paymentLog.setBillerId(billerId);
		paymentLog.setAmountwithinduedate(amountInDueDate);
		paymentLog.setAmountafterduedate(amountAfterDueDate);
		paymentLog.setTransactionStatus(transactionStatus);
		paymentLog.setChannel(channel);
		paymentLog.setBillStatus(billstatus);
		paymentLog.setTranDate(tranDate);
		paymentLog.setTranTime(tranTime);
		paymentLog.setTranAuthId(transAuthId);

		transactionDetialsLogRepository.save(paymentLog);


	}
	
	
	
	
	

}
