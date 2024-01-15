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
			BigDecimal amount, double charges, String activity, String paymentRefNo, String billerNumber,
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
		paymentLog.setAmount(amount);
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

}
