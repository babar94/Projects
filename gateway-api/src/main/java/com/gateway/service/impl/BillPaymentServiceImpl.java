package com.gateway.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gateway.entity.BillerConfiguration;
import com.gateway.entity.PaymentLog;
import com.gateway.entity.ProvinceTransaction;
import com.gateway.entity.SubBillersList;
import com.gateway.model.mpay.response.billinquiry.GetVoucherResponse;
import com.gateway.model.mpay.response.billpayment.UpdateVoucherResponse;
import com.gateway.repository.BillerConfigurationRepo;
import com.gateway.repository.PaymentLogRepository;
import com.gateway.repository.ProvinceTransactionDao;
import com.gateway.repository.SubBillerListRepository;
import com.gateway.request.billpayment.BillPaymentRequest;
import com.gateway.response.BillPaymentValidationResponse;
import com.gateway.response.billpaymentresponse.AdditionalInfoPay;
import com.gateway.response.billpaymentresponse.BillPaymentResponse;
import com.gateway.response.billpaymentresponse.InfoPay;
import com.gateway.response.billpaymentresponse.TxnInfoPay;
import com.gateway.service.AuditLoggingService;
import com.gateway.service.BillPaymentService;
import com.gateway.service.ParamsValidatorService;
import com.gateway.service.PaymentLoggingService;
import com.gateway.servicecaller.ServiceCaller;
import com.gateway.utils.BillerConstant;
import com.gateway.utils.CompAndDecompString;
import com.gateway.utils.Constants;
import com.gateway.utils.Constants.ResponseCodes;
import com.gateway.utils.JwtTokenUtil;
import com.gateway.utils.UtilMethods;

@Service
public class BillPaymentServiceImpl implements BillPaymentService {

	private static final Logger LOG = LoggerFactory.getLogger(BillPaymentServiceImpl.class);

	@Autowired
	private AuditLoggingService auditLoggingService;

	@Autowired
	private PaymentLoggingService paymentLoggingService;

	@Autowired
	private UtilMethods utilMethods;

	@Autowired
	private ServiceCaller serviceCaller;

	@Autowired
	// private BillerListRepository billerListRepository;
	private BillerConfigurationRepo billerConfigurationRepo;
	@Autowired
	private PaymentLogRepository paymentLogRepository;

	@Autowired
	private ParamsValidatorService paramsValidatorService;

	@Autowired
	private ProvinceTransactionDao provinceTransactionDao;

	@Autowired
	private JwtTokenUtil jwtTokenUtil;

	@Autowired
	private SubBillerListRepository subBillerListRepository;

	@Autowired
	private ObjectMapper objectMapper;

	@Override
	public BillPaymentResponse billPayment(HttpServletRequest httpRequestData, BillPaymentRequest request) {

		LOG.info("Inside method Bill Payment");
		BillPaymentResponse billPaymentResponse = null;
		InfoPay infoPay = null;

		String rrn = request.getInfo().getRrn();
		String stan = request.getInfo().getStan();
		String parentBillerId = null;
		String subBillerId = null;

		try {
			BillPaymentValidationResponse billPaymentValidationResponse = null;
			// billerList =
			// billerListRepository.findByBillerId(request.getTxnInfo().getAggregatorId());

			parentBillerId = request.getTxnInfo().getBillerId().substring(0, 3);
			subBillerId = request.getTxnInfo().getBillerId().substring(3);
			Optional<BillerConfiguration> billerConfiguration = billerConfigurationRepo.findByBillerId(parentBillerId);

			if (billerConfiguration.isPresent()) {
				BillerConfiguration billerDetail = billerConfiguration.get();
				String type = billerDetail.getType();
				Boolean isActive = billerDetail.getIsActive();

				if (isActive) {
					Optional<SubBillersList> subBiller = subBillerListRepository
							.findBySubBillerIdAndBillerConfiguration(subBillerId, billerDetail);
					if (subBiller.isPresent()) {
						SubBillersList subBillerDetail = subBiller.get();
						billPaymentValidationResponse = billPaymentValidations(httpRequestData, request,
								parentBillerId);
						if (billPaymentValidationResponse != null) {
							if (billPaymentValidationResponse.getResponseCode().equalsIgnoreCase("00")) {

								if (billerDetail.getBillerName().equalsIgnoreCase("BEOE")) { // BEOE

									switch (subBillerDetail.getSubBillerName()) {
									case BillerConstant.BEOE.BEOE:
										billPaymentResponse = billPaymentBEOE(request, billPaymentValidationResponse);
										break;

									default:
										LOG.info("subBiller does not exists.");
										infoPay = new InfoPay(Constants.ResponseCodes.INVALID_DATA,
												Constants.ResponseDescription.INVALID_INPUT_DATA, rrn, stan);
										billPaymentResponse = new BillPaymentResponse(infoPay, null, null);
										break;

									}
								} else if (billerDetail.getBillerName().equalsIgnoreCase("PRAL")) { // PRAL

									switch (subBillerDetail.getSubBillerName()) {

									case BillerConstant.PRAL.KPPSC:
										billPaymentResponse = billPaymentPRAL(request, billPaymentValidationResponse);
										break;

									default:
										LOG.info("subBiller does not exists.");
										infoPay = new InfoPay(Constants.ResponseCodes.INVALID_DATA,
												Constants.ResponseDescription.INVALID_INPUT_DATA, rrn, stan);
										billPaymentResponse = new BillPaymentResponse(infoPay, null, null);

										break;
									}

								} else {
									LOG.info("Biller does not exists.");
									infoPay = new InfoPay(Constants.ResponseCodes.INVALID_DATA,
											Constants.ResponseDescription.INVALID_INPUT_DATA, rrn, stan);
									billPaymentResponse = new BillPaymentResponse(infoPay, null, null);
								}

							} else {
								infoPay = new InfoPay(Constants.ResponseCodes.INVALID_DATA,
										Constants.ResponseDescription.INVALID_INPUT_DATA, rrn, stan);
								billPaymentResponse = new BillPaymentResponse(infoPay, null, null);
							}
						} else {
							infoPay = new InfoPay(Constants.ResponseCodes.SERVICE_FAIL,
									Constants.ResponseDescription.SERVICE_FAIL, rrn, stan);
							billPaymentResponse = new BillPaymentResponse(infoPay, null, null);
						}
					} else {
						infoPay = new InfoPay(Constants.ResponseCodes.INVALID_DATA,
								Constants.ResponseDescription.INVALID_INPUT_DATA, rrn, stan);
						billPaymentResponse = new BillPaymentResponse(infoPay, null, null);

					}
				} else {
					infoPay = new InfoPay(Constants.ResponseCodes.INVALID_DATA,
							Constants.ResponseDescription.INVALID_INPUT_DATA, rrn, stan);
					billPaymentResponse = new BillPaymentResponse(infoPay, null, null);
				}
			}
		} catch (Exception ex) {
			LOG.error("Exception in billPayment method {} " + ex);
		}
		return billPaymentResponse;

	}

	public BillPaymentValidationResponse billPaymentValidations(HttpServletRequest httpRequestData,
			BillPaymentRequest request, String billerId) {
		BillPaymentValidationResponse response = new BillPaymentValidationResponse();
		String channel = "";
		String username = "";
		String rrn = request.getInfo().getRrn();
		String stan = request.getInfo().getStan();

		Date strDate = new Date();
		List<PaymentLog> paymentHistory = null;
		try {
			rrn = request.getInfo().getRrn();
			stan = request.getInfo().getStan();

			UtilMethods.generalLog("IN - BillPayment  " + strDate, LOG);
			LOG.info("Bill Payment Request {}", request);
			LOG.info("Calling GetVoucher");

			ObjectMapper reqMapper = new ObjectMapper();
			String requestAsString = reqMapper.writeValueAsString(request);

			ProvinceTransaction provinceTransaction = null;

			if (!paramsValidatorService.validateRequestParams(requestAsString)) {
				response = new BillPaymentValidationResponse(Constants.ResponseCodes.INVALID_DATA,
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
				response = new BillPaymentValidationResponse(Constants.ResponseCodes.DUPLICATE_TRANSACTION,
						Constants.ResponseDescription.DUPLICATE_TRANSACTION, rrn, stan);
				return response;
			}
			// tran Auth Id Validation - Stop Duplication
			paymentHistory = paymentLogRepository.findByTranAuthIdAndActivity(request.getTxnInfo().getTranAuthId(),
					Constants.ACTIVITY.BillPayment);
			if (paymentHistory != null && !paymentHistory.isEmpty()) {
				response = new BillPaymentValidationResponse(Constants.ResponseCodes.DUPLICATE_TRANSACTION_AUTH_ID,
						Constants.ResponseDescription.DUPLICATE_TRANSACTION_AUTH_ID, rrn, stan);
				return response;
			}

			if (request.getTxnInfo().getBillerId() != null || !request.getTxnInfo().getBillerId().isEmpty()) {

				Optional<BillerConfiguration> billerConfiguration = billerConfigurationRepo
						.findByBillerId(request.getTxnInfo().getBillerId());

				if (!billerConfiguration.isPresent()) {

					response = new BillPaymentValidationResponse(Constants.ResponseCodes.INVALID_DATA,
							Constants.ResponseDescription.INVALID_DATA, rrn, stan);
					return response;
				}
			}

			if (request.getTerminalInfo().getProvince() == null || request.getTerminalInfo().getProvince().isEmpty()) {
				response = new BillPaymentValidationResponse(Constants.ResponseCodes.INVALID_DATA,
						Constants.ResponseDescription.INVALID_DATA, rrn, stan);
				return response;
			} else {
				provinceTransaction = provinceTransactionDao
						.findByProvinceCode(request.getTerminalInfo().getProvince());
			}
			if (provinceTransaction == null) {
				response = new BillPaymentValidationResponse(Constants.ResponseCodes.INVALID_DATA,
						Constants.ResponseDescription.INVALID_DATA, rrn, stan);
				return response;
			}

			response = new BillPaymentValidationResponse("00", "SUCCESS", username, channel, provinceTransaction, rrn,
					stan);

		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return response;

	}

	public BillPaymentResponse billPaymentBEOE(BillPaymentRequest request,
			BillPaymentValidationResponse billPaymentValidationResponse) {

		LOG.info("Inside method Bill Payment");
		BillPaymentResponse response = null;
		Date responseDate = new Date();
		UpdateVoucherResponse updateVoucherResponse = null;
		Date strDate = new Date();

		GetVoucherResponse getVoucherResponse = null;
		InfoPay infoPay = null;
		TxnInfoPay txnInfoPay = null;
		AdditionalInfoPay additionalInfoPay = null;
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
		String rrn = request.getInfo().getRrn();
		LOG.info("RRN :{ }", rrn);
		String stan = request.getInfo().getStan();
		String transAuthId = "";
		String paymentRefrence = "";

		String channel = "";
		String username = "";

		try {

			ArrayList<String> inquiryParams = new ArrayList<String>();
			inquiryParams.add(Constants.MPAY_REQUEST_METHODS.BEOE_BILL_INQUIRY);
			inquiryParams.add(request.getTxnInfo().getBillNumber().trim());
			inquiryParams.add(rrn);
			inquiryParams.add(stan);

			getVoucherResponse = serviceCaller.get(inquiryParams, GetVoucherResponse.class, stan,
					Constants.ACTIVITY.BillInquiry);

			if (getVoucherResponse != null) {
				if (getVoucherResponse.getResponse().getResponse_code().equals(ResponseCodes.OK)) {
					// Setting Values for Db entry
					cnic = getVoucherResponse.getResponse().getGetvoucher().getCnic();
					mobile = getVoucherResponse.getResponse().getGetvoucher().getMobile();
					address = getVoucherResponse.getResponse().getGetvoucher().getAddress();
					name = getVoucherResponse.getResponse().getGetvoucher().getName();
					address = getVoucherResponse.getResponse().getGetvoucher().getAddress();
					billStatus = getVoucherResponse.getResponse().getGetvoucher().getStatus();

					BigDecimal inquiryTotalAmountbdUp = new BigDecimal(
							Double.parseDouble(getVoucherResponse.getResponse().getGetvoucher().getTotal()))
							.setScale(2, RoundingMode.UP);

					dbAmount = utilMethods.bigDecimalToDouble(inquiryTotalAmountbdUp);

					if (Double.valueOf(request.getTxnInfo().getTranAmount()).compareTo(dbAmount) != 0) {
						infoPay = new InfoPay(Constants.ResponseCodes.AMMOUNT_MISMATCH,
								Constants.ResponseDescription.AMMOUNT_MISMATCH, rrn, stan);
						response = new BillPaymentResponse(infoPay, null, null);
						return response;
					}

					if (getVoucherResponse.getResponse().getGetvoucher().getStatus()
							.equalsIgnoreCase(Constants.BILL_STATUS.BILL_UNPAID)) {

						try {

							String tranDate = request.getTxnInfo().getTranDate();

							if (tranDate.length() == 8) {
								tranDate = utilMethods.transactionDateFormater(tranDate);
							}
							LOG.info("Calling UpdateVoucher");

							ArrayList<String> ubpsBillParams = new ArrayList<String>();

							ubpsBillParams.add(Constants.MPAY_REQUEST_METHODS.BEOE_BILL_PAYMENT);
							ubpsBillParams.add(request.getTxnInfo().getBillNumber());
							ubpsBillParams.add(tranDate);
							ubpsBillParams.add(request.getTxnInfo().getTranAmount());

							ubpsBillParams.add(CompAndDecompString.compressAndReturnB64(
									getVoucherResponse.getResponse().getGetvoucher().fees.toString())); // fees
							ubpsBillParams.add(request.getTxnInfo().getTranAuthId()); // payment reference
							ubpsBillParams.add(rrn);
							ubpsBillParams.add(stan);

							updateVoucherResponse = serviceCaller.get(ubpsBillParams, UpdateVoucherResponse.class, rrn,
									Constants.ACTIVITY.BillPayment);

							if (updateVoucherResponse != null) {
								infoPay = new InfoPay(updateVoucherResponse.getResponse().getResponse_code(),
										updateVoucherResponse.getResponse().getResponse_desc(), rrn, stan);
								if (updateVoucherResponse.getResponse().getResponse_code().equals(ResponseCodes.OK)) {
									paymentRefrence = utilMethods.getRRN();
									txnInfoPay = new TxnInfoPay(request.getTxnInfo().getBillerId(),
											request.getTxnInfo().getBillNumber(), name);
									additionalInfoPay = new AdditionalInfoPay(
											request.getAdditionalInfo().getReserveField1(),
											request.getAdditionalInfo().getReserveField2(),
											request.getAdditionalInfo().getReserveField3(),
											request.getAdditionalInfo().getReserveField4(),
											request.getAdditionalInfo().getReserveField5());

									response = new BillPaymentResponse(infoPay, txnInfoPay, additionalInfoPay);
									transactionStatus = Constants.Status.Success;
									billStatus = Constants.BILL_STATUS.BILL_PAID;

								} else if (updateVoucherResponse.getResponse().getResponse_code().equals("402")) {
									infoPay = new InfoPay(Constants.ResponseCodes.DUPLICATE_TRANSACTION,
											Constants.ResponseDescription.DUPLICATE_TRANSACTION, rrn, stan);
									txnInfoPay = new TxnInfoPay(request.getTxnInfo().getBillerId(),
											request.getTxnInfo().getBillNumber(), name);// paymentRefrence
									additionalInfoPay = new AdditionalInfoPay(
											request.getAdditionalInfo().getReserveField1(),
											request.getAdditionalInfo().getReserveField2(),
											request.getAdditionalInfo().getReserveField3(),
											request.getAdditionalInfo().getReserveField4(),
											request.getAdditionalInfo().getReserveField5());
									response = new BillPaymentResponse(infoPay, txnInfoPay, additionalInfoPay);
									transactionStatus = Constants.Status.Fail;
								} else {
									infoPay = new InfoPay(Constants.ResponseCodes.SERVICE_FAIL,
											Constants.ResponseDescription.SERVICE_FAIL, rrn, stan);
									txnInfoPay = new TxnInfoPay(request.getTxnInfo().getBillerId(),
											request.getTxnInfo().getBillNumber(), name);// paymentRefrence
									additionalInfoPay = new AdditionalInfoPay(
											request.getAdditionalInfo().getReserveField1(),
											request.getAdditionalInfo().getReserveField2(),
											request.getAdditionalInfo().getReserveField3(),
											request.getAdditionalInfo().getReserveField4(),
											request.getAdditionalInfo().getReserveField5());
									response = new BillPaymentResponse(infoPay, txnInfoPay, additionalInfoPay);
								}

							} else { // Second time bill inquiry to pe call from here and if bill is paid then return
										// success

								getVoucherResponse = serviceCaller.get(inquiryParams, GetVoucherResponse.class, stan,
										Constants.ACTIVITY.BillInquiry);

								if (getVoucherResponse != null) {
									if (getVoucherResponse.getResponse().getResponse_code().equals(ResponseCodes.OK)) {
										if (getVoucherResponse.getResponse().getGetvoucher().getStatus()
												.equalsIgnoreCase(Constants.BILL_STATUS.BILL_PAID)) {
											paymentRefrence = utilMethods.getRRN();
											txnInfoPay = new TxnInfoPay(request.getTxnInfo().getBillerId(),
													request.getTxnInfo().getBillNumber(), name);// paymentRefrence
											additionalInfoPay = new AdditionalInfoPay(
													request.getAdditionalInfo().getReserveField1(),
													request.getAdditionalInfo().getReserveField2(),
													request.getAdditionalInfo().getReserveField3(),
													request.getAdditionalInfo().getReserveField4(),
													request.getAdditionalInfo().getReserveField5());

											infoPay = new InfoPay(getVoucherResponse.getResponse().getResponse_code(),
													getVoucherResponse.getResponse().getResponse_desc(), rrn, stan);
											response = new BillPaymentResponse(infoPay, txnInfoPay, additionalInfoPay);
											transactionStatus = Constants.Status.Success;
											billStatus = Constants.BILL_STATUS.BILL_PAID;
										} else {
											infoPay = new InfoPay(Constants.ResponseCodes.SERVICE_FAIL,
													Constants.ResponseDescription.SERVICE_FAIL, rrn, stan);
											txnInfoPay = new TxnInfoPay(request.getTxnInfo().getBillerId(),
													request.getTxnInfo().getBillNumber(), name);// paymentRefrence
											additionalInfoPay = new AdditionalInfoPay(
													request.getAdditionalInfo().getReserveField1(),
													request.getAdditionalInfo().getReserveField2(),
													request.getAdditionalInfo().getReserveField3(),
													request.getAdditionalInfo().getReserveField4(),
													request.getAdditionalInfo().getReserveField5());
											response = new BillPaymentResponse(infoPay, txnInfoPay, additionalInfoPay);
										}
									} else {
										infoPay = new InfoPay(Constants.ResponseCodes.SERVICE_FAIL,
												Constants.ResponseDescription.SERVICE_FAIL, rrn, stan);
										txnInfoPay = new TxnInfoPay(request.getTxnInfo().getBillerId(),
												request.getTxnInfo().getBillNumber(), name);// paymentRefrence
										additionalInfoPay = new AdditionalInfoPay(
												request.getAdditionalInfo().getReserveField1(),
												request.getAdditionalInfo().getReserveField2(),
												request.getAdditionalInfo().getReserveField3(),
												request.getAdditionalInfo().getReserveField4(),
												request.getAdditionalInfo().getReserveField5());
										response = new BillPaymentResponse(infoPay, txnInfoPay, additionalInfoPay);
									}
								} else {
									infoPay = new InfoPay(Constants.ResponseCodes.SERVICE_FAIL,
											Constants.ResponseDescription.SERVICE_FAIL, rrn, stan);
									txnInfoPay = new TxnInfoPay(request.getTxnInfo().getBillerId(),
											request.getTxnInfo().getBillNumber(), name);// paymentRefrence
									additionalInfoPay = new AdditionalInfoPay(
											request.getAdditionalInfo().getReserveField1(),
											request.getAdditionalInfo().getReserveField2(),
											request.getAdditionalInfo().getReserveField3(),
											request.getAdditionalInfo().getReserveField4(),
											request.getAdditionalInfo().getReserveField5());
									response = new BillPaymentResponse(infoPay, txnInfoPay, additionalInfoPay);
									transactionStatus = Constants.Status.Fail;
								}

							}

						} catch (Exception ex) {
							LOG.error("{}", ex);
						}

					} else if (getVoucherResponse.getResponse().getGetvoucher().getStatus()
							.equalsIgnoreCase(Constants.BILL_STATUS.BILL_PAID)) {
						infoPay = new InfoPay(Constants.ResponseCodes.BILL_ALREADY_PAID,
								Constants.ResponseDescription.BILL_ALREADY_PAID, rrn, stan);
						txnInfoPay = new TxnInfoPay(request.getTxnInfo().getBillerId(),
								request.getTxnInfo().getBillNumber(), name);// paymentRefrence
						additionalInfoPay = new AdditionalInfoPay(request.getAdditionalInfo().getReserveField1(),
								request.getAdditionalInfo().getReserveField2(),
								request.getAdditionalInfo().getReserveField3(),
								request.getAdditionalInfo().getReserveField4(),
								request.getAdditionalInfo().getReserveField5());

						response = new BillPaymentResponse(infoPay, txnInfoPay, additionalInfoPay);
						transactionStatus = Constants.Status.Success;
					} else if (getVoucherResponse.getResponse().getGetvoucher().getStatus()
							.equalsIgnoreCase(Constants.BILL_STATUS.BILL_BLOCK)) {
						infoPay = new InfoPay(Constants.ResponseCodes.CONSUMER_NUMBER_BLOCK,
								Constants.ResponseDescription.CONSUMER_NUMBER_BLOCK, rrn, stan);
						txnInfoPay = new TxnInfoPay(request.getTxnInfo().getBillerId(),
								request.getTxnInfo().getBillNumber(), name);// paymentRefrence
						additionalInfoPay = new AdditionalInfoPay(request.getAdditionalInfo().getReserveField1(),
								request.getAdditionalInfo().getReserveField2(),
								request.getAdditionalInfo().getReserveField3(),
								request.getAdditionalInfo().getReserveField4(),
								request.getAdditionalInfo().getReserveField5());
						response = new BillPaymentResponse(infoPay, txnInfoPay, additionalInfoPay);
						transactionStatus = Constants.Status.Fail;
					}
				} else if (getVoucherResponse.getResponse().getResponse_code().equals("404")) {
					infoPay = new InfoPay(Constants.ResponseCodes.CONSUMER_NUMBER_NOT_EXISTS,
							Constants.ResponseDescription.CONSUMER_NUMBER_NOT_EXISTS, rrn, stan);
					txnInfoPay = new TxnInfoPay(request.getTxnInfo().getBillerId(),
							request.getTxnInfo().getBillNumber(), name);// paymentRefrence
					additionalInfoPay = new AdditionalInfoPay(request.getAdditionalInfo().getReserveField1(),
							request.getAdditionalInfo().getReserveField2(),
							request.getAdditionalInfo().getReserveField3(),
							request.getAdditionalInfo().getReserveField4(),
							request.getAdditionalInfo().getReserveField5());
					response = new BillPaymentResponse(infoPay, txnInfoPay, additionalInfoPay);
					transactionStatus = Constants.Status.Fail;

				} else {
					infoPay = new InfoPay(Constants.ResponseCodes.SERVICE_FAIL,
							Constants.ResponseDescription.SERVICE_FAIL, rrn, stan);
					txnInfoPay = new TxnInfoPay(request.getTxnInfo().getBillerId(),
							request.getTxnInfo().getBillNumber(), name);// paymentRefrence
					additionalInfoPay = new AdditionalInfoPay(request.getAdditionalInfo().getReserveField1(),
							request.getAdditionalInfo().getReserveField2(),
							request.getAdditionalInfo().getReserveField3(),
							request.getAdditionalInfo().getReserveField4(),
							request.getAdditionalInfo().getReserveField5());
					response = new BillPaymentResponse(infoPay, txnInfoPay, additionalInfoPay);
					transactionStatus = Constants.Status.Fail;
				}

			} else {
				infoPay = new InfoPay(Constants.ResponseCodes.BAD_TRANSACTION,
						Constants.ResponseDescription.BAD_TRANSACTION, rrn, stan);
				txnInfoPay = new TxnInfoPay(request.getTxnInfo().getBillerId(), request.getTxnInfo().getBillNumber(),
						name);// paymentRefrence
				additionalInfoPay = new AdditionalInfoPay(request.getAdditionalInfo().getReserveField1(),
						request.getAdditionalInfo().getReserveField2(), request.getAdditionalInfo().getReserveField3(),
						request.getAdditionalInfo().getReserveField4(), request.getAdditionalInfo().getReserveField5());
				response = new BillPaymentResponse(infoPay, txnInfoPay, additionalInfoPay);

				transactionStatus = Constants.Status.Fail;
				LOG.info("Calling Bill payment End");
			}
		} catch (Exception ex) {

			LOG.error("{}", ex);

		} finally {
			LOG.info("Bill Payment Response {}", response);
			try {

				String requestAsString = objectMapper.writeValueAsString(request);
				String responseAsString = objectMapper.writeValueAsString(response);

				auditLoggingService.auditLog(Constants.ACTIVITY.BillPayment, response.getInfo().getResponseCode(),
						response.getInfo().getResponseDesc(), requestAsString, responseAsString, strDate, strDate,
						request.getInfo().getRrn(), Long.parseLong(request.getTxnInfo().getBillerId()),
						request.getTxnInfo().getBillNumber(), channel, username);

			} catch (Exception ex) {
				LOG.error("{}", ex);
			}
			try {

				paymentLoggingService.paymentLog(responseDate, responseDate, rrn, stan,
						response.getInfo().getResponseCode(), response.getInfo().getResponseDesc(), cnic, mobile, name,
						request.getTxnInfo().getBillNumber(), request.getTxnInfo().getBillerId(), dbAmount,
						dbTransactionFees, Constants.ACTIVITY.BillPayment, paymentRefrence,
						request.getTxnInfo().getBillNumber(), transactionStatus, address, transactionFees, dbTax,
						dbTotal, channel, billStatus, request.getTxnInfo().getTranDate(),
						request.getTxnInfo().getTranTime(), province, transAuthId);

			} catch (Exception ex) {
				LOG.error("{}", ex);
			}
		}

		UtilMethods.generalLog("OUT -  Bill Payment Response {}" + response, LOG);

		return response;

	}

	public BillPaymentResponse billPaymentPRAL(BillPaymentRequest request,
			BillPaymentValidationResponse billPaymentValidationResponse) {

		LOG.info("Inside method Bill Payment");
		BillPaymentResponse response = null;
		Date responseDate = new Date();
		UpdateVoucherResponse updateVoucherResponse = null;
		Date strDate = new Date();

		GetVoucherResponse getVoucherResponse = null;
		InfoPay infoPay = null;
		TxnInfoPay txnInfoPay = null;
		AdditionalInfoPay additionalInfoPay = null;
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
		String rrn = "";
		String stan = "";
		String transAuthId = request.getTxnInfo().getTranAuthId();
		String paymentRefrence = "";

		String channel = "";
		String username = "";

		try {

			ArrayList<String> inquiryParams = new ArrayList<String>();
			inquiryParams.add(Constants.MPAY_REQUEST_METHODS.PRAL_BILL_PAYMENT);
			inquiryParams.add(request.getTxnInfo().getBillNumber().trim());
			inquiryParams.add(rrn);
			inquiryParams.add(stan);

			getVoucherResponse = serviceCaller.get(inquiryParams, GetVoucherResponse.class, stan,
					Constants.ACTIVITY.BillInquiry);

			if (getVoucherResponse != null) {
				if (getVoucherResponse.getResponse().getResponse_code().equals(ResponseCodes.OK)) {
					// Setting Values for Db entry
					cnic = getVoucherResponse.getResponse().getGetvoucher().getCnic();
					mobile = getVoucherResponse.getResponse().getGetvoucher().getMobile();
					address = getVoucherResponse.getResponse().getGetvoucher().getAddress();
					name = getVoucherResponse.getResponse().getGetvoucher().getName();
					address = getVoucherResponse.getResponse().getGetvoucher().getAddress();
					billStatus = getVoucherResponse.getResponse().getGetvoucher().getStatus();

					BigDecimal inquiryTotalAmountbdUp = new BigDecimal(
							Double.parseDouble(getVoucherResponse.getResponse().getGetvoucher().getTotal()))
							.setScale(2, RoundingMode.UP);

					dbAmount = utilMethods.bigDecimalToDouble(inquiryTotalAmountbdUp);
					if (Double.valueOf(request.getTxnInfo().getTranAmount()).compareTo(dbTotal) != 0) {
						infoPay = new InfoPay(Constants.ResponseCodes.AMMOUNT_MISMATCH,
								Constants.ResponseDescription.AMMOUNT_MISMATCH, rrn, stan);
						response = new BillPaymentResponse(infoPay, null, null);
						return response;
					}

					if (getVoucherResponse.getResponse().getGetvoucher().getStatus()
							.equalsIgnoreCase(Constants.BILL_STATUS.BILL_UNPAID)) {

						try {

							String tranDate = request.getTxnInfo().getTranDate();

							if (tranDate.length() == 8) {
								tranDate = utilMethods.transactionDateFormater(tranDate);
							}
							LOG.info("Calling UpdateVoucher");

							ArrayList<String> ubpsBillParams = new ArrayList<String>();

							ubpsBillParams.add(Constants.MPAY_REQUEST_METHODS.PRAL_BILL_PAYMENT);
							ubpsBillParams.add(request.getTxnInfo().getBillNumber());
							ubpsBillParams.add(tranDate);
							ubpsBillParams.add(request.getTxnInfo().getTranAmount());

							ubpsBillParams.add(CompAndDecompString.compressAndReturnB64(
									getVoucherResponse.getResponse().getGetvoucher().fees.toString())); // fees
							ubpsBillParams.add(request.getTxnInfo().getTranAuthId()); // payment reference
							ubpsBillParams.add(rrn);
							ubpsBillParams.add(stan);

							updateVoucherResponse = serviceCaller.get(ubpsBillParams, UpdateVoucherResponse.class, rrn,
									Constants.ACTIVITY.BillPayment);

							if (updateVoucherResponse != null) {
								infoPay = new InfoPay(updateVoucherResponse.getResponse().getResponse_code(),
										updateVoucherResponse.getResponse().getResponse_desc(), rrn, stan);
								if (updateVoucherResponse.getResponse().getResponse_code().equals(ResponseCodes.OK)) {
									paymentRefrence = utilMethods.getRRN();
									txnInfoPay = new TxnInfoPay(request.getTxnInfo().getBillerId(),
											request.getTxnInfo().getBillNumber(), name);
									additionalInfoPay = new AdditionalInfoPay(
											request.getAdditionalInfo().getReserveField1(),
											request.getAdditionalInfo().getReserveField2(),
											request.getAdditionalInfo().getReserveField3(),
											request.getAdditionalInfo().getReserveField4(),
											request.getAdditionalInfo().getReserveField5());

									response = new BillPaymentResponse(infoPay, txnInfoPay, additionalInfoPay);
									transactionStatus = Constants.Status.Success;
									billStatus = Constants.BILL_STATUS.BILL_PAID;

								} else if (updateVoucherResponse.getResponse().getResponse_code().equals("402")) {
									infoPay = new InfoPay(Constants.ResponseCodes.DUPLICATE_TRANSACTION,
											Constants.ResponseDescription.DUPLICATE_TRANSACTION, rrn, stan);
									response = new BillPaymentResponse(infoPay, null, null);
									transactionStatus = Constants.Status.Fail;
								} else {
									infoPay = new InfoPay(Constants.ResponseCodes.SERVICE_FAIL,
											Constants.ResponseDescription.SERVICE_FAIL, rrn, stan);
									response = new BillPaymentResponse(infoPay, null, null);
								}

							} else { // Second time bill inquiry to pe call from here and if bill is paid then return
										// success

								getVoucherResponse = serviceCaller.get(inquiryParams, GetVoucherResponse.class, stan,
										Constants.ACTIVITY.BillInquiry);

								if (getVoucherResponse != null) {
									if (getVoucherResponse.getResponse().getResponse_code().equals(ResponseCodes.OK)) {
										if (getVoucherResponse.getResponse().getGetvoucher().getStatus()
												.equalsIgnoreCase(Constants.BILL_STATUS.BILL_PAID)) {
											paymentRefrence = utilMethods.getRRN();
											txnInfoPay = new TxnInfoPay(request.getTxnInfo().getBillerId(),
													request.getTxnInfo().getBillNumber(), name);
											additionalInfoPay = new AdditionalInfoPay(
													request.getAdditionalInfo().getReserveField1(),
													request.getAdditionalInfo().getReserveField2(),
													request.getAdditionalInfo().getReserveField3(),
													request.getAdditionalInfo().getReserveField4(),
													request.getAdditionalInfo().getReserveField5());

											infoPay = new InfoPay(getVoucherResponse.getResponse().getResponse_code(),
													getVoucherResponse.getResponse().getResponse_desc(), rrn, stan);
											response = new BillPaymentResponse(infoPay, txnInfoPay, additionalInfoPay);
											transactionStatus = Constants.Status.Success;
											billStatus = Constants.BILL_STATUS.BILL_PAID;
										} else {
											infoPay = new InfoPay(Constants.ResponseCodes.SERVICE_FAIL,
													Constants.ResponseDescription.SERVICE_FAIL, rrn, stan);
											response = new BillPaymentResponse(infoPay, null, null);
										}
									} else {
										infoPay = new InfoPay(Constants.ResponseCodes.SERVICE_FAIL,
												Constants.ResponseDescription.SERVICE_FAIL, rrn, stan);
										response = new BillPaymentResponse(infoPay, null, null);
									}
								} else {
									infoPay = new InfoPay(Constants.ResponseCodes.SERVICE_FAIL,
											Constants.ResponseDescription.SERVICE_FAIL, rrn, stan);
									response = new BillPaymentResponse(infoPay, null, null);
									transactionStatus = Constants.Status.Fail;
								}

							}

						} catch (Exception ex) {
							LOG.error("{}", ex);
						}

					} else if (getVoucherResponse.getResponse().getGetvoucher().getStatus()
							.equalsIgnoreCase(Constants.BILL_STATUS.BILL_PAID)) {
						infoPay = new InfoPay(Constants.ResponseCodes.BILL_ALREADY_PAID,
								Constants.ResponseDescription.BILL_ALREADY_PAID, rrn, stan);
						response = new BillPaymentResponse(infoPay, null, null);
						transactionStatus = Constants.Status.Success;
					} else if (getVoucherResponse.getResponse().getGetvoucher().getStatus()
							.equalsIgnoreCase(Constants.BILL_STATUS.BILL_BLOCK)) {
						infoPay = new InfoPay(Constants.ResponseCodes.CONSUMER_NUMBER_BLOCK,
								Constants.ResponseDescription.CONSUMER_NUMBER_BLOCK, rrn, stan);
						response = new BillPaymentResponse(infoPay, null, null);
						transactionStatus = Constants.Status.Fail;
					}
				} else if (getVoucherResponse.getResponse().getResponse_code().equals("404")) {
					infoPay = new InfoPay(Constants.ResponseCodes.CONSUMER_NUMBER_NOT_EXISTS,
							Constants.ResponseDescription.CONSUMER_NUMBER_NOT_EXISTS, rrn, stan);
					response = new BillPaymentResponse(infoPay, null, null);
					transactionStatus = Constants.Status.Fail;

				} else {
					infoPay = new InfoPay(Constants.ResponseCodes.SERVICE_FAIL,
							Constants.ResponseDescription.SERVICE_FAIL, rrn, stan);
					response = new BillPaymentResponse(infoPay, null, null);
					transactionStatus = Constants.Status.Fail;
				}

			} else {
				infoPay = new InfoPay(Constants.ResponseCodes.BAD_TRANSACTION,
						Constants.ResponseDescription.BAD_TRANSACTION, rrn, stan);
				response = new BillPaymentResponse(infoPay, null, null);
				transactionStatus = Constants.Status.Fail;
				LOG.info("Calling Bill payment End");
			}
		} catch (Exception ex) {

			LOG.error("{}", ex);

		} finally {
			LOG.info("Bill Payment Response {}", response);
			try {

				ObjectMapper reqMapper = new ObjectMapper();
				String requestAsString = reqMapper.writeValueAsString(request);

				ObjectMapper respMapper = new ObjectMapper();
				String responseAsString = respMapper.writeValueAsString(response);

				auditLoggingService.auditLog(Constants.ACTIVITY.BillPayment, response.getInfo().getResponseCode(),
						response.getInfo().getResponseDesc(), requestAsString, responseAsString, strDate, strDate,
						request.getInfo().getRrn(), Long.parseLong(request.getTxnInfo().getBillerId()),
						request.getTxnInfo().getBillNumber(), channel, username);

			} catch (Exception ex) {
				LOG.error("{}", ex);
			}
			try {

				paymentLoggingService.paymentLog(responseDate, responseDate, rrn, stan,
						response.getInfo().getResponseCode(), response.getInfo().getResponseDesc(), cnic, mobile, name,
						request.getTxnInfo().getBillNumber(), request.getTxnInfo().getBillerId(), dbAmount,
						dbTransactionFees, Constants.ACTIVITY.BillPayment, paymentRefrence,
						request.getTxnInfo().getBillNumber(), transactionStatus, address, transactionFees, dbTax,
						dbTotal, channel, billStatus, request.getTxnInfo().getTranDate(),
						request.getTxnInfo().getTranTime(), province, transAuthId);

			} catch (Exception ex) {
				LOG.error("{}", ex);
			}
		}

		UtilMethods.generalLog("OUT -  Bill Payment Response {}" + response, LOG);

		return response;

	}

}
