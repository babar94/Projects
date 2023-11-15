package com.gateway.controller;

import java.io.IOException;

import org.apache.catalina.servlet4preview.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.gateway.api.ApiController;
import com.gateway.request.billinquiry.BillInquiryRequest;
import com.gateway.request.billpayment.BillPaymentRequest;
import com.gateway.request.paymentinquiry.PaymentInquiryRequest;
import com.gateway.response.billerlistresponse.BillerListResponse;
import com.gateway.response.billinquiryresponse.BillInquiryResponse;
import com.gateway.response.billpaymentresponse.BillPaymentResponse;
import com.gateway.response.paymentinquiryresponse.PaymentInquiryResponse;
import com.gateway.service.BillDetailsService;
import com.gateway.service.BillInquiryService;
import com.gateway.service.BillPaymentService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Api(tags = "Bill Controller")
@RestController
@RequestMapping(path = ApiController.BILL_URL)
public class BillController extends ApiController {
	private static final Logger LOG = LoggerFactory.getLogger(BillController.class);

	@Autowired
	private BillInquiryService billInquiryService;

	@Autowired
	private BillDetailsService billDetailsService;

	@Autowired
	private BillPaymentService billPaymentService;

	@ApiOperation(value = "API Gateway - Bill Inquiry", notes = "Bill inquiry")
	@RequestMapping(path = "/billinquiry", method = RequestMethod.POST)
	public BillInquiryResponse billInquiry(@RequestBody BillInquiryRequest request, HttpServletRequest httpRequestData)
			throws IOException {
		BillInquiryResponse response = null;
		LOG.info("Bill Controller - Bill Inquiry");
		try {

			LOG.info("bill Controller - Bill Payment");
			try {
			
				response = billInquiryService.billInquiry(httpRequestData, request);

			} catch (Exception ex) {
				LOG.error("{}", ex);
			}

			return response;

		} catch (Exception ex) {
			LOG.error("Exception in billInquiry method{}", ex);
		}
		return response;

	}

	@ApiOperation(value = "API Gateway - Bill Payment", notes = "Bill Payment")
	@RequestMapping(path = "/billpayment", method = RequestMethod.POST)
	public BillPaymentResponse billPayment(@RequestBody BillPaymentRequest request, HttpServletRequest httpRequestData)
			throws IOException {

		BillPaymentResponse response = null;
		LOG.info("bill Controller - Bill Payment");
		try {

			LOG.info("Pin Controller - Validate Request Params");

			LOG.info("Bill Controller - Bill Service");
			response = billPaymentService.billPayment(httpRequestData, request);

		} catch (Exception ex) {
			LOG.error("{}", ex);
		}
		return response;

	}

	@ApiOperation(value = "API Gateway - Payment Inquiry", notes = "Payment inquiry")
	@RequestMapping(path = "/paymentinquiry", method = RequestMethod.POST)
	public PaymentInquiryResponse paymentInquiry(@RequestBody PaymentInquiryRequest request,
			HttpServletRequest httpRequestData) throws IOException {
		PaymentInquiryResponse response = null;

		LOG.info("Bill Controller - Payment Inquiry");
		try {

			LOG.info("Pin Controller - Validate Request Params");

			response = billDetailsService.paymentInquiry(httpRequestData, request);

			return response;

		} catch (Exception ex) {
			LOG.error("{}", ex);
		}
		return response;

	}

	@ApiOperation(value = "API Gateway - Get Biller List", notes = "Get Biller List")
	@RequestMapping(path = "/getbillerlist", method = RequestMethod.GET)
	public BillerListResponse getBillerList(HttpServletRequest httpRequestData) throws IOException {
		BillerListResponse response = null;

		LOG.info("Bill Controller -Get Biller List");
		try {

			response = billDetailsService.getBillerList(httpRequestData);

			return response;

		} catch (Exception ex) {
			LOG.error("{}", ex);
		}
		return response;

	}

	
	}

