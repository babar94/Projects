package com.gateway.controller;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.gateway.api.ApiController;
import com.gateway.entity.PaymentLog;
import com.gateway.repository.PaymentLogRepository;
import com.gateway.request.billinquiry.BillInquiryRequest;
import com.gateway.request.billpayment.BillPaymentRequest;
import com.gateway.request.paymentinquiry.PaymentInquiryRequest;
import com.gateway.response.BillInquiryValidationResponse;
import com.gateway.response.BillPaymentValidationResponse;
import com.gateway.response.billerlistresponse.BillerListResponse;
import com.gateway.response.billinquiryresponse.BillInquiryResponse;
import com.gateway.response.billinquiryresponse.Info;
import com.gateway.response.billpaymentresponse.BillPaymentResponse;
import com.gateway.response.billpaymentresponse.InfoPay;
import com.gateway.response.paymentinquiryresponse.InfoPayInq;
import com.gateway.response.paymentinquiryresponse.PaymentInquiryResponse;
import com.gateway.service.BillDetailsService;
import com.gateway.service.BillInquiryService;
import com.gateway.service.BillPaymentService;
import com.gateway.utils.Constants;

import jakarta.servlet.http.HttpServletRequest;

//@Api(tags = "Bill Controller")
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
	@Autowired
	private PaymentLogRepository paymentLogRepository;

//	@ApiOperation(value = "API Gateway - Bill Inquiry", notes = "Bill inquiry")
	@RequestMapping(path = "/billinquiry", method = RequestMethod.POST)
	public BillInquiryResponse billInquiry(@RequestBody BillInquiryRequest request, HttpServletRequest httpRequestData)
			throws IOException {
		BillInquiryResponse response = null;
		LOG.info("Bill Controller - Bill Inquiry");
		try {
			if (isDuplicateRRN(request.getInfo().getRrn())) {
				// Duplicate RRN found, return a detailed response
				BillInquiryValidationResponse validationResponse = new BillInquiryValidationResponse(
						Constants.ResponseCodes.DUPLICATE_TRANSACTION,
						Constants.ResponseDescription.DUPLICATE_TRANSACTION, request.getInfo().getRrn(),
						request.getInfo().getStan());

				// Returning a BillInquiryResponse with the validation response

				return new BillInquiryResponse(
						new Info(validationResponse.getResponseCode(), validationResponse.getResponseDesc(),
								validationResponse.getRrn(), validationResponse.getStan()),
						null, null);
			}
			response = billInquiryService.billInquiry(httpRequestData, request);

		} catch (Exception ex) {
			LOG.error("Exception in billInquiry method{}", ex);
		}

		return response;

	}

	// @ApiOperation(value = "API Gateway - Bill Payment", notes = "Bill Payment")
	@RequestMapping(path = "/billpayment", method = RequestMethod.POST)
	public BillPaymentResponse billPayment(@RequestBody BillPaymentRequest request, HttpServletRequest httpRequestData)
			throws IOException {

		BillPaymentResponse response = null;

		LOG.info("Bill Controller - Bill Payment");

		try {

			if (isDuplicateRRN(request.getInfo().getRrn())) {
				// Duplicate RRN found, return a detailed response
				BillPaymentValidationResponse validationResponse = new BillPaymentValidationResponse(
						Constants.ResponseCodes.DUPLICATE_TRANSACTION,
						Constants.ResponseDescription.DUPLICATE_TRANSACTION, request.getInfo().getRrn(),
						request.getInfo().getStan());

				// Returning a BillInquiryResponse with the validation response

				return new BillPaymentResponse(
						new InfoPay(validationResponse.getResponseCode(), validationResponse.getResponseDesc(),
								validationResponse.getRrn(), validationResponse.getStan()),
						null, null);
			}

			response = billPaymentService.billPayment(httpRequestData, request);

		} catch (Exception ex) {
			LOG.error("{}", ex);
		}
		return response;

	}

	// @ApiOperation(value = "API Gateway - Payment Inquiry", notes = "Payment
	// inquiry")
	@RequestMapping(path = "/paymentinquiry", method = RequestMethod.POST)
	public PaymentInquiryResponse paymentInquiry(@RequestBody PaymentInquiryRequest request,
			HttpServletRequest httpRequestData) throws IOException {
		PaymentInquiryResponse response = null;

		LOG.info("Bill Controller - Payment Inquiry");
		try {

			if (isDuplicateRRN(request.getInfo().getRrn())) {
				// Duplicate RRN found, return a detailed response
				BillPaymentValidationResponse validationResponse = new BillPaymentValidationResponse(
						Constants.ResponseCodes.DUPLICATE_TRANSACTION,
						Constants.ResponseDescription.DUPLICATE_TRANSACTION, request.getInfo().getRrn(),
						request.getInfo().getStan());

				// Returning a BillInquiryResponse with the validation response

				return new PaymentInquiryResponse(
						new InfoPayInq(validationResponse.getResponseCode(), validationResponse.getResponseDesc(),
								validationResponse.getRrn(), validationResponse.getStan()),
						null, null);
			}

			response = billDetailsService.paymentInquiry(httpRequestData, request);

			return response;

		} catch (Exception ex) {
			LOG.error("{}", ex);
		}
		return response;

	}

	// @ApiOperation(value = "API Gateway - Get Biller List", notes = "Get Biller
	// List")
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

	private boolean isDuplicateRRN(String rrn) {
		// RRN validation: Check payment history
		List<PaymentLog> paymentHistory = paymentLogRepository.findByRrn(rrn);
		return paymentHistory != null && !paymentHistory.isEmpty();
	}

}
