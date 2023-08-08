package com.gateway.controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.validation.Valid;

import org.apache.catalina.servlet4preview.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gateway.api.ApiController;
import com.gateway.request.billinquiry.OneLinkBillInquiryRequest;
import com.gateway.request.billpayment.OneLinkBillPaymentRequest;
import com.gateway.response.BillInquiryValidationResponse;
import com.gateway.response.BillPaymentInquiryValidationResponse;
import com.gateway.response.billinquiryresponse.OneLinkBillInquiryResponse;
import com.gateway.response.billpaymentresponse.OneLinkBillPaymentResponse;
import com.gateway.service.BillInquiryService;
import com.gateway.service.BillPaymentService;
import com.gateway.utils.Constants;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Api(tags = "One Link Bill Controller")
@RestController
@RequestMapping(path = ApiController.BILL_URL)
public class OneLinkBillController {

	private static final Logger LOG = LoggerFactory.getLogger(BillController.class);

	@Autowired
	private BillInquiryService billInquiryService;
	@Autowired
	private BillPaymentService billPaymentService;

	@Autowired
	private ObjectMapper mapper;

	@ApiOperation(value = "API Gateway - One Link Bill Inquiry", notes = "One Link Bill inquiry")
	@RequestMapping(path = "/Payments/BillInquiry", method = RequestMethod.POST)
	public ResponseEntity<Object> OneLinkBillInquiry(@Valid @RequestBody OneLinkBillInquiryRequest request,
			BindingResult bindingResult, HttpServletRequest httpRequestData) throws IOException {
		
		String name = "Method Name: "+new Object(){}.getClass().getEnclosingMethod().getName();
		LOG.info("================ REQUEST "+name+" ================");
		String requestJsonBody = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);
		LOG.info("\n===>> "+name+" REQUEST ::" + requestJsonBody);
		
		//BillInquiryValidationResponse billInquiryResponse = null;
		OneLinkBillInquiryResponse oneLinkBillInquiryResponse = null;
		if (bindingResult.hasErrors()) {
			// If there are validation errors, create a response with the error details
			Map<String, String> errors = new HashMap<>();
			for (FieldError error : bindingResult.getFieldErrors()) {
				errors.put(error.getField(), error.getDefaultMessage());
			}
			LOG.info("ERROR:" + errors);
			oneLinkBillInquiryResponse = new OneLinkBillInquiryResponse(Constants.ResponseCodes.INVALID_DATA, "", "", "", "", "", "", "", "", "", "", request.getReserved());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(oneLinkBillInquiryResponse);
		}
		
		oneLinkBillInquiryResponse = billInquiryService.oneLinkBillInquiry(httpRequestData, request);
		String ResponseJsonBody = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(oneLinkBillInquiryResponse);		
		LOG.info("\n===>> "+name+" Response ::" + ResponseJsonBody);
		return ResponseEntity.ok(oneLinkBillInquiryResponse);

	}

	@ApiOperation(value = "API Gateway - One Link Bill Payment", notes = "One Link Bill Payment")
	@RequestMapping(path = "/Payments/BillPayment", method = RequestMethod.POST)
	public ResponseEntity<Object> OneLinkBillPayment(@Valid @RequestBody OneLinkBillPaymentRequest request,
			BindingResult bindingResult,HttpServletRequest httpRequestData) throws IOException {
		
		String name = "Method Name: "+new Object(){}.getClass().getEnclosingMethod().getName();
		LOG.info("================ REQUEST "+name+" ================" );
		String requestJsonBody = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);
		LOG.info("\n===>> "+name+" REQUEST ::" + requestJsonBody);

		OneLinkBillPaymentResponse response = null;
	//	BillPaymentInquiryValidationResponse billPaymentInquiryValidationResponse=null;
		if (bindingResult.hasErrors()) {
			// If there are validation errors, create a response with the error details
			Map<String, String> errors = new HashMap<>();
			for (FieldError error : bindingResult.getFieldErrors()) {
				errors.put(error.getField(), error.getDefaultMessage());
			}
			LOG.info(name+" ERROR:" + errors);
			response = new OneLinkBillPaymentResponse(Constants.ResponseCodes.INVALID_DATA,"",request.getReserved());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
		}
		
		LOG.info("bill Controller - Bill Payment");
		try {
			
			//LOG.info("Pin Controller - Validate Request Params");
			//LOG.info("Bill Controller - Bill Service");
			response = billPaymentService.oneLinkBillPayment(httpRequestData, request);
			String ResponseJsonBody = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(response);		
			
			LOG.info("\n===>> "+name+" Response ::" + ResponseJsonBody);
		} catch (Exception ex) {
			LOG.error("{}", ex);
		}
		return ResponseEntity.ok(response);

	}

}
