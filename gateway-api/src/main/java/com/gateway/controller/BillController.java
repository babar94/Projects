package com.gateway.controller;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gateway.api.ApiController;
import com.gateway.repository.PaymentLogRepository;
import com.gateway.request.billinquiry.BillInquiryRequest;
import com.gateway.request.billpayment.BillPaymentRequest;
import com.gateway.request.paymentinquiry.PaymentInquiryRequest;
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
import com.gateway.utils.ValidationUtil;

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

	@Autowired
	private ValidationUtil validationUtil;

	@RequestMapping(path = "/billinquiry", method = RequestMethod.POST)
	public BillInquiryResponse billInquiry(@RequestBody BillInquiryRequest request, HttpServletRequest httpRequestData)
			throws IOException {
		BillInquiryResponse response = null;
		
		LOG.info("Bill Controller - Bill Inquiry");
		
		try {

			String rrn = request.getInfo().getRrn();
			String stan = request.getInfo().getStan();
	       
	        String jsonString = toJsonString(request);
	        
	        String regex = "<[^>]+>";


	        Pattern pattern = Pattern.compile(regex);

	        Matcher matcher = pattern.matcher(jsonString);
	        				
			if(matcher.find()) {	
				return response = new BillInquiryResponse(new Info(Constants.ResponseCodes.INVALID_DATA,
						Constants.ResponseDescription.BAD_TRANSACTION, rrn, stan), null, null);
		
			}
			
	
			if (validationUtil.isNullOrEmpty(rrn)) {
				return response = new BillInquiryResponse(new Info(Constants.ResponseCodes.INVALID_DATA,
						Constants.ResponseDescription.INVALID_DATA, rrn, stan), null, null);
			}

			if (validationUtil.isDuplicateRRN(rrn)) {
				return response = new BillInquiryResponse(new Info(Constants.ResponseCodes.DUPLICATE_TRANSACTION,
						Constants.ResponseDescription.DUPLICATE_TRANSACTION, rrn, stan), null, null);

			}

			response = billInquiryService.billInquiry(httpRequestData, request);

		} catch (Exception ex) {
			LOG.error("Exception in billInquiry method{}", ex);
		}

		return response;

	}

	@RequestMapping(path = "/billpayment", method = RequestMethod.POST)
	public BillPaymentResponse billPayment(@RequestBody BillPaymentRequest request, HttpServletRequest httpRequestData)
			throws IOException {

		BillPaymentResponse response = null;

		LOG.info("Bill Controller - Bill Payment");

		try {

			String rrn = request.getInfo().getRrn();
			String stan = request.getInfo().getStan();

			if (validationUtil.isNullOrEmpty(rrn)) {
				return response = new BillPaymentResponse(new InfoPay(Constants.ResponseCodes.INVALID_DATA,
						Constants.ResponseDescription.INVALID_DATA, rrn, stan), null, null);
			}

			if (validationUtil.isDuplicateRRN(rrn)) {
				return response = new BillPaymentResponse(new InfoPay(Constants.ResponseCodes.DUPLICATE_TRANSACTION,
						Constants.ResponseDescription.DUPLICATE_TRANSACTION, rrn, stan), null, null);
			}

			response = billPaymentService.billPayment(httpRequestData, request);

		} catch (Exception ex) {
			LOG.error("{}", ex);
		}
		return response;

	}

	@RequestMapping(path = "/paymentinquiry", method = RequestMethod.POST)
	public PaymentInquiryResponse paymentInquiry(@RequestBody PaymentInquiryRequest request,
			HttpServletRequest httpRequestData) throws IOException {
		PaymentInquiryResponse response = null;

		LOG.info("Bill Controller - Payment Inquiry");
		try {

			String rrn = request.getInfo().getRrn();
			String stan = request.getInfo().getStan();

			if (validationUtil.isNullOrEmpty(rrn)) {
				return response = new PaymentInquiryResponse(new InfoPayInq(Constants.ResponseCodes.INVALID_DATA,
						Constants.ResponseDescription.INVALID_DATA, rrn, stan), null, null);
			}

			if (validationUtil.isDuplicateRRN(rrn)) {
				return response = new PaymentInquiryResponse(
						new InfoPayInq(Constants.ResponseCodes.DUPLICATE_TRANSACTION,
								Constants.ResponseDescription.DUPLICATE_TRANSACTION, rrn, stan),
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
//	@RequestMapping(path = "/getbillerlist", method = RequestMethod.GET)
	@RequestMapping(path = "/billers", method = RequestMethod.GET)
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

	public String toJsonString(Object object) {
	    try {
	        ObjectMapper mapper = new ObjectMapper();
	        return mapper.writeValueAsString(object);
	    } catch (Exception e) {
	        e.printStackTrace();
	        return null;
	    }
	}

}
