package com.gateway.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;

import org.apache.catalina.servlet4preview.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gateway.entity.BillerList;
import com.gateway.entity.PaymentLog;
import com.gateway.entity.ProvinceTransaction;
import com.gateway.entity.SubBillersList;
import com.gateway.model.mpay.response.billinquiry.GetVoucherResponse;
import com.gateway.repository.BillerListRepository;
import com.gateway.repository.PaymentLogRepository;
import com.gateway.repository.SubBillerListRepository;
import com.gateway.request.billinquiry.BillInquiryRequest;
import com.gateway.response.BillInquiryValidationResponse;
import com.gateway.response.billinquiryresponse.AdditionalInfo;
import com.gateway.response.billinquiryresponse.BillInquiryResponse;
import com.gateway.response.billinquiryresponse.Info;
import com.gateway.response.billinquiryresponse.TxnInfo;
import com.gateway.service.AuditLoggingService;
import com.gateway.service.BillInquiryService;
import com.gateway.service.ParamsValidatorService;
import com.gateway.service.PaymentLoggingService;
import com.gateway.servicecaller.ServiceCaller;
import com.gateway.utils.BillerConstant;
import com.gateway.utils.Constants;
import com.gateway.utils.Constants.ResponseCodes;
import com.gateway.utils.JwtTokenUtil;
import com.gateway.utils.UtilMethods;

@Service
public class BillInquiryServiceImpl implements BillInquiryService {

	private static final Logger LOG = LoggerFactory.getLogger(BillInquiryServiceImpl.class);

	@Autowired
	private AuditLoggingService auditLoggingService;

	@Autowired
	private PaymentLoggingService paymentLoggingService;

	@Autowired
	private UtilMethods utilMethods;

	@Autowired
	private ServiceCaller serviceCaller;

	@Autowired
	private BillerListRepository billerListRepository;

	@Autowired
	private PaymentLogRepository paymentLogRepository;

	@Autowired
	private ParamsValidatorService paramsValidatorService;

	@Autowired
	private JwtTokenUtil jwtTokenUtil;

	@Autowired
	private SubBillerListRepository subBillerListRepository;

	@Autowired
	private ObjectMapper objectMapper;

	@Override
	public BillInquiryResponse billInquiry(HttpServletRequest httpRequestData, BillInquiryRequest request) {

		LOG.info("================ REQUEST billInquiry ================");
		LOG.info("===>> REQUEST ::" + request.toString());
		BillInquiryResponse billInquiryResponse = null;
		BillerList billerDetail = null;
		SubBillersList subBiller;
		Info info = null;
		String billerId = null;
		String subBillerId = null;
		String rrn = request.getInfo().getRrn();
		String stan = request.getInfo().getStan();

		try {

			BillInquiryValidationResponse billInquiryValidationResponse = null;

			// TODO: here we have changed BillerId()
			billerId = request.getTxnInfo().getBillerId().substring(0, 2);
			subBillerId = request.getTxnInfo().getBillerId().substring(2);
			billerDetail = billerListRepository.findByBillerId(billerId).orElse(null);

			if (billerDetail != null) {
				subBiller = subBillerListRepository.findBySubBillerIdAndBiller(subBillerId, billerDetail).orElse(null);
				if (subBiller != null) {

					billInquiryValidationResponse = billInquiryValidations(httpRequestData, request, billerId);
					if (billInquiryValidationResponse != null) {
						if (billInquiryValidationResponse.getResponseCode().equalsIgnoreCase("00")) {

							if (billerDetail.getBillerName().equalsIgnoreCase("BEOE")) { // BEOE

								switch (subBiller.getSubBillerName()) {
								case BillerConstant.BEOE.BEOE:
									billInquiryResponse = billInquiryBEOE(request, billInquiryValidationResponse);
									break;

								default:
									LOG.info("subBiller does not exists.");
									info = new Info(Constants.ResponseCodes.INVALID_DATA,
											Constants.ResponseDescription.INVALID_INPUT_DATA, rrn, stan);
									billInquiryResponse = new BillInquiryResponse(info, null, null);
									break;

								}
							} else if (billerDetail.getBillerName().equalsIgnoreCase("PRAL")) { // PRAL

								switch (subBiller.getSubBillerName()) {

								case BillerConstant.PRAL.KPPSC:
									billInquiryResponse = billInquiryPRAL(request, billInquiryValidationResponse);
									break;

								default:
									LOG.info("subBiller does not exists.");
									info = new Info(Constants.ResponseCodes.INVALID_DATA,
											Constants.ResponseDescription.INVALID_INPUT_DATA, rrn, stan);
									billInquiryResponse = new BillInquiryResponse(info, null, null);

									break;
								}

							} else {
								info = new Info(Constants.ResponseCodes.INVALID_DATA,
										Constants.ResponseDescription.INVALID_INPUT_DATA, rrn, stan);
								billInquiryResponse = new BillInquiryResponse(info, null, null);
							}

						} else {
							info = new Info(Constants.ResponseCodes.INVALID_DATA,
									Constants.ResponseDescription.INVALID_INPUT_DATA, rrn, stan);
							billInquiryResponse = new BillInquiryResponse(info, null, null);
						}
					} else {
						info = new Info(Constants.ResponseCodes.SERVICE_FAIL,
								Constants.ResponseDescription.SERVICE_FAIL, rrn, stan);
						billInquiryResponse = new BillInquiryResponse(info, null, null);
					}
				} else {
					info = new Info(Constants.ResponseCodes.INVALID_DATA,
							Constants.ResponseDescription.INVALID_INPUT_DATA, rrn, stan);
					billInquiryResponse = new BillInquiryResponse(info, null, null);

				}
			} else {
				info = new Info(Constants.ResponseCodes.INVALID_DATA, Constants.ResponseDescription.INVALID_INPUT_DATA,
						rrn, stan);
				billInquiryResponse = new BillInquiryResponse(info, null, null);
			}

		} catch (Exception ex) {
			LOG.error("Exception in billInquiry method {} " + ex);
		}
		return billInquiryResponse;
	}

	public BillInquiryValidationResponse billInquiryValidations(HttpServletRequest httpRequestData,
			BillInquiryRequest request, String billerId) {

		LOG.info("billInquiryValidations {} ", request.toString());

		BillInquiryValidationResponse response = new BillInquiryValidationResponse();
		String channel = "";
		String username = "";
		String rrn = request.getInfo().getRrn();
		String stan = request.getInfo().getStan();
		try {
			ObjectMapper reqMapper = new ObjectMapper();
			String requestAsString = reqMapper.writeValueAsString(request);
			ProvinceTransaction provinceTransaction = null;
			BillerList billersList = null;

			if (!paramsValidatorService.validateRequestParams(requestAsString)) {
				response = new BillInquiryValidationResponse(Constants.ResponseCodes.INVALID_DATA,
						Constants.ResponseDescription.INVALID_DATA, rrn, stan);
				return response;
			}

			if (request.getTxnInfo().getBillerId() != null || !request.getTxnInfo().getBillerId().isEmpty()) {
				billersList = billerListRepository.findByBillerId(billerId).orElse(null);// biller id
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

			if (request.getTxnInfo().getBillerId() != null || !request.getTxnInfo().getBillerId().isEmpty()) {
				billersList = billerListRepository.findByBillerId(billerId).orElse(null);// biller id
				if (billersList == null) {
					response = new BillInquiryValidationResponse(Constants.ResponseCodes.INVALID_DATA,
							Constants.ResponseDescription.INVALID_DATA, rrn, stan);
					return response;
				}
			}

			response = new BillInquiryValidationResponse("00", "SUCCESS", username, channel, provinceTransaction, rrn,
					stan);

		} catch (Exception ex) {
			LOG.error("Exception in billInquiryValidations  ", ex);

		}
		return response;
	}

	public BillInquiryResponse billInquiryBEOE(BillInquiryRequest request,
			BillInquiryValidationResponse billInquiryValidationResponse) {

		LOG.info("BEOE Bill Inquiry Request {} {} ", request.toString());

		BillInquiryResponse response = null;
		GetVoucherResponse getVoucherResponse = null;
		Info info = null;
		Date strDate = new Date();
		String rrn = request.getInfo().getRrn(); // utilMethods.getRRN();
		String stan = request.getInfo().getStan(); // utilMethods.getStan();
		String transAuthId = request.getInfo().getStan(); // utilMethods.getStan();
		String transactionStatus = "";

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

		String billingMonth = "";
		String dueDAte = "";

		try {

			ArrayList<String> inquiryParams = new ArrayList<String>();
			inquiryParams.add(Constants.MPAY_REQUEST_METHODS.BEOE_BILL_INQUIRY);
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
					if (getVoucherResponse.getResponse().getGetvoucher() != null) {

						requestTotalAmountbdUp = BigDecimal
								.valueOf(
										Double.parseDouble(getVoucherResponse.getResponse().getGetvoucher().getTotal()))
								.setScale(2, RoundingMode.UP);
						amountInDueToDate = utilMethods.bigDecimalToDouble(requestTotalAmountbdUp);
						amountAfterDueDate = amountInDueToDate;

						cnic = getVoucherResponse.getResponse().getGetvoucher().getCnic();
						mobile = getVoucherResponse.getResponse().getGetvoucher().getMobile();
						address = getVoucherResponse.getResponse().getGetvoucher().getAddress();
						name = getVoucherResponse.getResponse().getGetvoucher().getName();
						address = getVoucherResponse.getResponse().getGetvoucher().getAddress();
						billStatus = getVoucherResponse.getResponse().getGetvoucher().getStatus();
						dbAmount = requestTotalAmountbdUp.doubleValue();
						amountPaid = String.format("%012d",
								Integer.parseInt(getVoucherResponse.getResponse().getGetvoucher().getTotal()));

						dbTotal = requestTotalAmountbdUp.doubleValue();
						if (getVoucherResponse.getResponse().getGetvoucher().getStatus()
								.equalsIgnoreCase(Constants.BILL_STATUS.BILL_PAID)) {
							PaymentLog paymentLog = paymentLogRepository.findFirstByBillerNumberAndBillStatus(
									request.getTxnInfo().getBillNumber().trim(), Constants.BILL_STATUS.BILL_PAID);
							if (paymentLog != null) {
								datePaid = paymentLog.getTranDate();
								billingMonth = utilMethods.formatDateString(datePaid);
								billstatus = "P";

							} else {
								info = new Info(Constants.ResponseCodes.PAYMENT_NOT_FOUND,
										Constants.ResponseDescription.PAYMENT_NOT_FOUND, rrn, stan);
								response = new BillInquiryResponse(info, null, null);
								return response;
							}

							transactionStatus = Constants.Status.Success;
						} else if (getVoucherResponse.getResponse().getGetvoucher().getStatus()
								.equalsIgnoreCase(Constants.BILL_STATUS.BILL_UNPAID)) {
							billstatus = "U";
							transAuthId = "";
							// PaymentLog paymentLog =
							// paymentLogRepository.findFirstByBillerNumberAndBillStatus(request.getTxnInfo().getBillNumber().trim(),Constants.BILL_STATUS.BILL_PAID);
							amountPaid = "";
							// datePaid = paymentLog.getTranDate();

							// billingMonth= utilMethods.formatDateString(datePaid);
							datePaid = "";

							transactionStatus = Constants.Status.Pending;

						} else if (getVoucherResponse.getResponse().getGetvoucher().getStatus()
								.equalsIgnoreCase(Constants.BILL_STATUS.BILL_BLOCK)) {
							transactionStatus = Constants.Status.Fail;
							billstatus = "B";
						}
						// hardcoded date
						// dueDAte = utilMethods.getDueDate("20220825");

					}

					TxnInfo txnInfo = new TxnInfo(request.getTxnInfo().getBillerId(),
							request.getTxnInfo().getBillNumber(),
							getVoucherResponse.getResponse().getGetvoucher().getName(), billstatus, dueDAte,
							String.valueOf(amountInDueToDate), String.valueOf(amountAfterDueDate), billingMonth,
							transAuthId, datePaid, amountPaid);

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

			LOG.error("Exception {}", ex);

		} finally {

			Date responseDate = new Date();
			try {

				String requestAsString = objectMapper.writeValueAsString(request);
				String responseAsString = objectMapper.writeValueAsString(response);

				auditLoggingService.auditLog(Constants.ACTIVITY.BillInquiry, response.getInfo().getResponseCode(),
						response.getInfo().getResponseDesc(), requestAsString, responseAsString, strDate, strDate, rrn,
						Long.parseLong(request.getTxnInfo().getBillerId()), request.getTxnInfo().getBillNumber(),
						channel, username);

			} catch (Exception ex) {
				LOG.error("{}", ex);
			}
			try {

				paymentLoggingService.paymentLog(responseDate, responseDate, rrn, stan,
						response.getInfo().getResponseCode(), response.getInfo().getResponseDesc(), cnic, mobile, name,
						request.getTxnInfo().getBillNumber(), request.getTxnInfo().getBillerId(), dbAmount,
						dbTransactionFees, Constants.ACTIVITY.BillInquiry, "", request.getTxnInfo().getBillNumber(),
						transactionStatus, address, transactionFees, dbTax, dbTotal, channel, billStatus,
						request.getTxnInfo().getTranDate(), request.getTxnInfo().getTranTime(), province, transAuthId);

			} catch (Exception ex) {
				LOG.error("{}", ex);
			}

		}
		return response;

	}

	public BillInquiryResponse billInquiryPRAL(BillInquiryRequest request,
			BillInquiryValidationResponse billInquiryValidationResponse) {

		LOG.info("PRAL Bill Inquiry Request {} ", request.toString());

		BillInquiryResponse response = null;
		GetVoucherResponse getVoucherResponse = null;
		Date strDate = new Date();
		String rrn = request.getInfo().getRrn();
		String stan = request.getInfo().getStan();
		String transAuthId = utilMethods.getStan();
		String transactionStatus = "";

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
		// String endpoint = httpServletRequest.getRequestURI();
		String billingMonth = "";
		try {

			ArrayList<String> inquiryParams = new ArrayList<String>();
			inquiryParams.add(Constants.MPAY_REQUEST_METHODS.PRAL_BILL_INQUIRY);
			inquiryParams.add("1000042311130000001");
			inquiryParams.add(rrn);
			inquiryParams.add(stan);

			getVoucherResponse = serviceCaller.get(inquiryParams, GetVoucherResponse.class, rrn,
					Constants.ACTIVITY.BillInquiry);

		} catch (Exception ex) {

			LOG.error("{}", ex);

		} finally {

			LOG.info("PRAL Bill Inquiry Response {}", response);
			Date responseDate = new Date();
			try {

				String requestAsString = objectMapper.writeValueAsString(request);
				String responseAsString = objectMapper.writeValueAsString(response);

				auditLoggingService.auditLog(Constants.ACTIVITY.BillInquiry, response.getInfo().getResponseCode(),
						response.getInfo().getResponseDesc(), requestAsString, responseAsString, strDate, strDate, rrn,
						Long.parseLong(request.getTxnInfo().getBillerId()), request.getTxnInfo().getBillNumber(),
						channel, username);

			} catch (Exception ex) {
				LOG.error("{}", ex);
			}
			try {

				paymentLoggingService.paymentLog(responseDate, responseDate, rrn, stan,
						response.getInfo().getResponseCode(), response.getInfo().getResponseDesc(), cnic, mobile, name,
						request.getTxnInfo().getBillNumber(), request.getTxnInfo().getBillerId(), dbAmount,
						dbTransactionFees, Constants.ACTIVITY.BillInquiry, "", request.getTxnInfo().getBillNumber(),
						transactionStatus, address, transactionFees, dbTax, dbTotal, channel, billStatus,
						request.getTxnInfo().getTranDate(), request.getTxnInfo().getTranTime(), province, transAuthId);

			} catch (Exception ex) {
				LOG.error("Exception {}", ex);
			}

		}
		return response;
	}

}
