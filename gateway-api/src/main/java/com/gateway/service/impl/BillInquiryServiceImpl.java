package com.gateway.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.catalina.servlet4preview.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gateway.entity.AggregatorsList;
import com.gateway.entity.BillersList;
import com.gateway.entity.PaymentLog;
import com.gateway.entity.ProvinceTransaction;
import com.gateway.model.mpay.response.billinquiry.GetVoucherResponse;
import com.gateway.repository.AggregatorsListRepository;
import com.gateway.repository.BillerListRepository;
import com.gateway.repository.PaymentLogRepository;
import com.gateway.repository.ProvinceTransactionDao;
import com.gateway.request.billinquiry.AdditionalInfoRequest;
import com.gateway.request.billinquiry.BillInquiryRequest;
import com.gateway.request.billinquiry.InfoRequest;
import com.gateway.request.billinquiry.OneLinkBillInquiryRequest;
import com.gateway.request.billinquiry.TxnInfoRequest;
import com.gateway.response.BillInquiryValidationResponse;
import com.gateway.response.billinquiryresponse.AdditionalInfo;
import com.gateway.response.billinquiryresponse.BillInquiryResponse;
import com.gateway.response.billinquiryresponse.Info;
import com.gateway.response.billinquiryresponse.OneLinkBillInquiryResponse;
import com.gateway.response.billinquiryresponse.TxnInfo;
import com.gateway.service.AuditLoggingService;
import com.gateway.service.BillInquiryService;
import com.gateway.service.ParamsValidatorService;
import com.gateway.service.PaymentLoggingService;
import com.gateway.servicecaller.ServiceCaller;
import com.gateway.utils.Constants;
import com.gateway.utils.Constants.ResponseCodes;
import com.gateway.utils.JwtTokenUtil;
import com.gateway.utils.UtilMethods;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class BillInquiryServiceImpl implements BillInquiryService {

	private static final Logger LOG = LoggerFactory.getLogger(BillInquiryServiceImpl.class);

	@Autowired
	private AuditLoggingService auditLoggingService;

	@Autowired
	private PaymentLoggingService paymentLoggingService;

	@Autowired
	UtilMethods utilMethods;

	@Autowired
	ServiceCaller serviceCaller;

	@Autowired
	BillerListRepository billerListRepository;

	@Autowired
	PaymentLogRepository paymentLogRepository;

	@Autowired
	ParamsValidatorService paramsValidatorService;

	@Autowired
	ProvinceTransactionDao provinceTransactionDao;

	@Autowired
	private JwtTokenUtil jwtTokenUtil;

	@Autowired
	private AggregatorsListRepository aggregatorsListRepository;

	@Override
	public BillInquiryResponse billInquiry(HttpServletRequest httpRequestData, BillInquiryRequest request) {

		LOG.info("================ REQUEST billInquiry ================");
		LOG.info("===>> REQUEST ::" + request.toString());
		BillInquiryResponse billInquiryResponse = null;
		AggregatorsList aggregatorDetail = null;
		Info info = null;
		String rrn = request.getInfo().getRrn();
		String stan = request.getInfo().getStan();

		try {
			BillInquiryValidationResponse billInquiryValidationResponse = null;
			aggregatorDetail = aggregatorsListRepository.findByAggregatorId(request.getTxnInfo().getAggregatorId());
			if (aggregatorDetail != null) {
				billInquiryValidationResponse = billInquiryValidations(httpRequestData, request);
				if (billInquiryValidationResponse != null) {
					if (billInquiryValidationResponse.getResponseCode().equalsIgnoreCase("00")) {

						if (aggregatorDetail.getAggregatorName().equalsIgnoreCase("BEOE")) { // BEOE
							billInquiryResponse = billInquiryBEOE(request, billInquiryValidationResponse);
						} else if (aggregatorDetail.getAggregatorName().equalsIgnoreCase("PRAL")) { // PRAL
							billInquiryResponse = billInquiryPRAL(request, billInquiryValidationResponse,
									httpRequestData);
						}
//						else if(aggregatorDetail.getAggregatorName().equalsIgnoreCase("NADRA")) { //NADRA
//							billInquiryResponse = billInquiryNADRA(request,billInquiryValidationResponse);
//						}
//						else if(aggregatorDetail.getAggregatorName().equalsIgnoreCase("PTA")) { //PTA
//							billInquiryResponse = billInquiryPTA(request,billInquiryValidationResponse);		
//						}
						else {
							info = new Info(Constants.ResponseCodes.SERVICE_FAIL,
									Constants.ResponseDescription.SERVICE_FAIL, rrn, stan);
							billInquiryResponse = new BillInquiryResponse(info, null, null);
						}

					} else {
						info = new Info(Constants.ResponseCodes.DUPLICATE_TRANSACTION,
								Constants.ResponseDescription.DUPLICATE_TRANSACTION, rrn, stan);
						billInquiryResponse = new BillInquiryResponse(info, null, null);
					}
				} else {
					info = new Info(Constants.ResponseCodes.SERVICE_FAIL, Constants.ResponseDescription.SERVICE_FAIL,
							rrn, stan);
					billInquiryResponse = new BillInquiryResponse(info, null, null);
				}
			} else {
				info = new Info(Constants.ResponseCodes.SERVICE_FAIL, Constants.ResponseDescription.SERVICE_FAIL, rrn,
						stan);
				billInquiryResponse = new BillInquiryResponse(info, null, null);
			}

		} catch (Exception ex) {

		}
		return billInquiryResponse;
	}

	public BillInquiryValidationResponse billInquiryValidations(HttpServletRequest httpRequestData,
			BillInquiryRequest request) {
		BillInquiryValidationResponse response = new BillInquiryValidationResponse();
		String channel = "";
		String username = "";
		String rrn = request.getInfo().getRrn();
		String stan = request.getInfo().getStan();
		try {
			ObjectMapper reqMapper = new ObjectMapper();
			String requestAsString = reqMapper.writeValueAsString(request);
			ProvinceTransaction provinceTransaction = null;
			BillersList billersList = null;
			List<PaymentLog> paymentHistory = null;
			if (!paramsValidatorService.validateRequestParams(requestAsString)) {
				response = new BillInquiryValidationResponse(Constants.ResponseCodes.INVALID_DATA,
						Constants.ResponseDescription.INVALID_DATA, rrn, stan);
				return response;
			}

			if (request.getTxnInfo().getBillerId() != null || !request.getTxnInfo().getBillerId().isEmpty()) {
				billersList = billerListRepository.findByBillerId(request.getTxnInfo().getBillerId());// biller id
				if (billersList == null) {
					response = new BillInquiryValidationResponse(Constants.ResponseCodes.INVALID_DATA,
							Constants.ResponseDescription.INVALID_DATA, rrn, stan);
					return response;
				}
			}

			try {
				String[] result = jwtTokenUtil.getTokenInformation(httpRequestData);
				username = result[0];
				channel = result[1];

			} catch (Exception ex) {
				ex.printStackTrace();
			}
			// RRN a validation First check payment Hsitory
			paymentHistory = paymentLogRepository.findByRrn(rrn);
			if (paymentHistory != null && !paymentHistory.isEmpty()) {
				response = new BillInquiryValidationResponse(Constants.ResponseCodes.DUPLICATE_TRANSACTION,
						Constants.ResponseDescription.DUPLICATE_TRANSACTION, rrn, stan);
				return response;
			}

			if (request.getTxnInfo().getBillerId() != null || !request.getTxnInfo().getBillerId().isEmpty()) {
				billersList = billerListRepository.findByBillerId(request.getTxnInfo().getBillerId());// biller id
				if (billersList == null) {
					response = new BillInquiryValidationResponse(Constants.ResponseCodes.INVALID_DATA,
							Constants.ResponseDescription.INVALID_DATA, rrn, stan);
					return response;
				}
			}

			if (request.getTerminalInfo().getProvince() == null || request.getTerminalInfo().getProvince().isEmpty()) {
				response = new BillInquiryValidationResponse(Constants.ResponseCodes.INVALID_DATA,
						Constants.ResponseDescription.INVALID_DATA, rrn, stan);
				return response;
			} else {
				provinceTransaction = provinceTransactionDao
						.findByProvinceCode(request.getTerminalInfo().getProvince());
			}
			if (provinceTransaction == null) {
				response = new BillInquiryValidationResponse(Constants.ResponseCodes.INVALID_DATA,
						Constants.ResponseDescription.INVALID_DATA, rrn, stan);
				return response;
			}

			response = new BillInquiryValidationResponse("00", "SUCCESS", username, channel, provinceTransaction, rrn,
					stan);

		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return response;

	}

	public BillInquiryResponse billInquiryBEOE(BillInquiryRequest request,
			BillInquiryValidationResponse billInquiryValidationResponse) {

		BillInquiryResponse response = null;
		GetVoucherResponse getVoucherResponse = null;
		Info info = null;
		Date strDate = new Date();
		String rrn = utilMethods.getRRN();
		String stan = utilMethods.getStan();
		String transAuthId = utilMethods.getStan();
		String transactionStatus = "";
		double fedTaxPercent = 1;
		double transactionFees = 0;
		String cnic = "";
		String mobile = "";
		String address = "";
		String name = "";
		String billStatus = "";
		double dbAmount = 0;
		double dbTax = 0;
		double dbTransactionFees = 0;
		double dbTotal = 0;
		String province = "";
		String channel = "";
		String username = "";
		String amountPaid = "";
		String datePaid = "";
		String tranDate="";
		String billingMonth="";
		String dueDAte = "";

		try {

			ProvinceTransaction provinceTransaction = null;
			LOG.info("BEOE Bill Inquiry Request {}", request);

			if (billInquiryValidationResponse != null) {
				provinceTransaction = billInquiryValidationResponse.getProvinceTransaction();
				fedTaxPercent = provinceTransaction.getFedTaxPercent();
				transactionFees = provinceTransaction.getTransactionFees();
				province = provinceTransaction.getProvince();
			}

			ArrayList<String> inquiryParams = new ArrayList<String>();
			inquiryParams.add(Constants.MPAY_REQUEST_METHODS.BILL_INQUIRY);
			inquiryParams.add(request.getTxnInfo().getBillNumber().trim());
			inquiryParams.add(rrn);
			inquiryParams.add(stan);

			getVoucherResponse = serviceCaller.get(inquiryParams, GetVoucherResponse.class, rrn,
					Constants.ACTIVITY.BillInquiry);

			if (getVoucherResponse != null) {
				info = new Info(getVoucherResponse.getResponse().getResponse_code(),
						getVoucherResponse.getResponse().getResponse_desc(), rrn, stan);
				if (getVoucherResponse.getResponse().getResponse_code().equals(ResponseCodes.OK)) {

					double amountInDueToDate = 0;
					double amountAfterDueDate = 0;
					String billstatus = "";
					
					BigDecimal requestTotalAmountbdUp = null;
//					BigDecimal amountInDueToDatebdUp = null;
//					BigDecimal amountAfterDueDatebdUp = null;

					if (getVoucherResponse.getResponse().getGetvoucher() != null) {
						
						
						requestTotalAmountbdUp = BigDecimal
								.valueOf(
										Double.parseDouble(getVoucherResponse.getResponse().getGetvoucher().getTotal()))
								.setScale(2, RoundingMode.UP);
						amountInDueToDate = utilMethods.bigDecimalToDouble(requestTotalAmountbdUp);
						amountAfterDueDate = amountInDueToDate; // provinceTransaction.getLateFees();
						// New work added for upto two decimal place - rounding up
//						amountInDueToDatebdUp = new BigDecimal(amountInDueToDate).setScale(2, RoundingMode.UP);
//						amountAfterDueDatebdUp = new BigDecimal(amountAfterDueDate).setScale(2, RoundingMode.UP);

						cnic = getVoucherResponse.getResponse().getGetvoucher().getCnic();
						mobile = getVoucherResponse.getResponse().getGetvoucher().getMobile();
						address = getVoucherResponse.getResponse().getGetvoucher().getAddress();
						name = getVoucherResponse.getResponse().getGetvoucher().getName();
						address = getVoucherResponse.getResponse().getGetvoucher().getAddress();
						billStatus = getVoucherResponse.getResponse().getGetvoucher().getStatus();
						dbAmount = requestTotalAmountbdUp.doubleValue();
						amountPaid = String.format("%012d",
								Integer.parseInt(getVoucherResponse.getResponse().getGetvoucher().getTotal()));
//						dbTax = (fedTaxPercent / 100) * transactionFees;
//						dbTransactionFees = transactionFees + dbTax;

						dbTotal = requestTotalAmountbdUp.doubleValue();
						if (getVoucherResponse.getResponse().getGetvoucher().getStatus()
								.equalsIgnoreCase(Constants.BILL_STATUS.BILL_PAID)) {
							PaymentLog paymentLog = paymentLogRepository.findFirstByBillerNumberAndBillStatus(request.getTxnInfo().getBillNumber().trim(),Constants.BILL_STATUS.BILL_PAID);
							datePaid = paymentLog.getTranDate();
							billingMonth= utilMethods.formatDateString(datePaid);
							billstatus = "P";
							

							transactionStatus = Constants.Status.Success;
						} else if (getVoucherResponse.getResponse().getGetvoucher().getStatus()
								.equalsIgnoreCase(Constants.BILL_STATUS.BILL_UNPAID)) {
							billstatus = "U";
							transAuthId="";
							//PaymentLog paymentLog = paymentLogRepository.findFirstByBillerNumberAndBillStatus(request.getTxnInfo().getBillNumber().trim(),Constants.BILL_STATUS.BILL_PAID);
							amountPaid="";
							//datePaid = paymentLog.getTranDate();
							
							//billingMonth= utilMethods.formatDateString(datePaid);
							datePaid="";
						
							transactionStatus = Constants.Status.Pending;
							

						} else if (getVoucherResponse.getResponse().getGetvoucher().getStatus()
								.equalsIgnoreCase(Constants.BILL_STATUS.BILL_BLOCK)) {
							transactionStatus = Constants.Status.Fail;
							billstatus = "B";
						}
						// hardcoded date
					//	dueDAte = utilMethods.getDueDate("20220825");
						
					}

					TxnInfo txnInfo = new TxnInfo(request.getTxnInfo().getBillerId(),
							request.getTxnInfo().getBillNumber(),
							getVoucherResponse.getResponse().getGetvoucher().getName(), billstatus, dueDAte,
							String.valueOf(amountInDueToDate), String.valueOf(amountAfterDueDate),billingMonth, transAuthId,
							datePaid,amountPaid);

					AdditionalInfo additionalInfo = new AdditionalInfo(request.getAdditionalInfo().getReserveField1(),

							request.getAdditionalInfo().getReserveField2(),
							request.getAdditionalInfo().getReserveField3(),
							request.getAdditionalInfo().getReserveField4(),
							request.getAdditionalInfo().getReserveField5());

					response = new BillInquiryResponse(info, txnInfo, additionalInfo);

				} else if (getVoucherResponse.getResponse().getResponse_code().equals("404")) {
					info = new Info(Constants.ResponseCodes.CONSUMER_NUMBER_NOT_EXISTS,
							Constants.ResponseDescription.CONSUMER_NUMBER_NOT_EXISTS, rrn, stan);
					response = new BillInquiryResponse(info, null, null);
					transactionStatus = Constants.Status.Fail;

				} else {
					info = new Info(Constants.ResponseCodes.UNKNOWN_ERROR, Constants.ResponseDescription.UNKNOWN_ERROR,
							rrn, stan);
					response = new BillInquiryResponse(info, null, null);
					transactionStatus = Constants.Status.Fail;
				}

			} else {
				info = new Info(Constants.ResponseCodes.SERVICE_FAIL, Constants.ResponseDescription.SERVICE_FAIL, rrn,
						stan);
				response = new BillInquiryResponse(info, null, null);

			}

		} catch (Exception ex) {

			LOG.error("{}", ex);

		} finally {

			Date responseDate = new Date();
			try {

				ObjectMapper reqMapper = new ObjectMapper();
				String requestAsString = reqMapper.writeValueAsString(request);

				ObjectMapper respMapper = new ObjectMapper();
				String responseAsString = respMapper.writeValueAsString(response);

				auditLoggingService.auditLog(Constants.ACTIVITY.BillInquiry, response.getInfo().getResponseCode(),
						response.getInfo().getResponseDesc(), requestAsString, responseAsString, strDate, strDate,
						request.getInfo().getRrn(), Long.parseLong(request.getTxnInfo().getBillerId()),
						request.getTxnInfo().getBillNumber(), channel, username);

			} catch (Exception ex) {
				LOG.error("{}", ex);
			}
			try {

				paymentLoggingService.paymentLog(responseDate, responseDate, rrn, stan,
						response.getInfo().getResponseCode(), response.getInfo().getResponseDesc(), cnic, mobile, name,
						request.getTxnInfo().getBillNumber(), request.getTxnInfo().getBillerId(),
						dbAmount, dbTransactionFees, Constants.ACTIVITY.BillInquiry, "",
						request.getTxnInfo().getBillNumber(), transactionStatus, address, transactionFees, dbTax,
						dbTotal, channel, billStatus, request.getTxnInfo().getTranDate(),
						request.getTxnInfo().getTranTime(), province, transAuthId);

			} catch (Exception ex) {
				LOG.error("{}", ex);
			}

			// Ahmed Ashraf
			// UtilMethods.generalLog("Bill Inquiry Response: " + response, LOG);
			// UtilMethods.generalLog("Bill Inquiry Response DateTime" + responseDate, LOG);
		}
		return response;

	}

	public BillInquiryResponse billInquiryPRAL(BillInquiryRequest request,
			BillInquiryValidationResponse billInquiryValidationResponse, HttpServletRequest httpServletRequest) {

		BillInquiryResponse response = null;
		GetVoucherResponse getVoucherResponse = null;
		Info info = null;
		Date strDate = new Date();
		String rrn = request.getInfo().getRrn();
		String stan = request.getInfo().getStan();
		String transAuthId = utilMethods.getStan();
		String transactionStatus = "";
		double fedTaxPercent = 1;
		double transactionFees = 0;
		String cnic = "";
		String mobile = "";
		String address = "";
		String name = "";
		String billStatus = "";
		double dbAmount = 0;
		double dbTax = 0;
		double dbTransactionFees = 0;
		double dbTotal = 0;
		String province = "";
		String channel = "";
		String username = "";
		String amountPaid = "";
		String datePaid = "";
		String endpoint = httpServletRequest.getRequestURI();
		String billingMonth="";
		try {

			ProvinceTransaction provinceTransaction = null;
			LOG.info("PRAL Bill Inquiry Request {}", request);

			provinceTransaction = billInquiryValidationResponse.getProvinceTransaction();
			fedTaxPercent = provinceTransaction.getFedTaxPercent();
			transactionFees = provinceTransaction.getTransactionFees();
			province = provinceTransaction.getProvince();

			ArrayList<String> inquiryParams = new ArrayList<String>();
			inquiryParams.add(Constants.MPAY_REQUEST_METHODS.BILL_INQUIRY);
			inquiryParams.add(request.getTxnInfo().getBillNumber().trim());
			inquiryParams.add(rrn);
			inquiryParams.add(stan);

			getVoucherResponse = serviceCaller.get(inquiryParams, GetVoucherResponse.class, rrn,
					Constants.ACTIVITY.BillInquiry);

			if (getVoucherResponse != null) {
				info = new Info(getVoucherResponse.getResponse().getResponse_code(),
						getVoucherResponse.getResponse().getResponse_desc(), rrn, stan);
				if (getVoucherResponse.getResponse().getResponse_code().equals(ResponseCodes.OK)) {

					double amountInDueToDate = 0;
					double amountAfterDueDate = 0;
					String billstatus = "";
					String dueDAte = "";
					BigDecimal requestTotalAmountbdUp = null;
					BigDecimal amountInDueToDatebdUp = null;
					BigDecimal amountAfterDueDatebdUp = null;

					if (getVoucherResponse.getResponse().getGetvoucher() != null) {

						requestTotalAmountbdUp = BigDecimal
								.valueOf(
										Double.parseDouble(getVoucherResponse.getResponse().getGetvoucher().getTotal()))
								.setScale(2, RoundingMode.UP);
						amountInDueToDate = requestTotalAmountbdUp.doubleValue()
								+ (transactionFees + ((fedTaxPercent / 100) * transactionFees));
						amountAfterDueDate = amountInDueToDate + provinceTransaction.getLateFees();
						// New work added for upto two decimal place - rounding up
						amountInDueToDatebdUp = new BigDecimal(amountInDueToDate).setScale(2, RoundingMode.UP);
						amountAfterDueDatebdUp = new BigDecimal(amountAfterDueDate).setScale(2, RoundingMode.UP);

						cnic = getVoucherResponse.getResponse().getGetvoucher().getCnic();
						mobile = getVoucherResponse.getResponse().getGetvoucher().getMobile();
						address = getVoucherResponse.getResponse().getGetvoucher().getAddress();
						name = getVoucherResponse.getResponse().getGetvoucher().getName();
						address = getVoucherResponse.getResponse().getGetvoucher().getAddress();
						billStatus = getVoucherResponse.getResponse().getGetvoucher().getStatus();
						dbAmount = requestTotalAmountbdUp.doubleValue();
						dbTax = (fedTaxPercent / 100) * transactionFees;
						dbTransactionFees = transactionFees + dbTax;
						dbTotal = amountInDueToDatebdUp.doubleValue();
						if (getVoucherResponse.getResponse().getGetvoucher().getStatus()
								.equalsIgnoreCase(Constants.BILL_STATUS.BILL_PAID)) {
							billstatus = "P";
							transactionStatus = Constants.Status.Success;
						} else if (getVoucherResponse.getResponse().getGetvoucher().getStatus()
								.equalsIgnoreCase(Constants.BILL_STATUS.BILL_UNPAID)) {
							billstatus = "U";
							transactionStatus = Constants.Status.Pending;

						} else if (getVoucherResponse.getResponse().getGetvoucher().getStatus()
								.equalsIgnoreCase(Constants.BILL_STATUS.BILL_BLOCK)) {
							transactionStatus = Constants.Status.Fail;
							billstatus = "B";
						}
						if (endpoint.equals("/api/v1/bill/Payments/BillInquiry"))
							dueDAte = utilMethods.getDueDate("20220825");
						else
							dueDAte = utilMethods.getDueDate(request.getTxnInfo().getTranDate());
					}

					TxnInfo txnInfo = new TxnInfo(request.getTxnInfo().getBillerId(),
							request.getTxnInfo().getBillNumber(),
							getVoucherResponse.getResponse().getGetvoucher().getName(), billstatus, dueDAte,
							String.valueOf(amountInDueToDate), String.valueOf(amountAfterDueDate),billingMonth, transAuthId,
							datePaid,amountPaid);
					AdditionalInfo additionalInfo = new AdditionalInfo(request.getAdditionalInfo().getReserveField1(),
							request.getAdditionalInfo().getReserveField2(),
							request.getAdditionalInfo().getReserveField3(),
							request.getAdditionalInfo().getReserveField4(),
							request.getAdditionalInfo().getReserveField5());

					response = new BillInquiryResponse(info, txnInfo, additionalInfo);

				} else if (getVoucherResponse.getResponse().getResponse_code().equals("404")) {
					info = new Info(Constants.ResponseCodes.CONSUMER_NUMBER_NOT_EXISTS,
							Constants.ResponseDescription.CONSUMER_NUMBER_NOT_EXISTS, rrn, stan);
					response = new BillInquiryResponse(info, null, null);
					transactionStatus = Constants.Status.Fail;

				} else {
					info = new Info(Constants.ResponseCodes.UNKNOWN_ERROR, Constants.ResponseDescription.UNKNOWN_ERROR,
							rrn, stan);
					response = new BillInquiryResponse(info, null, null);
					transactionStatus = Constants.Status.Fail;
				}

			} else {
				info = new Info(Constants.ResponseCodes.SERVICE_FAIL, Constants.ResponseDescription.SERVICE_FAIL, rrn,
						stan);
				response = new BillInquiryResponse(info, null, null);

			}

		} catch (Exception ex) {

			LOG.error("{}", ex);

		} finally {

			LOG.info("Bill Inquiry Response {}", response);
			Date responseDate = new Date();
			try {

				ObjectMapper reqMapper = new ObjectMapper();
				String requestAsString = reqMapper.writeValueAsString(request);

				ObjectMapper respMapper = new ObjectMapper();
				String responseAsString = respMapper.writeValueAsString(response);

				auditLoggingService.auditLog(Constants.ACTIVITY.BillInquiry, response.getInfo().getResponseCode(),
						response.getInfo().getResponseDesc(), requestAsString, responseAsString, strDate, strDate,
						request.getInfo().getRrn(), Long.parseLong(request.getTxnInfo().getBillerId()),
						request.getTxnInfo().getBillNumber(), channel, username);

			} catch (Exception ex) {
				LOG.error("{}", ex);
			}
			try {

				paymentLoggingService.paymentLog(responseDate, responseDate, rrn, stan,
						response.getInfo().getResponseCode(), response.getInfo().getResponseDesc(), cnic, mobile, name,
						request.getTxnInfo().getBillNumber(), request.getTxnInfo().getBillerId(),
						dbAmount, dbTransactionFees, Constants.ACTIVITY.BillInquiry, "",
						request.getTxnInfo().getBillNumber(), transactionStatus, address, transactionFees, dbTax,
						dbTotal, channel, billStatus, request.getTxnInfo().getTranDate(),
						request.getTxnInfo().getTranTime(), province, transAuthId);
				
				

			} catch (Exception ex) {
				LOG.error("{}", ex);
			}

			// Ahmed Ashraf
			// UtilMethods.generalLog("OUT - Bill Inquiry Response: " + response, LOG);
			// UtilMethods.generalLog("OUT - Bill Inquiry " + responseDate, LOG);
		}
		return response;
	}

	@Override
	public OneLinkBillInquiryResponse oneLinkBillInquiry(HttpServletRequest httpRequestData,
			OneLinkBillInquiryRequest request) {

		LOG.info("================ REQUEST oneLinkBillInquiry Impl ================");

		BillInquiryRequest billInquiryRequest = new BillInquiryRequest();
		InfoRequest infoRequest = new InfoRequest();
		TxnInfoRequest txnInfo = new TxnInfoRequest();
		AggregatorsList aggregatorDetail = null;
		;
		AdditionalInfoRequest additionalInfoRequest = null;
		BillInquiryResponse billInquiryResponse = null;
		OneLinkBillInquiryResponse oneLinkBillInquiryResponse = null;
		Info info = null;
		String rrn = utilMethods.getRRN();
		String stan = utilMethods.getStan();

		try {

			String aggregatorId = UtilMethods.getAggregatorId(request.getConsumerNumber());
			LOG.info("Aggregator id:" + aggregatorId);
			String billerNumber = UtilMethods.getBillerNumber(request.getConsumerNumber());
			aggregatorDetail = aggregatorsListRepository.findByAggregatorId(aggregatorId);
			txnInfo.setAggregatorId(aggregatorId);
			txnInfo.setBillNumber(billerNumber);
			txnInfo.setBillerId(aggregatorId);
			billInquiryRequest.setTxnInfo(txnInfo);
			infoRequest.setRrn(rrn);
			infoRequest.setStan(stan);
			additionalInfoRequest = new AdditionalInfoRequest(request.getReserved(), request.getReserved(),
					request.getReserved(), request.getReserved(), request.getReserved());
			billInquiryRequest.setAdditionalInfo(additionalInfoRequest);
			billInquiryRequest.setInfo(infoRequest);
			billInquiryRequest.getTxnInfo().setBillNumber(billerNumber);
			LOG.info("Bill Number" + billInquiryRequest.getTxnInfo().getBillNumber());

			if (aggregatorDetail != null) {

				if (aggregatorDetail.getAggregatorName().equalsIgnoreCase("BEOE")) { // BEOE
					billInquiryResponse = billInquiryBEOE(billInquiryRequest, null);
					if (billInquiryResponse.getInfo().getResponseCode().equals("00")) {
						oneLinkBillInquiryResponse = billInquiryResponseToOneLinkResponse(billInquiryResponse);
					} else {
						info = billInquiryResponse.getInfo();
						oneLinkBillInquiryResponse = new OneLinkBillInquiryResponse(info.getResponseCode(), "", "", "", "", "", "", "", "", "", "", request.getReserved());
						
//						oneLinkBillInquiryResponse = new OneLinkBillInquiryResponse(info.getResponseCode(),
//								info.getResponseDesc());
					}

				} else {
					oneLinkBillInquiryResponse = new OneLinkBillInquiryResponse(Constants.ResponseCodes.SERVICE_FAIL, "", "", "", "", "", "", "", "", "", "", request.getReserved());
					
//					oneLinkBillInquiryResponse = new OneLinkBillInquiryResponse(Constants.ResponseCodes.SERVICE_FAIL,
//							Constants.ResponseDescription.SERVICE_FAIL);
				}
			} else {
				oneLinkBillInquiryResponse = new OneLinkBillInquiryResponse(Constants.ResponseCodes.INVALID_DATA, "", "", "", "", "", "", "", "", "", "", request.getReserved());
				
//				oneLinkBillInquiryResponse = new OneLinkBillInquiryResponse(Constants.ResponseCodes.INVALID_DATA,
//						Constants.ResponseDescription.INVALID_DATA);
			}

		} catch (Exception ex) {

			LOG.error("Exception {}", ex);
		}

		// return billInquiryResponse;
		return oneLinkBillInquiryResponse;
	}

	public OneLinkBillInquiryResponse billInquiryResponseToOneLinkResponse(BillInquiryResponse billInquiryResponse) {

		OneLinkBillInquiryResponse oneLinkBillInquiryResponse = null;
		if (billInquiryResponse != null) {
			oneLinkBillInquiryResponse = new OneLinkBillInquiryResponse();
			oneLinkBillInquiryResponse.setResponseCode(billInquiryResponse.getInfo().getResponseCode());
			oneLinkBillInquiryResponse.setConsumerDetail(billInquiryResponse.getTxnInfo().getConsumerName());
			oneLinkBillInquiryResponse.setBillStatus(billInquiryResponse.getTxnInfo().getBillStatus());
			oneLinkBillInquiryResponse.setDueDate(billInquiryResponse.getTxnInfo().getDueDate());
			oneLinkBillInquiryResponse.setAmountWithinDueDate(
					utilMethods.convertAmountToISOFormat(billInquiryResponse.getTxnInfo().getAmountwithinduedate()));
			oneLinkBillInquiryResponse.setAmountAfterDueDate(
					utilMethods.convertAmountToISOFormat(billInquiryResponse.getTxnInfo().getAmountafterduedate()));
			oneLinkBillInquiryResponse.setBillingMonth(billInquiryResponse.getTxnInfo().getBillingMonth());
			oneLinkBillInquiryResponse.setDatePaid(billInquiryResponse.getTxnInfo().getDatePaid());
			oneLinkBillInquiryResponse.setAmountPaid(billInquiryResponse.getTxnInfo().getAmountPaid());
			oneLinkBillInquiryResponse.setTranAuthId(billInquiryResponse.getTxnInfo().getTranAuthId());
			oneLinkBillInquiryResponse.setReserved(billInquiryResponse.getAdditionalInfo().getReserveField1());
		}
		return oneLinkBillInquiryResponse;

	}

	/*
	 * public BillInquiryResponse billInquiryNADRA(BillInquiryRequest
	 * request,BillInquiryValidationResponse billInquiryValidationResponse) {
	 * 
	 * LOG.info("Inside method Bill Inquiry"); BillInquiryResponse response = null;
	 * GetVoucherResponse getVoucherResponse = null; List<PaymentLog> paymentHistory
	 * =null; Info info = null; Date strDate = new Date(); String rrn =
	 * request.getInfo().getRrn(); String stan = request.getInfo().getStan(); String
	 * transAuthId = utilMethods.getStan(); String transactionStatus = ""; double
	 * fedTaxPercent = 1; double transactionFees = 0; String cnic = ""; String
	 * mobile = ""; String address = ""; String name = ""; String billStatus = "";
	 * double dbAmount = 0; double dbTax = 0; double dbTransactionFees = 0; double
	 * dbTotal = 0; String province = ""; String channel = ""; String username = "";
	 * 
	 * try {
	 * 
	 * 
	 * fedTaxPercent = provinceTransaction.getFedTaxPercent(); transactionFees =
	 * provinceTransaction.getTransactionFees(); province =
	 * provinceTransaction.getProvince();
	 * 
	 * ArrayList<String> inquiryParams = new ArrayList<String>();
	 * inquiryParams.add(Constants.MPAY_REQUEST_METHODS.BILL_INQUIRY);
	 * inquiryParams.add(request.getTxnInfo().getBillNumber().trim());
	 * inquiryParams.add(rrn); inquiryParams.add(stan);
	 * 
	 * getVoucherResponse = serviceCaller.get(inquiryParams,
	 * GetVoucherResponse.class, rrn, Constants.ACTIVITY.BillInquiry);
	 * 
	 * if (getVoucherResponse != null) { info = new
	 * Info(getVoucherResponse.getResponse().getResponse_code(),
	 * getVoucherResponse.getResponse().getResponse_desc(), rrn, stan); if
	 * (getVoucherResponse.getResponse().getResponse_code().equals(ResponseCodes.OK)
	 * ) {
	 * 
	 * double amountInDueToDate = 0; double amountAfterDueDate = 0; String
	 * billstatus = ""; String dueDAte = ""; BigDecimal requestTotalAmountbdUp
	 * =null; BigDecimal amountInDueToDatebdUp = null; BigDecimal
	 * amountAfterDueDatebdUp = null;
	 * 
	 * if (getVoucherResponse.getResponse().getGetvoucher() != null) {
	 * 
	 * requestTotalAmountbdUp = new BigDecimal(Double
	 * .parseDouble(getVoucherResponse.getResponse().getGetvoucher().getTotal())).
	 * setScale(2,RoundingMode.UP); amountInDueToDate =
	 * requestTotalAmountbdUp.doubleValue()+ (transactionFees + ((fedTaxPercent /
	 * 100) *transactionFees)); amountAfterDueDate = amountInDueToDate +
	 * provinceTransaction.getLateFees(); //New work added for upto two decimal
	 * place - rounding up amountInDueToDatebdUp = new
	 * BigDecimal(amountInDueToDate).setScale(2,RoundingMode.UP);
	 * amountAfterDueDatebdUp = new
	 * BigDecimal(amountAfterDueDate).setScale(2,RoundingMode.UP);
	 * 
	 * cnic = getVoucherResponse.getResponse().getGetvoucher().getCnic(); mobile =
	 * getVoucherResponse.getResponse().getGetvoucher().getMobile(); address =
	 * getVoucherResponse.getResponse().getGetvoucher().getAddress(); name =
	 * getVoucherResponse.getResponse().getGetvoucher().getName(); address =
	 * getVoucherResponse.getResponse().getGetvoucher().getAddress(); billStatus =
	 * getVoucherResponse.getResponse().getGetvoucher().getStatus(); dbAmount =
	 * requestTotalAmountbdUp.doubleValue(); dbTax = (fedTaxPercent / 100) *
	 * transactionFees; dbTransactionFees = transactionFees + dbTax; dbTotal =
	 * amountInDueToDatebdUp.doubleValue(); if
	 * (getVoucherResponse.getResponse().getGetvoucher().getStatus().
	 * equalsIgnoreCase(Constants.BILL_STATUS.BILL_PAID)) { billstatus = "P";
	 * transactionStatus = Constants.Status.Success; } else if
	 * (getVoucherResponse.getResponse().getGetvoucher().getStatus()
	 * .equalsIgnoreCase(Constants.BILL_STATUS.BILL_UNPAID)) { billstatus = "U";
	 * transactionStatus = Constants.Status.Pending;
	 * 
	 * } else if (getVoucherResponse.getResponse().getGetvoucher().getStatus()
	 * .equalsIgnoreCase(Constants.BILL_STATUS.BILL_BLOCK)) { transactionStatus =
	 * Constants.Status.Fail; billstatus = "B"; } dueDAte =
	 * utilMethods.getDueDate(request.getTxnInfo().getTranDate()); }
	 * 
	 * TxnInfo txnInfo = new TxnInfo(request.getTxnInfo().getBillerId(),
	 * request.getTxnInfo().getBillNumber(),
	 * getVoucherResponse.getResponse().getGetvoucher().getName(), billstatus,
	 * dueDAte, String.valueOf(amountInDueToDatebdUp.doubleValue()),
	 * String.valueOf(amountAfterDueDatebdUp.doubleValue()), transAuthId);
	 * AdditionalInfo additionalInfo = new
	 * AdditionalInfo(request.getAdditionalInfo().getReserveField1(),
	 * request.getAdditionalInfo().getReserveField2(),
	 * request.getAdditionalInfo().getReserveField3(),
	 * request.getAdditionalInfo().getReserveField4(),
	 * request.getAdditionalInfo().getReserveField5());
	 * 
	 * response = new BillInquiryResponse(info, txnInfo, additionalInfo);
	 * 
	 * } else if (getVoucherResponse.getResponse().getResponse_code().equals("404"))
	 * { info = new Info(Constants.ResponseCodes.CONSUMER_NUMBER_NOT_EXISTS,
	 * Constants.ResponseDescription.CONSUMER_NUMBER_NOT_EXISTS, rrn, stan);
	 * response = new BillInquiryResponse(info, null, null); transactionStatus =
	 * Constants.Status.Fail;
	 * 
	 * } else { info = new Info(Constants.ResponseCodes.UNKNOWN_ERROR,
	 * Constants.ResponseDescription.UNKNOWN_ERROR, rrn, stan); response = new
	 * BillInquiryResponse(info, null, null); transactionStatus =
	 * Constants.Status.Fail; }
	 * 
	 * } else { info = new Info(Constants.ResponseCodes.UNABLE_TO_PROCESS,
	 * Constants.ResponseDescription.UNABLE_TO_PROCESS, rrn, stan); response = new
	 * BillInquiryResponse(info, null, null);
	 * 
	 * }
	 * 
	 * } catch (Exception ex) {
	 * 
	 * LOG.error("{}", ex);
	 * 
	 * } finally {
	 * 
	 * LOG.info("Bill Inquiry Response {}", response); Date responseDate = new
	 * Date(); try {
	 * 
	 * ObjectMapper reqMapper = new ObjectMapper(); String requestAsString =
	 * reqMapper.writeValueAsString(request);
	 * 
	 * ObjectMapper respMapper = new ObjectMapper(); String responseAsString =
	 * respMapper.writeValueAsString(response);
	 * 
	 * auditLoggingService.auditLog(Constants.ACTIVITY.BillInquiry,
	 * response.getInfo().getResponseCode(), response.getInfo().getResponseDesc(),
	 * requestAsString, responseAsString, strDate, strDate,
	 * request.getInfo().getRrn(),Long.parseLong(request.getTxnInfo().getBillerId())
	 * ,request.getTxnInfo().getBillNumber(),channel,username);
	 * 
	 * } catch (Exception ex) { LOG.error("{}", ex); } try {
	 * 
	 * paymentLoggingService.paymentLog(responseDate, responseDate, rrn, stan,
	 * response.getInfo().getResponseCode(), response.getInfo().getResponseDesc(),
	 * cnic, mobile, name, request.getTxnInfo().getBillNumber(),
	 * Long.parseLong(request.getTxnInfo().getBillerId()), dbAmount,
	 * dbTransactionFees, Constants.ACTIVITY.BillInquiry, "",
	 * request.getTxnInfo().getBillNumber(), transactionStatus, address,
	 * transactionFees, dbTax, dbTotal, channel, billStatus,
	 * request.getTxnInfo().getTranDate(), request.getTxnInfo().getTranTime(),
	 * province,transAuthId);
	 * 
	 * } catch (Exception ex) { LOG.error("{}", ex); }
	 * 
	 * UtilMethods.generalLog("OUT -  Bill Inquiry Response: " + response, LOG);
	 * UtilMethods.generalLog("OUT -  Bill Inquiry " + responseDate, LOG); } return
	 * response;
	 * 
	 * }
	 * 
	 * 
	 * public BillInquiryResponse billInquiryPTA(HttpServletRequest
	 * httpRequestData,BillInquiryRequest request,BillInquiryValidationResponse
	 * billInquiryValidationResponse) {
	 * 
	 * LOG.info("Inside method Bill Inquiry"); BillInquiryResponse response = null;
	 * GetVoucherResponse getVoucherResponse = null; List<PaymentLog> paymentHistory
	 * =null; Info info = null; Date strDate = new Date(); String rrn =
	 * request.getInfo().getRrn(); String stan = request.getInfo().getStan(); String
	 * transAuthId = utilMethods.getStan(); String transactionStatus = ""; double
	 * fedTaxPercent = 1; double transactionFees = 0; String cnic = ""; String
	 * mobile = ""; String address = ""; String name = ""; String billStatus = "";
	 * double dbAmount = 0; double dbTax = 0; double dbTransactionFees = 0; double
	 * dbTotal = 0; String province = ""; String channel = ""; String username = "";
	 * 
	 * try { UtilMethods.generalLog("IN - billInquiry  " + strDate, LOG);
	 * LOG.info("Calling Get Voucher");
	 * 
	 * ObjectMapper reqMapper = new ObjectMapper(); String requestAsString =
	 * reqMapper.writeValueAsString(request); ProvinceTransaction
	 * provinceTransaction = null; BillersList billersList = null;
	 * LOG.info("Bill Inquiry Request {}", request); if
	 * (!paramsValidatorService.validateRequestParams(requestAsString)) {
	 * 
	 * info = new Info(Constants.ResponseCodes.INVALID_DATA,
	 * Constants.ResponseDescription.INVALID_DATA, rrn, stan); response = new
	 * BillInquiryResponse(info, null, null); return response; }
	 * 
	 * try { String[] result = jwtTokenUtil.getTokenInformation(httpRequestData);
	 * username=result[0]; channel=result[1];
	 * 
	 * }catch(Exception ex) { ex.printStackTrace(); }
	 * 
	 * //RRN a validation First check payment Hsitory paymentHistory =
	 * paymentLogRepository.findByRrn(rrn); if(paymentHistory!=null &&
	 * !paymentHistory.isEmpty()) { info = new
	 * Info(Constants.ResponseCodes.DUPLICATE_TRANSACTION,
	 * Constants.ResponseDescription.DUPLICATE_TRANSACTION, rrn, stan); response =
	 * new BillInquiryResponse(info, null, null); return response; }
	 * 
	 * 
	 * if (request.getTxnInfo().getBillerId() != null ||
	 * !request.getTxnInfo().getBillerId().isEmpty()) { billersList =
	 * billerListRepository.getBiller(request.getTxnInfo().getBillerId());// biller
	 * id if (billersList == null) { info = new
	 * Info(Constants.ResponseCodes.INVALID_DATA,
	 * Constants.ResponseDescription.INVALID_DATA, rrn, stan); response = new
	 * BillInquiryResponse(info, null, null); return response; } }
	 * 
	 * if (request.getTerminalInfo().getProvince() == null ||
	 * request.getTerminalInfo().getProvince().isEmpty()) { info = new
	 * Info(Constants.ResponseCodes.INVALID_DATA,
	 * Constants.ResponseDescription.INVALID_DATA, rrn, stan); response = new
	 * BillInquiryResponse(info, null, null); return response; } else {
	 * provinceTransaction = provinceTransactionDao
	 * .getProvinceTransaction(request.getTerminalInfo().getProvince()); } if
	 * (provinceTransaction == null) { info = new
	 * Info(Constants.ResponseCodes.INVALID_DATA,
	 * Constants.ResponseDescription.INVALID_DATA, rrn, stan); response = new
	 * BillInquiryResponse(info, null, null); return response; }
	 * 
	 * fedTaxPercent = provinceTransaction.getFedTaxPercent(); transactionFees =
	 * provinceTransaction.getTransactionFees(); province =
	 * provinceTransaction.getProvince();
	 * 
	 * ArrayList<String> inquiryParams = new ArrayList<String>();
	 * inquiryParams.add(Constants.MPAY_REQUEST_METHODS.BILL_INQUIRY);
	 * inquiryParams.add(request.getTxnInfo().getBillNumber().trim());
	 * inquiryParams.add(rrn); inquiryParams.add(stan);
	 * 
	 * getVoucherResponse = serviceCaller.get(inquiryParams,
	 * GetVoucherResponse.class, rrn, Constants.ACTIVITY.BillInquiry);
	 * 
	 * if (getVoucherResponse != null) { info = new
	 * Info(getVoucherResponse.getResponse().getResponse_code(),
	 * getVoucherResponse.getResponse().getResponse_desc(), rrn, stan); if
	 * (getVoucherResponse.getResponse().getResponse_code().equals(ResponseCodes.OK)
	 * ) {
	 * 
	 * double amountInDueToDate = 0; double amountAfterDueDate = 0; String
	 * billstatus = ""; String dueDAte = ""; BigDecimal requestTotalAmountbdUp
	 * =null; BigDecimal amountInDueToDatebdUp = null; BigDecimal
	 * amountAfterDueDatebdUp = null;
	 * 
	 * if (getVoucherResponse.getResponse().getGetvoucher() != null) {
	 * 
	 * requestTotalAmountbdUp = new BigDecimal(Double
	 * .parseDouble(getVoucherResponse.getResponse().getGetvoucher().getTotal())).
	 * setScale(2,RoundingMode.UP); amountInDueToDate =
	 * requestTotalAmountbdUp.doubleValue()+ (transactionFees + ((fedTaxPercent /
	 * 100) *transactionFees)); amountAfterDueDate = amountInDueToDate +
	 * provinceTransaction.getLateFees(); //New work added for upto two decimal
	 * place - rounding up amountInDueToDatebdUp = new
	 * BigDecimal(amountInDueToDate).setScale(2,RoundingMode.UP);
	 * amountAfterDueDatebdUp = new
	 * BigDecimal(amountAfterDueDate).setScale(2,RoundingMode.UP);
	 * 
	 * cnic = getVoucherResponse.getResponse().getGetvoucher().getCnic(); mobile =
	 * getVoucherResponse.getResponse().getGetvoucher().getMobile(); address =
	 * getVoucherResponse.getResponse().getGetvoucher().getAddress(); name =
	 * getVoucherResponse.getResponse().getGetvoucher().getName(); address =
	 * getVoucherResponse.getResponse().getGetvoucher().getAddress(); billStatus =
	 * getVoucherResponse.getResponse().getGetvoucher().getStatus(); dbAmount =
	 * requestTotalAmountbdUp.doubleValue(); dbTax = (fedTaxPercent / 100) *
	 * transactionFees; dbTransactionFees = transactionFees + dbTax; dbTotal =
	 * amountInDueToDatebdUp.doubleValue(); if
	 * (getVoucherResponse.getResponse().getGetvoucher().getStatus().
	 * equalsIgnoreCase(Constants.BILL_STATUS.BILL_PAID)) { billstatus = "P";
	 * transactionStatus = Constants.Status.Success; } else if
	 * (getVoucherResponse.getResponse().getGetvoucher().getStatus()
	 * .equalsIgnoreCase(Constants.BILL_STATUS.BILL_UNPAID)) { billstatus = "U";
	 * transactionStatus = Constants.Status.Pending;
	 * 
	 * } else if (getVoucherResponse.getResponse().getGetvoucher().getStatus()
	 * .equalsIgnoreCase(Constants.BILL_STATUS.BILL_BLOCK)) { transactionStatus =
	 * Constants.Status.Fail; billstatus = "B"; } dueDAte =
	 * utilMethods.getDueDate(request.getTxnInfo().getTranDate()); }
	 * 
	 * TxnInfo txnInfo = new TxnInfo(request.getTxnInfo().getBillerId(),
	 * request.getTxnInfo().getBillNumber(),
	 * getVoucherResponse.getResponse().getGetvoucher().getName(), billstatus,
	 * dueDAte, String.valueOf(amountInDueToDatebdUp.doubleValue()),
	 * String.valueOf(amountAfterDueDatebdUp.doubleValue()), transAuthId);
	 * AdditionalInfo additionalInfo = new
	 * AdditionalInfo(request.getAdditionalInfo().getReserveField1(),
	 * request.getAdditionalInfo().getReserveField2(),
	 * request.getAdditionalInfo().getReserveField3(),
	 * request.getAdditionalInfo().getReserveField4(),
	 * request.getAdditionalInfo().getReserveField5());
	 * 
	 * response = new BillInquiryResponse(info, txnInfo, additionalInfo);
	 * 
	 * } else if (getVoucherResponse.getResponse().getResponse_code().equals("404"))
	 * { info = new Info(Constants.ResponseCodes.CONSUMER_NUMBER_NOT_EXISTS,
	 * Constants.ResponseDescription.CONSUMER_NUMBER_NOT_EXISTS, rrn, stan);
	 * response = new BillInquiryResponse(info, null, null); transactionStatus =
	 * Constants.Status.Fail;
	 * 
	 * } else { info = new Info(Constants.ResponseCodes.UNKNOWN_ERROR,
	 * Constants.ResponseDescription.UNKNOWN_ERROR, rrn, stan); response = new
	 * BillInquiryResponse(info, null, null); transactionStatus =
	 * Constants.Status.Fail; }
	 * 
	 * } else { info = new Info(Constants.ResponseCodes.UNABLE_TO_PROCESS,
	 * Constants.ResponseDescription.UNABLE_TO_PROCESS, rrn, stan); response = new
	 * BillInquiryResponse(info, null, null);
	 * 
	 * }
	 * 
	 * } catch (Exception ex) {
	 * 
	 * LOG.error("{}", ex);
	 * 
	 * } finally {
	 * 
	 * LOG.info("Bill Inquiry Response {}", response); Date responseDate = new
	 * Date(); try {
	 * 
	 * ObjectMapper reqMapper = new ObjectMapper(); String requestAsString =
	 * reqMapper.writeValueAsString(request);
	 * 
	 * ObjectMapper respMapper = new ObjectMapper(); String responseAsString =
	 * respMapper.writeValueAsString(response);
	 * 
	 * auditLoggingService.auditLog(Constants.ACTIVITY.BillInquiry,
	 * response.getInfo().getResponseCode(), response.getInfo().getResponseDesc(),
	 * requestAsString, responseAsString, strDate, strDate,
	 * request.getInfo().getRrn(),Long.parseLong(request.getTxnInfo().getBillerId())
	 * ,request.getTxnInfo().getBillNumber(),channel,username);
	 * 
	 * } catch (Exception ex) { LOG.error("{}", ex); } try {
	 * 
	 * paymentLoggingService.paymentLog(responseDate, responseDate, rrn, stan,
	 * response.getInfo().getResponseCode(), response.getInfo().getResponseDesc(),
	 * cnic, mobile, name, request.getTxnInfo().getBillNumber(),
	 * Long.parseLong(request.getTxnInfo().getBillerId()), dbAmount,
	 * dbTransactionFees, Constants.ACTIVITY.BillInquiry, "",
	 * request.getTxnInfo().getBillNumber(), transactionStatus, address,
	 * transactionFees, dbTax, dbTotal, channel, billStatus,
	 * request.getTxnInfo().getTranDate(), request.getTxnInfo().getTranTime(),
	 * province,transAuthId);
	 * 
	 * } catch (Exception ex) { LOG.error("{}", ex); }
	 * 
	 * UtilMethods.generalLog("OUT -  Bill Inquiry Response: " + response, LOG);
	 * UtilMethods.generalLog("OUT -  Bill Inquiry " + responseDate, LOG); } return
	 * response;
	 * 
	 * }
	 * 
	 */

}
