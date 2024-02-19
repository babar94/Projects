package com.gateway.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gateway.entity.BillerConfiguration;
import com.gateway.entity.PaymentLog;
import com.gateway.entity.SubBillersList;
import com.gateway.model.mpay.response.billinquiry.GetVoucherResponse;
import com.gateway.repository.BillerConfigurationRepo;
import com.gateway.repository.PaymentLogRepository;
import com.gateway.repository.ProvinceTransactionDao;
import com.gateway.repository.SubBillerListRepository;
import com.gateway.request.paymentinquiry.PaymentInquiryRequest;
import com.gateway.response.BillPaymentInquiryValidationResponse;
import com.gateway.response.billerlistresponse.BillerListResponse;
import com.gateway.response.billerlistresponse.Billers;
import com.gateway.response.billerlistresponse.InfoBiller;
import com.gateway.response.billerlistresponse.TxnInfoBiller;
import com.gateway.response.billinquiryresponse.BillInquiryResponse;
import com.gateway.response.billinquiryresponse.Info;
import com.gateway.response.paymentinquiryresponse.AdditionalInfoPayInq;
import com.gateway.response.paymentinquiryresponse.InfoPayInq;
import com.gateway.response.paymentinquiryresponse.PaymentInquiryResponse;
import com.gateway.response.paymentinquiryresponse.TxnInfoPayInq;
import com.gateway.service.AuditLoggingService;
import com.gateway.service.BillDetailsService;
import com.gateway.service.ParamsValidatorService;
import com.gateway.service.PaymentLoggingService;
import com.gateway.servicecaller.ServiceCaller;
import com.gateway.utils.BillerConstant;
import com.gateway.utils.Constants;
import com.gateway.utils.Constants.ResponseCodes;
import com.gateway.utils.JwtTokenUtil;
import com.gateway.utils.UtilMethods;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class BillDetailsServiceImpl implements BillDetailsService {

	private static final Logger LOG = LoggerFactory.getLogger(BillDetailsServiceImpl.class);

	@Autowired
	private AuditLoggingService auditLoggingService;

	@Autowired
	private PaymentLoggingService paymentLoggingService;

	@Autowired
	UtilMethods utilMethods;

	@Autowired
	ServiceCaller serviceCaller;

	@Autowired
	// BillerListRepository billerListRepository;
	private BillerConfigurationRepo billerConfigurationRepo;

	@Autowired
	PaymentLogRepository paymentLogRepository;

	@Autowired
	ParamsValidatorService paramsValidatorService;

	@Autowired
	ProvinceTransactionDao provinceTransactionDao;

	@Autowired
	private JwtTokenUtil jwtTokenUtil;

	@Autowired
	private SubBillerListRepository subBillerListRepository;

	@Autowired
	private ObjectMapper objectMapper;

	@Override
	public PaymentInquiryResponse paymentInquiry(HttpServletRequest httpRequestData, PaymentInquiryRequest request) {

		LOG.info("Inside method Bill Inquiry");
		PaymentInquiryResponse paymentInquiryResponse = null;

		Date strDate = new Date();
		String rrn = "";
		String stan = "";

		InfoPayInq info = null;
		String parentBillerId = null;
		String subBillerId = null;

		try {
			UtilMethods.generalLog("IN - Payment Inquiry  " + strDate, LOG);

			BillPaymentInquiryValidationResponse billPaymentInquiryValidationResponse = null;
			// billerDetail=billerListRepository.findByBillerId(request.getTxnInfo().getBillerId());

			// TODO: here we have changed BillerId()
			parentBillerId = request.getTxnInfo().getBillerId().substring(0, 2);
			subBillerId = request.getTxnInfo().getBillerId().substring(2);
			Optional<BillerConfiguration> billerConfiguration = billerConfigurationRepo.findByBillerId(parentBillerId);

			if (billerConfiguration.isPresent()) {
				BillerConfiguration billerDetail = billerConfiguration.get();
				Boolean isActive = billerDetail.getIsActive();

				if (isActive) {
					Optional<SubBillersList> subBiller = subBillerListRepository
							.findBySubBillerIdAndBillerConfiguration(subBillerId, billerDetail);
					if (subBiller.isPresent()) {
						SubBillersList subBillerDetail = subBiller.get();
						billPaymentInquiryValidationResponse = billPaymentInquiryValidations(httpRequestData, request,
								parentBillerId);
						if (billPaymentInquiryValidationResponse != null) {
							if (billPaymentInquiryValidationResponse.getResponseCode().equalsIgnoreCase("00")) {

								if (billerDetail.getBillerName().equalsIgnoreCase("BEOE")) { // BEOE

									switch (subBillerDetail.getSubBillerName()) {
									case BillerConstant.BEOE.BEOE:
										paymentInquiryResponse = paymentInquiryBEOE(request,
												billPaymentInquiryValidationResponse);
										break;

									default:
										LOG.info("subBiller does not exists.");
										info = new InfoPayInq(Constants.ResponseCodes.INVALID_DATA,
												Constants.ResponseDescription.INVALID_INPUT_DATA, rrn, stan);
										paymentInquiryResponse = new PaymentInquiryResponse(info, null, null);
										break;

									}
								} else if (billerDetail.getBillerName().equalsIgnoreCase("PRAL")) { // PRAL

									switch (subBillerDetail.getSubBillerName()) {

									case BillerConstant.PRAL.KPPSC:
										paymentInquiryResponse = paymentInquiryPRAL(request,
												billPaymentInquiryValidationResponse);
										break;

									default:
										LOG.info("subBiller does not exists.");
										info = new InfoPayInq(Constants.ResponseCodes.INVALID_DATA,
												Constants.ResponseDescription.INVALID_INPUT_DATA, rrn, stan);
										paymentInquiryResponse = new PaymentInquiryResponse(info, null, null);

										break;
									}

								}

//								//add new 
//								else if (billerDetail.getBillerName().equalsIgnoreCase("PRAL")
//										&& type.equalsIgnoreCase(Constants.BillerType.ONLINE_BILLER)) {
//
//									switch (subBillerDetail.getSubBillerName()) {
//
//									case BillerConstant.PRAL.KPPSC:
//										paymentInquiryResponse = billInquiryKppsc(request,
//												billPaymentInquiryValidationResponse);
//										break;
//									case BillerConstant.PRAL.FBR:
//										paymentInquiryResponse = billInquiryFbr(request, billPaymentInquiryValidationResponse);
//										break;
//
//									default:
//										LOG.info("subBiller does not exists.");
//										info = new Info(Constants.ResponseCodes.INVALID_BILLER_ID,
//												Constants.ResponseDescription.INVALID_BILLER_ID, rrn, stan);
//										billInquiryResponse = new BillInquiryResponse(info, null, null);
//
//										break;
//									}
//								}
//
//								else if (billerDetail.getBillerName().equalsIgnoreCase(BillerConstant.PTA.PTA)
//										&& type.equalsIgnoreCase(Constants.BillerType.ONLINE_BILLER)) {
//
//									switch (subBillerDetail.getSubBillerName()) {
//
//									case BillerConstant.PTA.PTA:
//										billInquiryResponse = billInquiryPta(request, httpRequestData);
//										break;
//
//									default:
//										LOG.info("subBiller does not exists.");
//										info = new Info(Constants.ResponseCodes.INVALID_BILLER_ID,
//												Constants.ResponseDescription.INVALID_BILLER_ID, rrn, stan);
//										billInquiryResponse = new BillInquiryResponse(info, null, null);
//
//										break;
//									}
//								}
//
//								else if (billerDetail.getBillerName().equalsIgnoreCase(BillerConstant.AIOU.AIOU)
//										&& type.equalsIgnoreCase(Constants.BillerType.ONLINE_BILLER)) {
//
//									switch (subBillerDetail.getSubBillerName()) {
//
//									case BillerConstant.AIOU.AIOU:
//										billInquiryResponse = billInquiryAiou(request, httpRequestData);
//										break;
//
//									default:
//										LOG.info("subBiller does not exists.");
//										info = new Info(Constants.ResponseCodes.INVALID_BILLER_ID,
//												Constants.ResponseDescription.INVALID_BILLER_ID, rrn, stan);
//										billInquiryResponse = new BillInquiryResponse(info, null, null);
//
//										break;
//									}
//								}
//
//								else if (type.equalsIgnoreCase(Constants.BillerType.OFFLINE_BILLER)) {
//									// offline apis
//									billInquiryResponse = billInquiryOffline(httpRequestData, request,
//											parentBillerId, subBillerId);
//
//								}
//								
//								
//								//end here
//								

								else {
									info = new InfoPayInq(Constants.ResponseCodes.INVALID_DATA,
											Constants.ResponseDescription.INVALID_INPUT_DATA, rrn, stan);
									paymentInquiryResponse = new PaymentInquiryResponse(info, null, null);
								}

							} else {
								info = new InfoPayInq(Constants.ResponseCodes.INVALID_DATA,
										Constants.ResponseDescription.INVALID_INPUT_DATA, rrn, stan);
								paymentInquiryResponse = new PaymentInquiryResponse(info, null, null);
							}
						} else {
							info = new InfoPayInq(Constants.ResponseCodes.SERVICE_FAIL,
									Constants.ResponseDescription.SERVICE_FAIL, rrn, stan);
							paymentInquiryResponse = new PaymentInquiryResponse(info, null, null);
						}
					} else {
						info = new InfoPayInq(Constants.ResponseCodes.INVALID_DATA,
								Constants.ResponseDescription.INVALID_INPUT_DATA, rrn, stan);
						paymentInquiryResponse = new PaymentInquiryResponse(info, null, null);

					}
				} else {
					info = new InfoPayInq(Constants.ResponseCodes.INVALID_DATA,
							Constants.ResponseDescription.INVALID_INPUT_DATA, rrn, stan);
					paymentInquiryResponse = new PaymentInquiryResponse(info, null, null);
				}

			}
		} catch (Exception ex) {
			LOG.error("{}", ex);

		}

		return paymentInquiryResponse;

	}

	public BillPaymentInquiryValidationResponse billPaymentInquiryValidations(HttpServletRequest httpRequestData,
			PaymentInquiryRequest request, String billerId) {
		BillPaymentInquiryValidationResponse response = new BillPaymentInquiryValidationResponse();
		String channel = "";
		String username = "";
		String rrn = request.getInfo().getRrn();
		String stan = request.getInfo().getStan();

		Date strDate = new Date();
		List<PaymentLog> paymentHistory = null;
		try {
			UtilMethods.generalLog("IN - Payment Inquiry  " + strDate, LOG);
			LOG.info("Calling Payment Inquiry");
			LOG.info("Payment Inquiry Request {}", request);

			ObjectMapper reqMapper = new ObjectMapper();
			String requestAsString = reqMapper.writeValueAsString(request);
			rrn = request.getInfo().getRrn();
			stan = request.getInfo().getStan();

			if (!paramsValidatorService.validateRequestParams(requestAsString)) {

				response = new BillPaymentInquiryValidationResponse(Constants.ResponseCodes.INVALID_DATA,
						Constants.ResponseDescription.INVALID_DATA, rrn, stan);
				return response;
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
				response = new BillPaymentInquiryValidationResponse(Constants.ResponseCodes.DUPLICATE_TRANSACTION,
						Constants.ResponseDescription.DUPLICATE_TRANSACTION, rrn, stan);
				return response;
			}

//			if (request.getTxnInfo().getBillerId() != null || !request.getTxnInfo().getBillerId().isEmpty()) {
//
//				Optional<BillerConfiguration> billerConfiguration = billerConfigurationRepo
//						.findByBillerId(request.getTxnInfo().getBillerId()); // // biller
//				if (!billerConfiguration.isPresent()) {
//					response = new BillPaymentInquiryValidationResponse(Constants.ResponseCodes.INVALID_DATA,
//							Constants.ResponseDescription.INVALID_DATA, rrn, stan);
//					return response;
//				}
//			}

			response = new BillPaymentInquiryValidationResponse("00", "SUCCESS", username, channel, rrn, stan);

		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return response;

	}

	public PaymentInquiryResponse paymentInquiryBEOE(PaymentInquiryRequest request,
			BillPaymentInquiryValidationResponse BillPaymentInquiryValidationResponse) {

		LOG.info("Inside method Bill Inquiry");
		PaymentInquiryResponse response = null;

		Date strDate = new Date();
		String rrn = request.getInfo().getRrn(); // utilMethods.getRRN();
		String stan = request.getInfo().getStan(); // utilMethods.getStan();
		GetVoucherResponse getVoucherResponse = null;
		// List<PaymentLog> paymentHistory = null;
		InfoPayInq info = null;
		TxnInfoPayInq txnInfo = null;
		AdditionalInfoPayInq additionalInfo = null;
		String transactionStatus = "";
		double transactionFees = 0;
		String cnic = "";
		String mobile = "";
		String address = "";
		String name = "";
		String billStatus = "";
		BigDecimal dbAmount = null;
		double dbTax = 0;
		double dbTransactionFees = 0;
		double dbTotal = 0;
		String tranDate = "";
		String tranTime = "";
		String province = "";
		String paymentReferenceDb = "";
		String channel = "";
		String username = "";

		try {
			UtilMethods.generalLog("IN - Payment Inquiry  " + strDate, LOG);
			LOG.info("Calling Payment Inquiry");

			ArrayList<String> inquiryParams = new ArrayList<String>();
			inquiryParams.add(Constants.MPAY_REQUEST_METHODS.BEOE_BILL_INQUIRY);
			inquiryParams.add(request.getTxnInfo().getBillNumber().trim());
			inquiryParams.add(rrn);
			inquiryParams.add(stan);

			getVoucherResponse = serviceCaller.get(inquiryParams, GetVoucherResponse.class, rrn,
					Constants.ACTIVITY.BillInquiry);
			if (getVoucherResponse != null) {
				if (getVoucherResponse.getResponse().getResponse_code().equals(ResponseCodes.OK)) {
					billStatus = getVoucherResponse.getResponse().getGetvoucher().getStatus();
					if (getVoucherResponse.getResponse().getGetvoucher().getStatus()
							.equalsIgnoreCase(Constants.BILL_STATUS.BILL_PAID)) {
						try {
							LOG.info("Calling PAyment Inquiry");
							PaymentLog paymentLog = paymentLogRepository
									.findFirstByBillerIdAndBillerNumberAndBillStatusIgnoreCaseAndActivityAndResponseCodeOrderByIDDesc(
											request.getTxnInfo().getBillerId().trim(),
											request.getTxnInfo().getBillNumber().trim(),
											Constants.BILL_STATUS.BILL_PAID, Constants.ACTIVITY.BillPayment,
											Constants.ResponseCodes.OK);
//							paymentHistory = paymentLogRepository
//									.findByBillerIdAndBillerNumberAndBillStatusAndActivityAndResponseCode(
//											request.getTxnInfo().getBillerId(), request.getTxnInfo().getBillNumber(),
//											Constants.BILL_STATUS.BILL_PAID, Constants.ACTIVITY.BillPayment,
//											Constants.ResponseCodes.OK);

							if (paymentLog != null && paymentLog.getID() != null) {

								// PaymentLog paymentLogRecord = paymentLog.get(0);
								info = new InfoPayInq(Constants.ResponseCodes.OK, Constants.ResponseDescription.OK, rrn,
										stan); // success

								txnInfo = new TxnInfoPayInq(request.getTxnInfo().getBillerId(),
										request.getTxnInfo().getBillNumber(), paymentLog.getPaymentRefNo(),
										paymentLog.getTranDate(), paymentLog.getTranTime(),
										String.valueOf(paymentLog.getTotal()));

								additionalInfo = new AdditionalInfoPayInq(
										request.getAdditionalInfo().getReserveField1(),
										request.getAdditionalInfo().getReserveField2(),
										request.getAdditionalInfo().getReserveField3(),
										request.getAdditionalInfo().getReserveField4(),
										request.getAdditionalInfo().getReserveField5(),
										request.getAdditionalInfo().getReserveField6(),
										request.getAdditionalInfo().getReserveField7(),
										request.getAdditionalInfo().getReserveField8(),
										request.getAdditionalInfo().getReserveField9(),
										request.getAdditionalInfo().getReserveField10());

								transactionStatus = Constants.Status.Success;
								cnic = paymentLog.getCnic();
								mobile = paymentLog.getMobile();
								address = paymentLog.getAddress();
								name = paymentLog.getName();
								address = paymentLog.getAddress();
								billStatus = paymentLog.getBillStatus();
								dbAmount = paymentLog.getAmount();
								dbTax = paymentLog.getTaxAmount();
								dbTransactionFees = paymentLog.getTransactionFees();
								dbTotal = paymentLog.getTotal();
								tranDate = paymentLog.getTranDate();
								tranTime = paymentLog.getTranTime();
								transactionFees = paymentLog.getTransactionFees();
								province = paymentLog.getProvince();
								paymentReferenceDb = paymentLog.getPaymentRefNo();

								response = new PaymentInquiryResponse(info, txnInfo, additionalInfo);
								return response;
							} else {
								info = new InfoPayInq(Constants.ResponseCodes.PAYMENT_NOT_FOUND,
										Constants.ResponseDescription.PAYMENT_NOT_FOUND, rrn, stan);
								response = new PaymentInquiryResponse(info, null, null);
								transactionStatus = Constants.Status.Fail;
								return response;

							}

						} catch (Exception ex) {
//							exception = ex;

							LOG.error("Exception{}", ex);

						}

					} else {

						info = new InfoPayInq(Constants.ResponseCodes.PAYMENT_NOT_FOUND,
								Constants.ResponseDescription.PAYMENT_NOT_FOUND, rrn, stan);
						response = new PaymentInquiryResponse(info, null, null);
						transactionStatus = Constants.Status.Fail;
						return response;

					}
				} else if (getVoucherResponse.getResponse().getResponse_code().equals("404")) {
					info = new InfoPayInq(Constants.ResponseCodes.INVALID_DATA,
							Constants.ResponseDescription.INVALID_DATA, rrn, stan);
					response = new PaymentInquiryResponse(info, null, null);
					transactionStatus = Constants.Status.Fail;

				} else {
					info = new InfoPayInq(Constants.ResponseCodes.PAYMENT_NOT_FOUND,
							Constants.ResponseDescription.PAYMENT_NOT_FOUND, rrn, stan);
					response = new PaymentInquiryResponse(info, null, null);
					transactionStatus = Constants.Status.Fail;
					return response;
				}

			} else {
				info = new InfoPayInq(rrn, stan, Constants.ResponseCodes.UNABLE_TO_PROCESS,
						Constants.ResponseDescription.UNABLE_TO_PROCESS);
				response = new PaymentInquiryResponse(info, null, null);
				transactionStatus = Constants.Status.Fail;
				return response;
			}

		} catch (Exception ex) {
			LOG.error("{}", ex);

		} finally {

			LOG.info("Bill Payment Inquiry Response {}", response);
			Date responseDate = new Date();

			try {

				String requestAsString = objectMapper.writeValueAsString(request);
				String responseAsString = objectMapper.writeValueAsString(response);

				auditLoggingService.auditLog(Constants.ACTIVITY.PaymentInquiry, response.getInfo().getResponseCode(),
						response.getInfo().getResponseDesc(), requestAsString, responseAsString, strDate, strDate,
						request.getInfo().getRrn(), request.getTxnInfo().getBillerId(),
						request.getTxnInfo().getBillNumber(), channel, username);

			} catch (Exception ex) {
				LOG.error("Exception {}", ex);
			}

			try {

				paymentLoggingService.paymentLog(responseDate, responseDate, rrn, stan,
						response.getInfo().getResponseCode(), response.getInfo().getResponseDesc(), cnic, mobile, name,
						request.getTxnInfo().getBillNumber(), request.getTxnInfo().getBillerId(), dbAmount,
						dbTransactionFees, Constants.ACTIVITY.PaymentInquiry, paymentReferenceDb,
						request.getTxnInfo().getBillNumber(), transactionStatus, address, transactionFees, dbTax,
						dbTotal, channel, billStatus, tranDate, tranTime, province, "");

			} catch (Exception ex) {
				LOG.error("{}", ex);
			}
			UtilMethods.generalLog("OUT -  Payment Inquiry " + responseDate, LOG);
			LOG.info("Payment Inquiry Response: ", response);
		}

		return response;

	}

	public PaymentInquiryResponse paymentInquiryPRAL(PaymentInquiryRequest request,
			BillPaymentInquiryValidationResponse BillPaymentInquiryValidationResponse) {

		LOG.info("Inside method Bill Inquiry");
		PaymentInquiryResponse response = null;

		Date strDate = new Date();
		String rrn = "";
		String stan = "";
		GetVoucherResponse getVoucherResponse = null;
		// List<PaymentLog> paymentHistory = null;
		InfoPayInq info = null;
		TxnInfoPayInq txnInfo = null;
		AdditionalInfoPayInq additionalInfo = null;
		String transactionStatus = "";
		double transactionFees = 0;
		String cnic = "";
		String mobile = "";
		String address = "";
		String name = "";
		String billStatus = "";
		BigDecimal dbAmount = null;
		double dbTax = 0;
		double dbTransactionFees = 0;
		double dbTotal = 0;
		String tranDate = "";
		String tranTime = "";
		String province = "";
		String paymentReferenceDb = "";
		String channel = "";
		String username = "";

		try {
			UtilMethods.generalLog("IN - Payment Inquiry  " + strDate, LOG);
			LOG.info("Calling Payment Inquiry");
			LOG.info("Payment Inquiry Request {}", request);

			ArrayList<String> inquiryParams = new ArrayList<String>();
			inquiryParams.add(Constants.MPAY_REQUEST_METHODS.PRAL_BILL_INQUIRY);
			inquiryParams.add(request.getTxnInfo().getBillNumber().trim());
			inquiryParams.add(rrn);
			inquiryParams.add(stan);

			getVoucherResponse = serviceCaller.get(inquiryParams, GetVoucherResponse.class, rrn,
					Constants.ACTIVITY.BillInquiry);
			if (getVoucherResponse != null) {
				if (getVoucherResponse.getResponse().getResponse_code().equals(ResponseCodes.OK)) {
					billStatus = getVoucherResponse.getResponse().getGetvoucher().getStatus();
					if (getVoucherResponse.getResponse().getGetvoucher().getStatus()
							.equalsIgnoreCase(Constants.BILL_STATUS.BILL_PAID)) {
						try {
							LOG.info("Calling PAyment Inquiry");
							PaymentLog paymentLog = paymentLogRepository
									.findFirstByBillerIdAndBillerNumberAndBillStatusIgnoreCaseAndActivityAndResponseCodeOrderByIDDesc(
											request.getTxnInfo().getBillerId().trim(),
											request.getTxnInfo().getBillNumber().trim(),
											Constants.BILL_STATUS.BILL_PAID, Constants.ACTIVITY.BillPayment,
											Constants.ResponseCodes.OK);
							if (paymentLog != null && paymentLog.getID() != null) {

//								PaymentLog paymentLogRecord = paymentHistory.get(0);
								info = new InfoPayInq(Constants.ResponseCodes.OK, Constants.ResponseDescription.OK, rrn,
										stan); // success

								txnInfo = new TxnInfoPayInq(request.getTxnInfo().getBillerId(),
										request.getTxnInfo().getBillNumber(), paymentLog.getPaymentRefNo(),
										paymentLog.getTranDate(), paymentLog.getTranTime(),
										String.valueOf(paymentLog.getTotal()));

								additionalInfo = new AdditionalInfoPayInq(
										request.getAdditionalInfo().getReserveField1(),
										request.getAdditionalInfo().getReserveField2(),
										request.getAdditionalInfo().getReserveField3(),
										request.getAdditionalInfo().getReserveField4(),
										request.getAdditionalInfo().getReserveField5(),
										request.getAdditionalInfo().getReserveField6(),
										request.getAdditionalInfo().getReserveField7(),
										request.getAdditionalInfo().getReserveField8(),
										request.getAdditionalInfo().getReserveField9(),
										request.getAdditionalInfo().getReserveField10());

								transactionStatus = Constants.Status.Success;
								cnic = paymentLog.getCnic();
								mobile = paymentLog.getMobile();
								address = paymentLog.getAddress();
								name = paymentLog.getName();
								address = paymentLog.getAddress();
								billStatus = paymentLog.getBillStatus();
								// dbAmount = paymentLogRecord.getAmount();
								dbTax = paymentLog.getTaxAmount();
								dbTransactionFees = paymentLog.getTransactionFees();
								dbTotal = paymentLog.getTotal();
								tranDate = paymentLog.getTranDate();
								tranTime = paymentLog.getTranTime();
								transactionFees = paymentLog.getTransactionFees();
								province = paymentLog.getProvince();
								paymentReferenceDb = paymentLog.getPaymentRefNo();

								response = new PaymentInquiryResponse(info, txnInfo, additionalInfo);
								return response;
							} else {
								info = new InfoPayInq(Constants.ResponseCodes.PAYMENT_NOT_FOUND,
										Constants.ResponseDescription.PAYMENT_NOT_FOUND, rrn, stan);
								response = new PaymentInquiryResponse(info, null, null);
								transactionStatus = Constants.Status.Fail;
								return response;

							}

						} catch (Exception ex) {
//							exception = ex;

							LOG.error("{}", ex);

						}

					} else {

						info = new InfoPayInq(Constants.ResponseCodes.PAYMENT_NOT_FOUND,
								Constants.ResponseDescription.PAYMENT_NOT_FOUND, rrn, stan);
						response = new PaymentInquiryResponse(info, null, null);
						transactionStatus = Constants.Status.Fail;
						return response;

					}
				} else if (getVoucherResponse.getResponse().getResponse_code().equals("404")) {
					info = new InfoPayInq(Constants.ResponseCodes.INVALID_DATA,
							Constants.ResponseDescription.INVALID_DATA, rrn, stan);
					response = new PaymentInquiryResponse(info, null, null);
					transactionStatus = Constants.Status.Fail;

				} else {
					info = new InfoPayInq(Constants.ResponseCodes.PAYMENT_NOT_FOUND,
							Constants.ResponseDescription.PAYMENT_NOT_FOUND, rrn, stan);
					response = new PaymentInquiryResponse(info, null, null);
					transactionStatus = Constants.Status.Fail;
					return response;
				}

			} else {
				info = new InfoPayInq(rrn, stan, Constants.ResponseCodes.UNABLE_TO_PROCESS,
						Constants.ResponseDescription.UNABLE_TO_PROCESS);
				response = new PaymentInquiryResponse(info, null, null);
				transactionStatus = Constants.Status.Fail;
				return response;
			}

		} catch (Exception ex) {
			LOG.error("{}", ex);

		} finally {

			LOG.info("Bill Payment Inquiry Response {}", response);
			Date responseDate = new Date();

			try {

				ObjectMapper reqMapper = new ObjectMapper();
				String requestAsString = reqMapper.writeValueAsString(request);

				ObjectMapper respMapper = new ObjectMapper();
				String responseAsString = respMapper.writeValueAsString(response);

				auditLoggingService.auditLog(Constants.ACTIVITY.PaymentInquiry, response.getInfo().getResponseCode(),
						response.getInfo().getResponseDesc(), requestAsString, responseAsString, strDate, strDate,
						request.getInfo().getRrn(), request.getTxnInfo().getBillerId(),
						request.getTxnInfo().getBillNumber(), channel, username);

			} catch (Exception ex) {
				LOG.error("{}", ex);
			}

			try {

				paymentLoggingService.paymentLog(responseDate, responseDate, rrn, stan,
						response.getInfo().getResponseCode(), response.getInfo().getResponseDesc(), cnic, mobile, name,
						request.getTxnInfo().getBillNumber(), request.getTxnInfo().getBillerId(), dbAmount,
						dbTransactionFees, Constants.ACTIVITY.PaymentInquiry, paymentReferenceDb,
						request.getTxnInfo().getBillNumber(), transactionStatus, address, transactionFees, dbTax,
						dbTotal, channel, billStatus, tranDate, tranTime, province, "");

			} catch (Exception ex) {
				LOG.error("{}", ex);
			}
			UtilMethods.generalLog("OUT -  Payment Inquiry " + responseDate, LOG);
			LOG.info("Payment Inquiry Response: ", response);
		}

		return response;

	}

	@Override
	public BillerListResponse getBillerList(HttpServletRequest httpRequestData) {

		LOG.info("Inside method Get Biller List");
		BillerListResponse response = null;
		InfoBiller info = null;
		TxnInfoBiller txnInfo = null;
		Date strDate = new Date();
		String channel = "";
		String username = "";

		try {
			LOG.info("Getting biller list");
			UtilMethods.generalLog("IN - getBillerList  " + strDate, LOG);

			try {
				String[] result = jwtTokenUtil.getTokenInformation(httpRequestData);
				username = result[0];
				channel = result[1];

			} catch (Exception ex) {
				ex.printStackTrace();
			}
//			List<BillerConfiguration> billers = billerConfigurationRepo.findAll();
			List<BillerConfiguration> billers = billerConfigurationRepo.findByIsActiveTrue();

			if (billers != null) {
				ArrayList<Billers> billersResponseList = new ArrayList<>();

				for (BillerConfiguration temp : billers) {
					List<SubBillersList> subBillers = subBillerListRepository
							.findByBillerConfigurationAndIsActiveTrue(temp);

					if (!subBillers.isEmpty()) {
						for (SubBillersList subBiller : subBillers) {
							Billers billersResponse = new Billers();
							StringBuilder combinedId = new StringBuilder(temp.getBillerId());

							combinedId.append(subBiller.getSubBillerId());
							billersResponse.setBillerId(combinedId.toString());
							billersResponse.setBillerName(subBiller.getSubBillerName());
							billersResponseList.add(billersResponse);
						}
					}
				}

				info = new InfoBiller(Constants.ResponseCodes.OK, Constants.ResponseDescription.OK);
				txnInfo = new TxnInfoBiller(billersResponseList);

				response = new BillerListResponse(info, txnInfo);

			} else {
				info = new InfoBiller(Constants.ResponseCodes.UNABLE_TO_PROCESS,
						Constants.ResponseDescription.UNABLE_TO_PROCESS);
				response = new BillerListResponse(info, null);
			}

		} catch (Exception ex) {
			info = new InfoBiller(Constants.ResponseCodes.UNABLE_TO_PROCESS,
					Constants.ResponseDescription.UNABLE_TO_PROCESS);
			response = new BillerListResponse(info, null);
			LOG.error("{}", ex);

		} finally {
			LOG.info("Biller List Response {}", response);
			Date responseDate = new Date();

			try {

				ObjectMapper respMapper = new ObjectMapper();
				String responseAsString = respMapper.writeValueAsString(response);

				auditLoggingService.auditLog(Constants.ACTIVITY.GetBillerList, response.getInfo().getResponseCode(),
						response.getInfo().getResponseDesc(), "", responseAsString, strDate, strDate, null, null, null,
						channel, username);
			} catch (Exception ex) {
				LOG.error("{}", ex);
			}
			UtilMethods.generalLog("OUT -  Get Biller List " + responseDate, LOG);
		}

		LOG.info("Biller List Response : ", response);
		return response;

	}

}
