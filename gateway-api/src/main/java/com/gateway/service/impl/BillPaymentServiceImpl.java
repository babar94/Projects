package com.gateway.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
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
import com.gateway.entity.ProvinceTransaction;
import com.gateway.entity.SubBillersList;
import com.gateway.model.mpay.response.billinquiry.GetVoucherResponse;
import com.gateway.model.mpay.response.billinquiry.aiou.AiouGetVoucherResponse;
import com.gateway.model.mpay.response.billinquiry.fbr.FbrGetVoucherResponse;
import com.gateway.model.mpay.response.billinquiry.offline.OfflineGetVoucherResponse;
import com.gateway.model.mpay.response.billinquiry.pta.DataWrapper;
import com.gateway.model.mpay.response.billinquiry.pta.PtaGetVoucherResponse;
import com.gateway.model.mpay.response.billpayment.UpdateVoucherResponse;
import com.gateway.model.mpay.response.billpayment.aiou.AiouUpdateVoucherResponse;
import com.gateway.model.mpay.response.billpayment.fbr.FbrUpdateVoucherResponse;
import com.gateway.model.mpay.response.billpayment.offline.OfflineUpdateVoucherResponse;
import com.gateway.model.mpay.response.billpayment.pta.PtaUpdateVoucherResponse;
import com.gateway.repository.BillerConfigurationRepo;
import com.gateway.repository.PaymentLogRepository;
import com.gateway.repository.ProvinceTransactionDao;
import com.gateway.repository.SubBillerListRepository;
import com.gateway.request.billpayment.BillPaymentRequest;
import com.gateway.response.BillPaymentValidationResponse;
import com.gateway.response.billinquiryresponse.Info;
import com.gateway.response.billpaymentresponse.AdditionalInfoPay;
import com.gateway.response.billpaymentresponse.BillPaymentResponse;
import com.gateway.response.billpaymentresponse.InfoPay;
import com.gateway.response.billpaymentresponse.TxnInfoPay;
import com.gateway.service.AuditLoggingService;
import com.gateway.service.BillPaymentService;
import com.gateway.service.ParamsValidatorService;
import com.gateway.service.PaymentLoggingService;
import com.gateway.service.ReservedFieldsValidationService;
import com.gateway.servicecaller.ServiceCaller;
import com.gateway.utils.BillerConstant;
import com.gateway.utils.CompAndDecompString;
import com.gateway.utils.Constants;
import com.gateway.utils.Constants.ResponseCodes;
import com.gateway.utils.JwtTokenUtil;
import com.gateway.utils.UtilMethods;

import jakarta.servlet.http.HttpServletRequest;

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
	private ReservedFieldsValidationService reservedFieldsValidationService;

	@Autowired
	private ObjectMapper objectMapper;

	@Override
	public BillPaymentResponse billPayment(HttpServletRequest httpRequestData, BillPaymentRequest request) {

		LOG.info("Inside method Bill Payment");
		BillPaymentResponse billPaymentResponse = null;
		BillPaymentValidationResponse billPaymentValidationResponse = null;

		InfoPay infoPay = null;

		String rrn = request.getInfo().getRrn();
		String stan = request.getInfo().getStan();
		String parentBillerId = null;
		String subBillerId = null;

		try {
			String billerId = request.getTxnInfo().getBillerId();

			if (billerId != null && billerId.length() == 4) {
				parentBillerId = billerId.substring(0, 2);
				subBillerId = billerId.substring(2);

				Optional<BillerConfiguration> billerConfiguration = billerConfigurationRepo
						.findByBillerId(parentBillerId);

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

									if (billerDetail.getBillerName().equalsIgnoreCase("BEOE")
											&& type.equalsIgnoreCase(Constants.BillerType.ONLINE_BILLER)) { // BEOE

										switch (subBillerDetail.getSubBillerName()) {
										case BillerConstant.BEOE.BEOE:
											billPaymentResponse = billPaymentBEOE(request, httpRequestData);
											break;

										default:
											LOG.info("subBiller does not exists.");
											infoPay = new InfoPay(Constants.ResponseCodes.INVALID_BILLER_ID,
													Constants.ResponseDescription.INVALID_BILLER_ID, rrn, stan);
											billPaymentResponse = new BillPaymentResponse(infoPay, null, null);
											break;

										}
									} else if (billerDetail.getBillerName().equalsIgnoreCase("PRAL")
											&& type.equalsIgnoreCase(Constants.BillerType.ONLINE_BILLER)) { // PRAL

										switch (subBillerDetail.getSubBillerName()) {

										case BillerConstant.PRAL.KPPSC:
											billPaymentResponse = billPaymentPral(request, httpRequestData);
											break;

										case BillerConstant.PRAL.FBR:
											billPaymentResponse = billPaymentFbr(request, httpRequestData);
											break;

										default:
											LOG.info("subBiller does not exists.");
											infoPay = new InfoPay(Constants.ResponseCodes.INVALID_BILLER_ID,
													Constants.ResponseDescription.INVALID_BILLER_ID, rrn, stan);
											billPaymentResponse = new BillPaymentResponse(infoPay, null, null);

											break;
										}
									}

									// PTA
									else if (billerDetail.getBillerName().equalsIgnoreCase("PTA")
											&& type.equalsIgnoreCase(Constants.BillerType.ONLINE_BILLER)) { // PRAL

										switch (subBillerDetail.getSubBillerName()) {

										case BillerConstant.PTA.PTA:
											billPaymentResponse = billPaymentPta(request, httpRequestData);
											break;

										default:
											LOG.info("subBiller does not exists.");
											infoPay = new InfoPay(Constants.ResponseCodes.INVALID_BILLER_ID,
													Constants.ResponseDescription.INVALID_BILLER_ID, rrn, stan);
											billPaymentResponse = new BillPaymentResponse(infoPay, null, null);

											break;
										}
									}

									// AIOU
									// PTA
									else if (billerDetail.getBillerName().equalsIgnoreCase(BillerConstant.AIOU.AIOU)
											&& type.equalsIgnoreCase(Constants.BillerType.ONLINE_BILLER)) { // PRAL

										switch (subBillerDetail.getSubBillerName()) {

										case BillerConstant.AIOU.AIOU:

//											reservedFieldsValidationService.validateReservedFields(request)
//											billPaymentResponse = billPaymentAiou(request, httpRequestData);
//											break;

											// Validate the additional fields of AIOU from the request
											if (reservedFieldsValidationService.validateReservedFields(request,
													parentBillerId)) {
												// Proceed with AIOU bill payment
												billPaymentResponse = billPaymentAiou(request, httpRequestData);
											} else {
												// Handle the case when reserved fields validation fails
												LOG.info("Reserved fields validation failed.");
												infoPay = new InfoPay(Constants.ResponseCodes.INVALID_DATA,
														Constants.ResponseDescription.INVALID_DATA, rrn, stan);
												billPaymentResponse = new BillPaymentResponse(infoPay, null, null);
											}

											break;

										default:
											LOG.info("subBiller does not exists.");
											infoPay = new InfoPay(Constants.ResponseCodes.INVALID_BILLER_ID,
													Constants.ResponseDescription.INVALID_BILLER_ID, rrn, stan);
											billPaymentResponse = new BillPaymentResponse(infoPay, null, null);

											break;
										}
									}

									else if (type.equalsIgnoreCase(Constants.BillerType.OFFLINE_BILLER)
											&& subBiller.get().getIsActive()) {
										// offline apis
										billPaymentResponse = billPaymentOffline(request, httpRequestData,
												parentBillerId, subBillerId);

									}

									else {
										LOG.info("Biller does not exists.");
										infoPay = new InfoPay(Constants.ResponseCodes.INVALID_BILLER_ID,
												Constants.ResponseDescription.INVALID_BILLER_ID, rrn, stan);
										billPaymentResponse = new BillPaymentResponse(infoPay, null, null);
									}
								}

								else {
									infoPay = new InfoPay(Constants.ResponseCodes.INVALID_DATA,
											billPaymentValidationResponse.getResponseDesc(), rrn, stan);
									billPaymentResponse = new BillPaymentResponse(infoPay, null, null);
								}
							} else {
								infoPay = new InfoPay(Constants.ResponseCodes.SERVICE_FAIL,
										Constants.ResponseDescription.SERVICE_FAIL, rrn, stan);
								billPaymentResponse = new BillPaymentResponse(infoPay, null, null);
							}
						} else {
							infoPay = new InfoPay(Constants.ResponseCodes.INVALID_BILLER_ID,
									Constants.ResponseDescription.INVALID_BILLER_ID, rrn, stan);
							billPaymentResponse = new BillPaymentResponse(infoPay, null, null);

						}
					} else {
						infoPay = new InfoPay(Constants.ResponseCodes.INVALID_DATA,
								Constants.ResponseDescription.INVALID_INPUT_DATA, rrn, stan);
						billPaymentResponse = new BillPaymentResponse(infoPay, null, null);
					}

				} else {
					infoPay = new InfoPay(Constants.ResponseCodes.BILLER_NOT_FOUND_DISABLED,
							Constants.ResponseDescription.BILLER_NOT_FOUND_DISABLED, rrn, stan);
					billPaymentResponse = new BillPaymentResponse(infoPay, null, null);
				}
			} else {
				infoPay = new InfoPay(Constants.ResponseCodes.INVALID_BILLER_ID,
						Constants.ResponseDescription.INVALID_BILLER_ID, rrn, stan);
				billPaymentResponse = new BillPaymentResponse(infoPay, null, null);
			}
		}

		catch (

		Exception ex) {
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

			// tran Auth Id Validation - Stop Duplication
			paymentHistory = paymentLogRepository.findByTranAuthIdAndActivity(request.getTxnInfo().getTranAuthId(),
					Constants.ACTIVITY.BillPayment);
			if (paymentHistory != null && !paymentHistory.isEmpty()) {
				LOG.info("Duplicate/Invalid Tran-Auth Id ");
				response = new BillPaymentValidationResponse(Constants.ResponseCodes.DUPLICATE_TRANSACTION_AUTH_ID,
						Constants.ResponseDescription.DUPLICATE_TRANSACTION_AUTH_ID, rrn, stan);
				return response;
			}

			if (request.getTxnInfo().getBillerId() != null || !request.getTxnInfo().getBillerId().isEmpty()) {

				Optional<BillerConfiguration> billerConfiguration = billerConfigurationRepo
						.findByBillerId(request.getTxnInfo().getBillerId().substring(0, 2));

				if (!billerConfiguration.isPresent()) {

					response = new BillPaymentValidationResponse(Constants.ResponseCodes.INVALID_DATA,
							Constants.ResponseDescription.INVALID_DATA, rrn, stan);
					return response;
				}
			}

//			if (request.getTerminalInfo().getProvince() == null || request.getTerminalInfo().getProvince().isEmpty()) {
//				response = new BillPaymentValidationResponse(Constants.ResponseCodes.INVALID_DATA,
//						Constants.ResponseDescription.INVALID_DATA, rrn, stan);
//				return response;
//			} else {
//				provinceTransaction = provinceTransactionDao
//						.findByProvinceCode(request.getTerminalInfo().getProvince());
//			}
//			if (provinceTransaction == null) {
//				response = new BillPaymentValidationResponse(Constants.ResponseCodes.INVALID_DATA,
//						Constants.ResponseDescription.INVALID_DATA, rrn, stan);
//				return response;
//			}

			response = new BillPaymentValidationResponse("00", "SUCCESS", username, channel, provinceTransaction, rrn,
					stan);

		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return response;

	}

	public BillPaymentResponse billPaymentBEOE(BillPaymentRequest request, HttpServletRequest httpRequestData) {

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
		String transAuthId = request.getTxnInfo().getTranAuthId();
		String paymentRefrence = utilMethods.getRRN();
		BigDecimal inquiryTotalAmountbdUp = null;

		String channel = "";
		String username = "";

		try {

			String[] result = jwtTokenUtil.getTokenInformation(httpRequestData);
			username = result[0];
			channel = result[1];

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

					inquiryTotalAmountbdUp = new BigDecimal(
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
											request.getTxnInfo().getBillNumber(), paymentRefrence);

									additionalInfoPay = new AdditionalInfoPay(
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

									response = new BillPaymentResponse(infoPay, txnInfoPay, additionalInfoPay);
									transactionStatus = Constants.Status.Success;
									billStatus = Constants.BILL_STATUS.BILL_PAID;

								} else if (updateVoucherResponse.getResponse().getResponse_code().equals("402")) {
									infoPay = new InfoPay(Constants.ResponseCodes.DUPLICATE_TRANSACTION,
											Constants.ResponseDescription.DUPLICATE_TRANSACTION, rrn, stan);
									txnInfoPay = new TxnInfoPay(request.getTxnInfo().getBillerId(),
											request.getTxnInfo().getBillNumber(), paymentRefrence);// paymentRefrence
									additionalInfoPay = new AdditionalInfoPay(
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
									response = new BillPaymentResponse(infoPay, txnInfoPay, additionalInfoPay);
									transactionStatus = Constants.Status.Fail;
								} else {
									infoPay = new InfoPay(Constants.ResponseCodes.SERVICE_FAIL,
											Constants.ResponseDescription.SERVICE_FAIL, rrn, stan);
									txnInfoPay = new TxnInfoPay(request.getTxnInfo().getBillerId(),
											request.getTxnInfo().getBillNumber(), paymentRefrence);// paymentRefrence
									additionalInfoPay = new AdditionalInfoPay(
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
													request.getTxnInfo().getBillNumber(), paymentRefrence);// paymentRefrence
											additionalInfoPay = new AdditionalInfoPay(
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

											infoPay = new InfoPay(getVoucherResponse.getResponse().getResponse_code(),
													getVoucherResponse.getResponse().getResponse_desc(), rrn, stan);
											response = new BillPaymentResponse(infoPay, txnInfoPay, additionalInfoPay);
											transactionStatus = Constants.Status.Success;
											billStatus = Constants.BILL_STATUS.BILL_PAID;
										} else {
											infoPay = new InfoPay(Constants.ResponseCodes.SERVICE_FAIL,
													Constants.ResponseDescription.SERVICE_FAIL, rrn, stan);
											txnInfoPay = new TxnInfoPay(request.getTxnInfo().getBillerId(),
													request.getTxnInfo().getBillNumber(), paymentRefrence);// paymentRefrence
											additionalInfoPay = new AdditionalInfoPay(
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
											response = new BillPaymentResponse(infoPay, txnInfoPay, additionalInfoPay);
										}
									} else {
										infoPay = new InfoPay(Constants.ResponseCodes.SERVICE_FAIL,
												Constants.ResponseDescription.SERVICE_FAIL, rrn, stan);
										txnInfoPay = new TxnInfoPay(request.getTxnInfo().getBillerId(),
												request.getTxnInfo().getBillNumber(), paymentRefrence);// paymentRefrence
										additionalInfoPay = new AdditionalInfoPay(
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
										response = new BillPaymentResponse(infoPay, txnInfoPay, additionalInfoPay);
									}
								} else {
									infoPay = new InfoPay(Constants.ResponseCodes.SERVICE_FAIL,
											Constants.ResponseDescription.SERVICE_FAIL, rrn, stan);
									txnInfoPay = new TxnInfoPay(request.getTxnInfo().getBillerId(),
											request.getTxnInfo().getBillNumber(), paymentRefrence);// paymentRefrence
									additionalInfoPay = new AdditionalInfoPay(
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
								request.getTxnInfo().getBillNumber(), paymentRefrence);// paymentRefrence
						additionalInfoPay = new AdditionalInfoPay(request.getAdditionalInfo().getReserveField1(),
								request.getAdditionalInfo().getReserveField2(),
								request.getAdditionalInfo().getReserveField3(),
								request.getAdditionalInfo().getReserveField4(),
								request.getAdditionalInfo().getReserveField5(),
								request.getAdditionalInfo().getReserveField6(),
								request.getAdditionalInfo().getReserveField7(),
								request.getAdditionalInfo().getReserveField8(),
								request.getAdditionalInfo().getReserveField9(),
								request.getAdditionalInfo().getReserveField10());

						response = new BillPaymentResponse(infoPay, txnInfoPay, additionalInfoPay);
						transactionStatus = Constants.Status.Success;
					} else if (getVoucherResponse.getResponse().getGetvoucher().getStatus()
							.equalsIgnoreCase(Constants.BILL_STATUS.BILL_BLOCK)) {
						infoPay = new InfoPay(Constants.ResponseCodes.CONSUMER_NUMBER_BLOCK,
								Constants.ResponseDescription.CONSUMER_NUMBER_BLOCK, rrn, stan);
						txnInfoPay = new TxnInfoPay(request.getTxnInfo().getBillerId(),
								request.getTxnInfo().getBillNumber(), paymentRefrence);// paymentRefrence
						additionalInfoPay = new AdditionalInfoPay(request.getAdditionalInfo().getReserveField1(),
								request.getAdditionalInfo().getReserveField2(),
								request.getAdditionalInfo().getReserveField3(),
								request.getAdditionalInfo().getReserveField4(),
								request.getAdditionalInfo().getReserveField5(),
								request.getAdditionalInfo().getReserveField6(),
								request.getAdditionalInfo().getReserveField7(),
								request.getAdditionalInfo().getReserveField8(),
								request.getAdditionalInfo().getReserveField9(),
								request.getAdditionalInfo().getReserveField10());
						response = new BillPaymentResponse(infoPay, txnInfoPay, additionalInfoPay);
						transactionStatus = Constants.Status.Fail;
					}
				} else if (getVoucherResponse.getResponse().getResponse_code().equals("404")) {
					infoPay = new InfoPay(Constants.ResponseCodes.CONSUMER_NUMBER_NOT_EXISTS,
							Constants.ResponseDescription.CONSUMER_NUMBER_NOT_EXISTS, rrn, stan);
					txnInfoPay = new TxnInfoPay(request.getTxnInfo().getBillerId(),
							request.getTxnInfo().getBillNumber(), paymentRefrence);// paymentRefrence
					additionalInfoPay = new AdditionalInfoPay(request.getAdditionalInfo().getReserveField1(),
							request.getAdditionalInfo().getReserveField2(),
							request.getAdditionalInfo().getReserveField3(),
							request.getAdditionalInfo().getReserveField4(),
							request.getAdditionalInfo().getReserveField5(),
							request.getAdditionalInfo().getReserveField6(),
							request.getAdditionalInfo().getReserveField7(),
							request.getAdditionalInfo().getReserveField8(),
							request.getAdditionalInfo().getReserveField9(),
							request.getAdditionalInfo().getReserveField10());
					response = new BillPaymentResponse(infoPay, txnInfoPay, additionalInfoPay);
					transactionStatus = Constants.Status.Fail;

				} else {
					infoPay = new InfoPay(Constants.ResponseCodes.SERVICE_FAIL,
							Constants.ResponseDescription.SERVICE_FAIL, rrn, stan);
					txnInfoPay = new TxnInfoPay(request.getTxnInfo().getBillerId(),
							request.getTxnInfo().getBillNumber(), paymentRefrence);// paymentRefrence
					additionalInfoPay = new AdditionalInfoPay(request.getAdditionalInfo().getReserveField1(),
							request.getAdditionalInfo().getReserveField2(),
							request.getAdditionalInfo().getReserveField3(),
							request.getAdditionalInfo().getReserveField4(),
							request.getAdditionalInfo().getReserveField5(),
							request.getAdditionalInfo().getReserveField6(),
							request.getAdditionalInfo().getReserveField7(),
							request.getAdditionalInfo().getReserveField8(),
							request.getAdditionalInfo().getReserveField9(),
							request.getAdditionalInfo().getReserveField10());
					response = new BillPaymentResponse(infoPay, txnInfoPay, additionalInfoPay);
					transactionStatus = Constants.Status.Fail;
				}

			} else {
				infoPay = new InfoPay(Constants.ResponseCodes.BAD_TRANSACTION,
						Constants.ResponseDescription.BAD_TRANSACTION, rrn, stan);
				txnInfoPay = new TxnInfoPay(request.getTxnInfo().getBillerId(), request.getTxnInfo().getBillNumber(),
						paymentRefrence);// paymentRefrence
				additionalInfoPay = new AdditionalInfoPay(request.getAdditionalInfo().getReserveField1(),
						request.getAdditionalInfo().getReserveField2(), request.getAdditionalInfo().getReserveField3(),
						request.getAdditionalInfo().getReserveField4(), request.getAdditionalInfo().getReserveField5(),
						request.getAdditionalInfo().getReserveField6(), request.getAdditionalInfo().getReserveField7(),
						request.getAdditionalInfo().getReserveField8(), request.getAdditionalInfo().getReserveField9(),
						request.getAdditionalInfo().getReserveField10());
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
						request.getInfo().getRrn(), request.getTxnInfo().getBillerId(),
						request.getTxnInfo().getBillNumber(), channel, username);

			} catch (Exception ex) {
				LOG.error("{}", ex);
			}
			try {

				paymentLoggingService.paymentLog(responseDate, responseDate, rrn, stan,
						response.getInfo().getResponseCode(), response.getInfo().getResponseDesc(), cnic,
						request.getTerminalInfo().getMobile(), name, request.getTxnInfo().getBillNumber(),
						request.getTxnInfo().getBillerId(), inquiryTotalAmountbdUp, dbTransactionFees,
						Constants.ACTIVITY.BillPayment, paymentRefrence, request.getTxnInfo().getBillNumber(),
						transactionStatus, address, transactionFees, dbTax, dbTotal, channel, billStatus,
						request.getTxnInfo().getTranDate(), request.getTxnInfo().getTranTime(), province, transAuthId);

			} catch (Exception ex) {
				LOG.error("{}", ex);
			}
		}

		UtilMethods.generalLog("OUT -  Bill Payment Response {}" + response, LOG);

		return response;

	}

	public BillPaymentResponse billPaymentPral(BillPaymentRequest request, HttpServletRequest httpRequestData) {

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
		BigDecimal inquiryTotalAmountbdUp = null;

		try {
			String[] result = jwtTokenUtil.getTokenInformation(httpRequestData);
			username = result[0];
			channel = result[1];

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

					inquiryTotalAmountbdUp = new BigDecimal(
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
											request.getAdditionalInfo().getReserveField5(),
											request.getAdditionalInfo().getReserveField6(),
											request.getAdditionalInfo().getReserveField7(),
											request.getAdditionalInfo().getReserveField8(),
											request.getAdditionalInfo().getReserveField9(),
											request.getAdditionalInfo().getReserveField10());

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
													request.getTxnInfo().getBillNumber(), paymentRefrence);
											additionalInfoPay = new AdditionalInfoPay(
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
						request.getInfo().getRrn(), request.getTxnInfo().getBillerId(),
						request.getTxnInfo().getBillNumber(), channel, username);

			} catch (Exception ex) {
				LOG.error("{}", ex);
			}
			try {

				paymentLoggingService.paymentLog(responseDate, responseDate, rrn, stan,
						response.getInfo().getResponseCode(), response.getInfo().getResponseDesc(), cnic,
						request.getTerminalInfo().getMobile(), name, request.getTxnInfo().getBillNumber(),
						request.getTxnInfo().getBillerId(), inquiryTotalAmountbdUp, dbTransactionFees,
						Constants.ACTIVITY.BillPayment, paymentRefrence, request.getTxnInfo().getBillNumber(),
						transactionStatus, address, transactionFees, dbTax, dbTotal, channel, billStatus,
						request.getTxnInfo().getTranDate(), request.getTxnInfo().getTranTime(), province, transAuthId);

			} catch (Exception ex) {
				LOG.error("{}", ex);
			}
		}

		UtilMethods.generalLog("OUT -  Bill Payment Response {}" + response, LOG);

		return response;

	}

	@Override
	public BillPaymentResponse billPaymentOffline(BillPaymentRequest request, HttpServletRequest httpRequestData,
			String parentBiller, String subBiller) {

		LOG.info("Inside method billPaymentOffline");
		BillPaymentResponse response = null;
		Date responseDate = new Date();
		OfflineUpdateVoucherResponse OfflineUpdateVoucherResponse = null;
		Date strDate = new Date();

		OfflineGetVoucherResponse getVoucherResponse = null;
		InfoPay infoPay = null;
		Info info = null;
		TxnInfoPay txnInfoPay = null;
		AdditionalInfoPay additionalInfoPay = null;
		String transactionStatus = "";
		double transactionFees = 0;
		String cnic = "";
		String mobile = "";
		String address = "";
		String name = "";
		String billStatus = "";
		double amountInDueToDate = 0; // dbAmount = 0;
		double dbTax = 0;
		double dbTransactionFees = 0;
		double dbTotal = 0;
		String province = "";
		String rrn = request.getInfo().getRrn();
		LOG.info("RRN :{ }", rrn);
		String stan = request.getInfo().getStan();
		// String transAuthId = "";
		String transAuthId = request.getTxnInfo().getTranAuthId();
		String paymentRefrence = "";
		String billingDate = "";
		String billingMonth = "";
		String dueDateStr = "";
		String expiryDate = "";
		String amountPaidInDueDate = "";
		String amountPaidAfterDueDate = "";
		String paymentwithinduedateflag = "false";

		String channel = "";
		String username = "";
		BigDecimal requestAmount = null;
		try {

			String[] result = jwtTokenUtil.getTokenInformation(httpRequestData);
			username = result[0];
			channel = result[1];

			ArrayList<String> inquiryParams = new ArrayList<String>();
			inquiryParams.add(Constants.MPAY_REQUEST_METHODS.OFFLINE_BILLER_INQUIRY.trim());
			inquiryParams.add(parentBiller.trim());
			inquiryParams.add(subBiller.trim());
			inquiryParams.add(request.getTxnInfo().getBillNumber().trim());
			inquiryParams.add(channel);// channel
			inquiryParams.add(rrn);
			inquiryParams.add(stan);

			getVoucherResponse = serviceCaller.get(inquiryParams, OfflineGetVoucherResponse.class, rrn,
					Constants.ACTIVITY.BillInquiry);

			if (getVoucherResponse != null) {

				if (getVoucherResponse.getResponse().getResponseCode().equals(ResponseCodes.OK)) {

					double amountAfterDueDate = 0;
					String billstatus = "";

					BigDecimal requestAmountafterduedate = null;
					if (getVoucherResponse.getResponse().getOfflineBillerGetvoucher() != null) {

						String amountStr = getVoucherResponse.getResponse().getOfflineBillerGetvoucher().getGetvoucher()
								.getAmount();
						String amountAfterDueDateStr = getVoucherResponse.getResponse().getOfflineBillerGetvoucher()
								.getGetvoucher().getAmountAfterDueDate();

						if (!amountStr.isEmpty()) {
							requestAmount = BigDecimal.valueOf(Double.parseDouble(amountStr)).setScale(2,
									RoundingMode.UP);
							amountInDueToDate = utilMethods.bigDecimalToDouble(requestAmount);
							// amountPaidInDueDate = utilMethods.formatAmount(requestAmount, 12);
						}

						if (!amountAfterDueDateStr.isEmpty()) {
							requestAmountafterduedate = BigDecimal.valueOf(Double.parseDouble(amountAfterDueDateStr))
									.setScale(2, RoundingMode.UP);
							amountAfterDueDate = utilMethods.bigDecimalToDouble(requestAmountafterduedate);
							// amountPaidAfterDueDate = utilMethods.formatAmount(requestAmountafterduedate,
							// 12);
						}

						name = getVoucherResponse.getResponse().getOfflineBillerGetvoucher().getGetvoucher().getName();

						billingDate = getVoucherResponse.getResponse().getOfflineBillerGetvoucher().getGetvoucher()
								.getBillingDate();
						billingMonth = getVoucherResponse.getResponse().getOfflineBillerGetvoucher().getGetvoucher()
								.getBillingMonth();
						dueDateStr = getVoucherResponse.getResponse().getOfflineBillerGetvoucher().getGetvoucher()
								.getDueDate();
						expiryDate = getVoucherResponse.getResponse().getOfflineBillerGetvoucher().getGetvoucher()
								.getExpiryDate();
						billStatus = getVoucherResponse.getResponse().getOfflineBillerGetvoucher().getGetvoucher()
								.getBillStatus();
						// dbAmount = requestAmount.doubleValue();

						if (utilMethods.isValidInput(dueDateStr)) {
							LocalDate currentDate = LocalDate.now();

							try {
								LocalDate dueDate = utilMethods.parseDueDate(dueDateStr);

								// Check due date conditions
								if (utilMethods.isPaymentWithinDueDate(currentDate, dueDate)) {
									if (Double.valueOf(request.getTxnInfo().getTranAmount())
											.compareTo(amountInDueToDate) != 0) {
										infoPay = new InfoPay(Constants.ResponseCodes.AMMOUNT_MISMATCH,
												Constants.ResponseDescription.AMMOUNT_MISMATCH, rrn, stan);
										response = new BillPaymentResponse(infoPay, null, null);
										return response;
									}
								} else {
									paymentwithinduedateflag = "true";
									if (Double.valueOf(request.getTxnInfo().getTranAmount())
											.compareTo(amountAfterDueDate) != 0) {
										infoPay = new InfoPay(Constants.ResponseCodes.AMMOUNT_MISMATCH,
												Constants.ResponseDescription.AMMOUNT_MISMATCH, rrn, stan);
										response = new BillPaymentResponse(infoPay, null, null);
										return response;
									}
								}
							} catch (DateTimeParseException e) {
								LOG.error("Error parsing due date: " + e.getMessage());
							}
						} else {
							LOG.info("Invalid due date input");
							if (Double.valueOf(request.getTxnInfo().getTranAmount())
									.compareTo(amountInDueToDate) != 0) {
								infoPay = new InfoPay(Constants.ResponseCodes.AMMOUNT_MISMATCH,
										Constants.ResponseDescription.AMMOUNT_MISMATCH, rrn, stan);
								response = new BillPaymentResponse(infoPay, null, null);
								return response;
							}
						}

						if (getVoucherResponse.getResponse().getOfflineBillerGetvoucher().getGetvoucher()
								.getBillStatus().equalsIgnoreCase(Constants.BILL_STATUS.BILL_UNPAID)) {

							try {

								String tranDate = request.getTxnInfo().getTranDate();

								if (tranDate.length() == 8) {
									tranDate = utilMethods.transactionDateFormater(tranDate);
								}
								LOG.info("Calling UpdateVoucher");

								ArrayList<String> ubpsBillParams = new ArrayList<>();
								ubpsBillParams.add(Constants.MPAY_REQUEST_METHODS.OFFLINE_BILLER_PAYMENT.trim());
								ubpsBillParams.add(parentBiller.trim());
								ubpsBillParams.add(subBiller.trim());
								ubpsBillParams.add(request.getTxnInfo().getBillNumber().trim());
								ubpsBillParams.add(channel);// channel
								ubpsBillParams.add(request.getTxnInfo().getTranAmount().trim());
								ubpsBillParams.add(request.getAdditionalInfo().getReserveField1());
								ubpsBillParams.add(request.getAdditionalInfo().getReserveField2());
								ubpsBillParams.add(request.getAdditionalInfo().getReserveField3());
								ubpsBillParams.add(request.getAdditionalInfo().getReserveField4());
								ubpsBillParams.add(request.getAdditionalInfo().getReserveField5());
								ubpsBillParams.add(paymentwithinduedateflag);
								ubpsBillParams.add(rrn);
								ubpsBillParams.add(stan);

								OfflineUpdateVoucherResponse = serviceCaller.get(ubpsBillParams,
										OfflineUpdateVoucherResponse.class, rrn, Constants.ACTIVITY.BillPayment);

								if (OfflineUpdateVoucherResponse != null) {
									infoPay = new InfoPay(OfflineUpdateVoucherResponse.getResponse().getResponse_code(),
											OfflineUpdateVoucherResponse.getResponse().getResponse_desc(), rrn, stan);
									if (OfflineUpdateVoucherResponse.getResponse().getResponse_code()
											.equals(ResponseCodes.OK)) {
										paymentRefrence = utilMethods.getRRN();
										txnInfoPay = new TxnInfoPay(request.getTxnInfo().getBillerId(),
												request.getTxnInfo().getBillNumber(), paymentRefrence);
										additionalInfoPay = new AdditionalInfoPay(
												request.getAdditionalInfo().getReserveField1(),
												request.getAdditionalInfo().getReserveField2(),
												request.getAdditionalInfo().getReserveField3(),
												request.getAdditionalInfo().getReserveField4(),
												request.getAdditionalInfo().getReserveField5());
//												request.getAdditionalInfo().getReserveField6(),
//												request.getAdditionalInfo().getReserveField7(),
//												request.getAdditionalInfo().getReserveField8(),
//												request.getAdditionalInfo().getReserveField9(),
//												request.getAdditionalInfo().getReserveField10());

										response = new BillPaymentResponse(infoPay, txnInfoPay, additionalInfoPay);
										transactionStatus = Constants.Status.Success;
										billStatus = Constants.BILL_STATUS.BILL_PAID;

									} else if (OfflineUpdateVoucherResponse.getResponse().getResponse_code()
											.equals("402")) {
										infoPay = new InfoPay(Constants.ResponseCodes.DUPLICATE_TRANSACTION,
												Constants.ResponseDescription.DUPLICATE_TRANSACTION, rrn, stan);
										txnInfoPay = new TxnInfoPay(request.getTxnInfo().getBillerId(),
												request.getTxnInfo().getBillNumber(), paymentRefrence);// paymentRefrence
										additionalInfoPay = new AdditionalInfoPay(
												request.getAdditionalInfo().getReserveField1(),
												request.getAdditionalInfo().getReserveField2(),
												request.getAdditionalInfo().getReserveField3(),
												request.getAdditionalInfo().getReserveField4(),
												request.getAdditionalInfo().getReserveField5());
//												request.getAdditionalInfo().getReserveField6(),
//												request.getAdditionalInfo().getReserveField7(),
//												request.getAdditionalInfo().getReserveField8(),
//												request.getAdditionalInfo().getReserveField9(),
//												request.getAdditionalInfo().getReserveField10());
										response = new BillPaymentResponse(infoPay, txnInfoPay, additionalInfoPay);
										transactionStatus = Constants.Status.Fail;
									} else {
										infoPay = new InfoPay(Constants.ResponseCodes.SERVICE_FAIL,
												Constants.ResponseDescription.SERVICE_FAIL, rrn, stan);
										txnInfoPay = new TxnInfoPay(request.getTxnInfo().getBillerId(),
												request.getTxnInfo().getBillNumber(), paymentRefrence);// paymentRefrence
										additionalInfoPay = new AdditionalInfoPay(
												request.getAdditionalInfo().getReserveField1(),
												request.getAdditionalInfo().getReserveField2(),
												request.getAdditionalInfo().getReserveField3(),
												request.getAdditionalInfo().getReserveField4(),
												request.getAdditionalInfo().getReserveField5());
//												request.getAdditionalInfo().getReserveField6(),
//												request.getAdditionalInfo().getReserveField7(),
//												request.getAdditionalInfo().getReserveField8(),
//												request.getAdditionalInfo().getReserveField9(),
//												request.getAdditionalInfo().getReserveField10());
										;
										response = new BillPaymentResponse(infoPay, txnInfoPay, additionalInfoPay);
									}

								} else { // Second time bill inquiry to pe call from here and if bill is paid then
											// return
											// success

									getVoucherResponse = serviceCaller.get(inquiryParams,
											OfflineGetVoucherResponse.class, stan, Constants.ACTIVITY.BillInquiry);

									if (getVoucherResponse != null) {
										if (getVoucherResponse.getResponse().getResponseCode()
												.equals(ResponseCodes.OK)) {
											if (getVoucherResponse.getResponse().getOfflineBillerGetvoucher()
													.getGetvoucher().getBillStatus()
													.equalsIgnoreCase(Constants.BILL_STATUS.BILL_PAID)) {
												paymentRefrence = utilMethods.getRRN();
												txnInfoPay = new TxnInfoPay(request.getTxnInfo().getBillerId(),
														request.getTxnInfo().getBillNumber(), paymentRefrence);// paymentRefrence
												additionalInfoPay = new AdditionalInfoPay(
														request.getAdditionalInfo().getReserveField1(),
														request.getAdditionalInfo().getReserveField2(),
														request.getAdditionalInfo().getReserveField3(),
														request.getAdditionalInfo().getReserveField4(),
														request.getAdditionalInfo().getReserveField5());
//														request.getAdditionalInfo().getReserveField6(),
//														request.getAdditionalInfo().getReserveField7(),
//														request.getAdditionalInfo().getReserveField8(),
//														request.getAdditionalInfo().getReserveField9(),
//														request.getAdditionalInfo().getReserveField10());

												infoPay = new InfoPay(
														getVoucherResponse.getResponse().getResponseCode(),
														getVoucherResponse.getResponse().getResponseCode(), rrn, stan);
												response = new BillPaymentResponse(infoPay, txnInfoPay,
														additionalInfoPay);
												transactionStatus = Constants.Status.Success;
												billStatus = Constants.BILL_STATUS.BILL_PAID;
											} else {
												infoPay = new InfoPay(Constants.ResponseCodes.SERVICE_FAIL,
														Constants.ResponseDescription.SERVICE_FAIL, rrn, stan);
												txnInfoPay = new TxnInfoPay(request.getTxnInfo().getBillerId(),
														request.getTxnInfo().getBillNumber(), paymentRefrence);// paymentRefrence
												additionalInfoPay = new AdditionalInfoPay(
														request.getAdditionalInfo().getReserveField1(),
														request.getAdditionalInfo().getReserveField2(),
														request.getAdditionalInfo().getReserveField3(),
														request.getAdditionalInfo().getReserveField4(),
														request.getAdditionalInfo().getReserveField5());
//														request.getAdditionalInfo().getReserveField6(),
//														request.getAdditionalInfo().getReserveField7(),
//														request.getAdditionalInfo().getReserveField8(),
//														request.getAdditionalInfo().getReserveField9(),
//														request.getAdditionalInfo().getReserveField10());
												response = new BillPaymentResponse(infoPay, txnInfoPay,
														additionalInfoPay);
											}
										} else {
											infoPay = new InfoPay(Constants.ResponseCodes.SERVICE_FAIL,
													Constants.ResponseDescription.SERVICE_FAIL, rrn, stan);
											txnInfoPay = new TxnInfoPay(request.getTxnInfo().getBillerId(),
													request.getTxnInfo().getBillNumber(), paymentRefrence);// paymentRefrence
											additionalInfoPay = new AdditionalInfoPay(
													request.getAdditionalInfo().getReserveField1(),
													request.getAdditionalInfo().getReserveField2(),
													request.getAdditionalInfo().getReserveField3(),
													request.getAdditionalInfo().getReserveField4(),
													request.getAdditionalInfo().getReserveField5());
//													request.getAdditionalInfo().getReserveField6(),
//													request.getAdditionalInfo().getReserveField7(),
//													request.getAdditionalInfo().getReserveField8(),
//													request.getAdditionalInfo().getReserveField9(),
//													request.getAdditionalInfo().getReserveField10());
											response = new BillPaymentResponse(infoPay, txnInfoPay, additionalInfoPay);
										}
									} else {
										infoPay = new InfoPay(Constants.ResponseCodes.SERVICE_FAIL,
												Constants.ResponseDescription.SERVICE_FAIL, rrn, stan);
										txnInfoPay = new TxnInfoPay(request.getTxnInfo().getBillerId(),
												request.getTxnInfo().getBillNumber(), paymentRefrence);// paymentRefrence
										additionalInfoPay = new AdditionalInfoPay(
												request.getAdditionalInfo().getReserveField1(),
												request.getAdditionalInfo().getReserveField2(),
												request.getAdditionalInfo().getReserveField3(),
												request.getAdditionalInfo().getReserveField4(),
												request.getAdditionalInfo().getReserveField5());
//												request.getAdditionalInfo().getReserveField6(),
//												request.getAdditionalInfo().getReserveField7(),
//												request.getAdditionalInfo().getReserveField8(),
//												request.getAdditionalInfo().getReserveField9(),
//												request.getAdditionalInfo().getReserveField10());
										response = new BillPaymentResponse(infoPay, txnInfoPay, additionalInfoPay);
										transactionStatus = Constants.Status.Fail;
									}

								}

							} catch (Exception ex) {
								LOG.error("{}", ex);
							}

						} else if (getVoucherResponse.getResponse().getOfflineBillerGetvoucher().getGetvoucher()
								.getBillStatus().equalsIgnoreCase(Constants.BILL_STATUS.BILL_PAID)) {
							infoPay = new InfoPay(Constants.ResponseCodes.BILL_ALREADY_PAID,
									Constants.ResponseDescription.BILL_ALREADY_PAID, rrn, stan);
							txnInfoPay = new TxnInfoPay(request.getTxnInfo().getBillerId(),
									request.getTxnInfo().getBillNumber(), paymentRefrence);// paymentRefrence
							additionalInfoPay = new AdditionalInfoPay(request.getAdditionalInfo().getReserveField1(),
									request.getAdditionalInfo().getReserveField2(),
									request.getAdditionalInfo().getReserveField3(),
									request.getAdditionalInfo().getReserveField4(),
									request.getAdditionalInfo().getReserveField5());
//									request.getAdditionalInfo().getReserveField6(),
//									request.getAdditionalInfo().getReserveField7(),
//									request.getAdditionalInfo().getReserveField8(),
//									request.getAdditionalInfo().getReserveField9(),
//									request.getAdditionalInfo().getReserveField10());

							response = new BillPaymentResponse(infoPay, txnInfoPay, additionalInfoPay);
							transactionStatus = Constants.Status.Success;
						} else if (getVoucherResponse.getResponse().getOfflineBillerGetvoucher().getGetvoucher()
								.getBillStatus().equalsIgnoreCase(Constants.BILL_STATUS.BILL_EXPIRED)) {
							infoPay = new InfoPay(Constants.BILL_STATUS.BILL_EXPIRED,
									Constants.ResponseDescription.CONSUMER_NUMBER_Expired, rrn, stan);
							txnInfoPay = new TxnInfoPay(request.getTxnInfo().getBillerId(),
									request.getTxnInfo().getBillNumber(), paymentRefrence);// paymentRefrence
							additionalInfoPay = new AdditionalInfoPay(request.getAdditionalInfo().getReserveField1(),
									request.getAdditionalInfo().getReserveField2(),
									request.getAdditionalInfo().getReserveField3(),
									request.getAdditionalInfo().getReserveField4(),
									request.getAdditionalInfo().getReserveField5());
//									request.getAdditionalInfo().getReserveField6(),
//									request.getAdditionalInfo().getReserveField7(),
//									request.getAdditionalInfo().getReserveField8(),
//									request.getAdditionalInfo().getReserveField9(),
//									request.getAdditionalInfo().getReserveField10());
							response = new BillPaymentResponse(infoPay, txnInfoPay, additionalInfoPay);
							transactionStatus = Constants.Status.Fail;
						}
					} else if (getVoucherResponse.getResponse().getResponseCode()
							.equals(Constants.ResponseCodes.NOT_FOUND)) {
						infoPay = new InfoPay(Constants.ResponseCodes.CONSUMER_NUMBER_NOT_EXISTS,
								Constants.ResponseDescription.CONSUMER_NUMBER_NOT_EXISTS, rrn, stan);
						txnInfoPay = new TxnInfoPay(request.getTxnInfo().getBillerId(),
								request.getTxnInfo().getBillNumber(), paymentRefrence);// paymentRefrence
						additionalInfoPay = new AdditionalInfoPay(request.getAdditionalInfo().getReserveField1(),
								request.getAdditionalInfo().getReserveField2(),
								request.getAdditionalInfo().getReserveField3(),
								request.getAdditionalInfo().getReserveField4(),
								request.getAdditionalInfo().getReserveField5());
//								request.getAdditionalInfo().getReserveField6(),
//								request.getAdditionalInfo().getReserveField7(),
//								request.getAdditionalInfo().getReserveField8(),
//								request.getAdditionalInfo().getReserveField9(),
//								request.getAdditionalInfo().getReserveField10());
						response = new BillPaymentResponse(infoPay, txnInfoPay, additionalInfoPay);
						transactionStatus = Constants.Status.Fail;

					} else {
						infoPay = new InfoPay(Constants.ResponseCodes.SERVICE_FAIL,
								Constants.ResponseDescription.SERVICE_FAIL, rrn, stan);
						txnInfoPay = new TxnInfoPay(request.getTxnInfo().getBillerId(),
								request.getTxnInfo().getBillNumber(), paymentRefrence);// paymentRefrence
						additionalInfoPay = new AdditionalInfoPay(request.getAdditionalInfo().getReserveField1(),
								request.getAdditionalInfo().getReserveField2(),
								request.getAdditionalInfo().getReserveField3(),
								request.getAdditionalInfo().getReserveField4(),
								request.getAdditionalInfo().getReserveField5());
//								request.getAdditionalInfo().getReserveField6(),
//								request.getAdditionalInfo().getReserveField7(),
//								request.getAdditionalInfo().getReserveField8(),
//								request.getAdditionalInfo().getReserveField9(),
//								request.getAdditionalInfo().getReserveField10());
						response = new BillPaymentResponse(infoPay, txnInfoPay, additionalInfoPay);
						transactionStatus = Constants.Status.Fail;
					}

				} else if (getVoucherResponse.getResponse().getResponseCode()
						.equals(Constants.ResponseCodes.NOT_FOUND)) {
					infoPay = new InfoPay(Constants.ResponseCodes.CONSUMER_NUMBER_NOT_EXISTS,
							Constants.ResponseDescription.CONSUMER_NUMBER_NOT_EXISTS, rrn, stan);
					txnInfoPay = new TxnInfoPay(request.getTxnInfo().getBillerId(),
							request.getTxnInfo().getBillNumber(), paymentRefrence);// paymentRefrence
					additionalInfoPay = new AdditionalInfoPay(request.getAdditionalInfo().getReserveField1(),
							request.getAdditionalInfo().getReserveField2(),
							request.getAdditionalInfo().getReserveField3(),
							request.getAdditionalInfo().getReserveField4(),
							request.getAdditionalInfo().getReserveField5());
//							request.getAdditionalInfo().getReserveField6(),
//							request.getAdditionalInfo().getReserveField7(),
//							request.getAdditionalInfo().getReserveField8(),
//							request.getAdditionalInfo().getReserveField9(),
//							request.getAdditionalInfo().getReserveField10());
					response = new BillPaymentResponse(infoPay, txnInfoPay, additionalInfoPay);
					transactionStatus = Constants.Status.Fail;

				}

				else {
					infoPay = new InfoPay(Constants.ResponseCodes.BAD_TRANSACTION,
							Constants.ResponseDescription.BAD_TRANSACTION, rrn, stan);
					txnInfoPay = new TxnInfoPay(request.getTxnInfo().getBillerId(),
							request.getTxnInfo().getBillNumber(), paymentRefrence);// paymentRefrence
					additionalInfoPay = new AdditionalInfoPay(request.getAdditionalInfo().getReserveField1(),
							request.getAdditionalInfo().getReserveField2(),
							request.getAdditionalInfo().getReserveField3(),
							request.getAdditionalInfo().getReserveField4(),
							request.getAdditionalInfo().getReserveField5());
//							request.getAdditionalInfo().getReserveField6(),
//							request.getAdditionalInfo().getReserveField7(),
//							request.getAdditionalInfo().getReserveField8(),
//							request.getAdditionalInfo().getReserveField9(),
//							request.getAdditionalInfo().getReserveField10());
					response = new BillPaymentResponse(infoPay, txnInfoPay, additionalInfoPay);

					transactionStatus = Constants.Status.Fail;
					LOG.info("Calling Bill payment End");
				}
			} else {
				infoPay = new InfoPay(Constants.ResponseCodes.SERVICE_FAIL, Constants.ResponseDescription.SERVICE_FAIL,
						rrn, stan);
				response = new BillPaymentResponse(infoPay, null, null);

			}
		} catch (Exception ex) {

			LOG.error("Exception {}", ex);

		} finally {
			LOG.info("Bill Payment Response {}", response);
			try {

				String requestAsString = objectMapper.writeValueAsString(request);
				String responseAsString = objectMapper.writeValueAsString(response);

				auditLoggingService.auditLog(Constants.ACTIVITY.BillPayment, response.getInfo().getResponseCode(),
						response.getInfo().getResponseDesc(), requestAsString, responseAsString, strDate, strDate,
						request.getInfo().getRrn(), request.getTxnInfo().getBillerId(),
						request.getTxnInfo().getBillNumber(), channel, username);

			} catch (Exception ex) {
				LOG.error("{}", ex);
			}
			try {

				paymentLoggingService.paymentLog(responseDate, responseDate, rrn, stan,
						response.getInfo().getResponseCode(), response.getInfo().getResponseDesc(), cnic,
						request.getTerminalInfo().getMobile(), name, request.getTxnInfo().getBillNumber(),
						request.getTxnInfo().getBillerId(), requestAmount, dbTransactionFees,
						Constants.ACTIVITY.BillPayment, paymentRefrence, request.getTxnInfo().getBillNumber(),
						transactionStatus, address, transactionFees, dbTax, dbTotal, channel, billStatus,
						request.getTxnInfo().getTranDate(), request.getTxnInfo().getTranTime(), province, transAuthId);

			} catch (Exception ex) {
				LOG.error("{}", ex);
			}
		}

		UtilMethods.generalLog("OUT -  Bill Payment Response {}" + response, LOG);

		return response;

	}

	@Override
	public BillPaymentResponse billPaymentPta(BillPaymentRequest request, HttpServletRequest httpRequestData) {

		LOG.info("Inside billPaymentPta method ");
		BillPaymentResponse response = null;
		PtaGetVoucherResponse getVoucherResponse = null;
		Date responseDate = new Date();
		PtaUpdateVoucherResponse ptaUpdateVoucherResponse = null;
		Date strDate = new Date();
		InfoPay infoPay = null;
		Info info = null;
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
		String transAuthId = request.getTxnInfo().getTranAuthId();
		String paymentRefrence = "";
		String amountPaidInDueDate = "";
		String amountPaid = "";
		String depostiroName = "";
		String channel = "";
		String username = "";
		BigDecimal requestTotalAmountbdUp = null;

		try {
			String[] result = jwtTokenUtil.getTokenInformation(httpRequestData);
			username = result[0];
			channel = result[1];

			ArrayList<String> inquiryParams = new ArrayList<String>();
			inquiryParams.add(Constants.MPAY_REQUEST_METHODS.PTA_BILL_INQUIRY);
			inquiryParams.add(request.getTxnInfo().getBillNumber().trim());
			inquiryParams.add(rrn);
			inquiryParams.add(stan);

			getVoucherResponse = serviceCaller.get(inquiryParams, PtaGetVoucherResponse.class, rrn,
					Constants.ACTIVITY.BillInquiry);

			if (getVoucherResponse != null) {
				info = new Info(getVoucherResponse.getResponse().getResponseCode(),
						getVoucherResponse.getResponse().getResponseDesc(), rrn, stan);
				if (getVoucherResponse.getResponse().getResponseCode().equals(ResponseCodes.OK)) {

					double amountInDueToDate = 0;
					String amountAfterDueDate = "";
					String status = "";

					if (getVoucherResponse.getResponse().getPtaGetVoucher() != null) {

						DataWrapper dataWrapper = getVoucherResponse.getResponse().getPtaGetVoucher().getDataWrapper()
								.get(0);

						requestTotalAmountbdUp = BigDecimal.valueOf(Double.parseDouble(dataWrapper.getTotalAmount()))
								.setScale(2, RoundingMode.UP);
						amountInDueToDate = utilMethods.bigDecimalToDouble(requestTotalAmountbdUp);
						// amountPaidInDueDate = utilMethods.formatAmount(requestTotalAmountbdUp, 12);
						amountAfterDueDate = String.valueOf(amountInDueToDate);
						// amountPaid = amountPaidInDueDate;

						depostiroName = dataWrapper.getDepositorName();
						mobile = dataWrapper.getDepositorContactNo();
//						billStatus = dataWrapper.getStatus();
						billStatus = dataWrapper.getStatus().trim().equals("0") ? Constants.BILL_STATUS.BILL_UNPAID
								: Constants.BILL_STATUS.BILL_PAID;
						dbTotal = requestTotalAmountbdUp.doubleValue();
						dbAmount = requestTotalAmountbdUp.doubleValue();

						if (Double.valueOf(request.getTxnInfo().getTranAmount()).compareTo(dbAmount) != 0) {
							infoPay = new InfoPay(Constants.ResponseCodes.AMMOUNT_MISMATCH,
									Constants.ResponseDescription.AMMOUNT_MISMATCH, rrn, stan);
							response = new BillPaymentResponse(infoPay, null, null);
							return response;
						}
//						billStatus = dataWrapper.getStatus().trim().equals("0") ? Constants.BILL_STATUS.BILL_UNPAID
//								: Constants.BILL_STATUS.BILL_PAID;

						if (billStatus.equalsIgnoreCase(Constants.BILL_STATUS.BILL_UNPAID)) {

							try {

								String tranDate = request.getTxnInfo().getTranDate();

								if (tranDate.length() == 8) {
									tranDate = utilMethods.transactionDateFormater(tranDate);
								}
								LOG.info("Calling UpdateVoucher ");
								ArrayList<String> ubpsBillParams = new ArrayList<>();

								ubpsBillParams.add(Constants.MPAY_REQUEST_METHODS.PTA_BILL_PAYMENT);
								ubpsBillParams.add(request.getTxnInfo().getBillNumber().trim());
								ubpsBillParams.add(Constants.Status.VOUCHER_UPDATED);
								ubpsBillParams.add(rrn);
								ubpsBillParams.add(stan);

								ptaUpdateVoucherResponse = serviceCaller.get(ubpsBillParams,
										PtaUpdateVoucherResponse.class, rrn, Constants.ACTIVITY.BillPayment);

								if (ptaUpdateVoucherResponse != null) {
									infoPay = new InfoPay(ptaUpdateVoucherResponse.getResponse().getResponseCode(),
											ptaUpdateVoucherResponse.getResponse().getResponseDesc(), rrn, stan);
									if (ptaUpdateVoucherResponse.getResponse().getResponseCode()
											.equals(ResponseCodes.OK)) {
										paymentRefrence = utilMethods.getRRN();
										txnInfoPay = new TxnInfoPay(request.getTxnInfo().getBillerId(),
												request.getTxnInfo().getBillNumber(), paymentRefrence);
										additionalInfoPay = new AdditionalInfoPay(
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

										response = new BillPaymentResponse(infoPay, txnInfoPay, additionalInfoPay);
										transactionStatus = Constants.Status.Success;
										billStatus = Constants.BILL_STATUS.BILL_PAID;

									} else if (ptaUpdateVoucherResponse.getResponse().getResponseCode().equals("402")) {
										infoPay = new InfoPay(Constants.ResponseCodes.DUPLICATE_TRANSACTION,
												Constants.ResponseDescription.DUPLICATE_TRANSACTION, rrn, stan);
										txnInfoPay = new TxnInfoPay(request.getTxnInfo().getBillerId(),
												request.getTxnInfo().getBillNumber(), paymentRefrence);// paymentRefrence
										additionalInfoPay = new AdditionalInfoPay(
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
										response = new BillPaymentResponse(infoPay, txnInfoPay, additionalInfoPay);
										transactionStatus = Constants.Status.Fail;
									} else {
										infoPay = new InfoPay(Constants.ResponseCodes.SERVICE_FAIL,
												Constants.ResponseDescription.SERVICE_FAIL, rrn, stan);
										txnInfoPay = new TxnInfoPay(request.getTxnInfo().getBillerId(),
												request.getTxnInfo().getBillNumber(), paymentRefrence);// paymentRefrence
										additionalInfoPay = new AdditionalInfoPay(
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
										response = new BillPaymentResponse(infoPay, txnInfoPay, additionalInfoPay);
									}

								} else { // Second time bill inquiry to pe call from here and if bill is paid then
											// return
											// success

									getVoucherResponse = serviceCaller.get(inquiryParams, PtaGetVoucherResponse.class,
											stan, Constants.ACTIVITY.BillInquiry);

									if (getVoucherResponse != null) {
										if (getVoucherResponse.getResponse().getResponseCode()
												.equals(ResponseCodes.OK)) {
											billStatus = getVoucherResponse.getResponse().getPtaGetVoucher()
													.getDataWrapper().get(0).getStatus().trim().equals("0")
															? Constants.BILL_STATUS.BILL_UNPAID
															: Constants.BILL_STATUS.BILL_PAID;
											if (billStatus.equalsIgnoreCase(Constants.BILL_STATUS.BILL_PAID)) {
												paymentRefrence = utilMethods.getRRN();
												txnInfoPay = new TxnInfoPay(request.getTxnInfo().getBillerId(),
														request.getTxnInfo().getBillNumber(), paymentRefrence);// paymentRefrence
												additionalInfoPay = new AdditionalInfoPay(
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

												infoPay = new InfoPay(
														getVoucherResponse.getResponse().getResponseCode(),
														getVoucherResponse.getResponse().getResponseDesc(), rrn, stan);
												response = new BillPaymentResponse(infoPay, txnInfoPay,
														additionalInfoPay);
												transactionStatus = Constants.Status.Success;
												billStatus = Constants.BILL_STATUS.BILL_PAID;
											} else {
												infoPay = new InfoPay(Constants.ResponseCodes.SERVICE_FAIL,
														Constants.ResponseDescription.SERVICE_FAIL, rrn, stan);
												txnInfoPay = new TxnInfoPay(request.getTxnInfo().getBillerId(),
														request.getTxnInfo().getBillNumber(), paymentRefrence);// paymentRefrence
												additionalInfoPay = new AdditionalInfoPay(
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
												response = new BillPaymentResponse(infoPay, txnInfoPay,
														additionalInfoPay);
											}
										} else {
											infoPay = new InfoPay(Constants.ResponseCodes.SERVICE_FAIL,
													Constants.ResponseDescription.SERVICE_FAIL, rrn, stan);
											txnInfoPay = new TxnInfoPay(request.getTxnInfo().getBillerId(),
													request.getTxnInfo().getBillNumber(), paymentRefrence);// paymentRefrence
											additionalInfoPay = new AdditionalInfoPay(
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
											response = new BillPaymentResponse(infoPay, txnInfoPay, additionalInfoPay);
										}
									} else {
										infoPay = new InfoPay(Constants.ResponseCodes.SERVICE_FAIL,
												Constants.ResponseDescription.SERVICE_FAIL, rrn, stan);
										txnInfoPay = new TxnInfoPay(request.getTxnInfo().getBillerId(),
												request.getTxnInfo().getBillNumber(), paymentRefrence);// paymentRefrence
										additionalInfoPay = new AdditionalInfoPay(
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
										response = new BillPaymentResponse(infoPay, txnInfoPay, additionalInfoPay);
										transactionStatus = Constants.Status.Fail;
									}

								}

							} catch (Exception ex) {
								LOG.error("{}", ex);
							}

						} else if (billStatus.equalsIgnoreCase(Constants.BILL_STATUS.BILL_PAID)) {
							infoPay = new InfoPay(Constants.ResponseCodes.BILL_ALREADY_PAID,
									Constants.ResponseDescription.BILL_ALREADY_PAID, rrn, stan);
							txnInfoPay = new TxnInfoPay(request.getTxnInfo().getBillerId(),
									request.getTxnInfo().getBillNumber(), paymentRefrence);// paymentRefrence
							additionalInfoPay = new AdditionalInfoPay(request.getAdditionalInfo().getReserveField1(),
									request.getAdditionalInfo().getReserveField2(),
									request.getAdditionalInfo().getReserveField3(),
									request.getAdditionalInfo().getReserveField4(),
									request.getAdditionalInfo().getReserveField5(),
									request.getAdditionalInfo().getReserveField6(),
									request.getAdditionalInfo().getReserveField7(),
									request.getAdditionalInfo().getReserveField8(),
									request.getAdditionalInfo().getReserveField9(),
									request.getAdditionalInfo().getReserveField10());

							response = new BillPaymentResponse(infoPay, txnInfoPay, additionalInfoPay);
							transactionStatus = Constants.Status.Success;
						} else if (billStatus.equalsIgnoreCase(Constants.BILL_STATUS.BILL_BLOCK)) {
							infoPay = new InfoPay(Constants.ResponseCodes.CONSUMER_NUMBER_BLOCK,
									Constants.ResponseDescription.CONSUMER_NUMBER_BLOCK, rrn, stan);
							txnInfoPay = new TxnInfoPay(request.getTxnInfo().getBillerId(),
									request.getTxnInfo().getBillNumber(), paymentRefrence);// paymentRefrence
							additionalInfoPay = new AdditionalInfoPay(request.getAdditionalInfo().getReserveField1(),
									request.getAdditionalInfo().getReserveField2(),
									request.getAdditionalInfo().getReserveField3(),
									request.getAdditionalInfo().getReserveField4(),
									request.getAdditionalInfo().getReserveField5(),
									request.getAdditionalInfo().getReserveField6(),
									request.getAdditionalInfo().getReserveField7(),
									request.getAdditionalInfo().getReserveField8(),
									request.getAdditionalInfo().getReserveField9(),
									request.getAdditionalInfo().getReserveField10());
							response = new BillPaymentResponse(infoPay, txnInfoPay, additionalInfoPay);
							transactionStatus = Constants.Status.Fail;
						}
					} else if (getVoucherResponse.getResponse().getResponseCode()
							.equals(Constants.ResponseCodes.NOT_FOUND)) {
						infoPay = new InfoPay(Constants.ResponseCodes.CONSUMER_NUMBER_NOT_EXISTS,
								Constants.ResponseDescription.CONSUMER_NUMBER_NOT_EXISTS, rrn, stan);
						txnInfoPay = new TxnInfoPay(request.getTxnInfo().getBillerId(),
								request.getTxnInfo().getBillNumber(), paymentRefrence);// paymentRefrence
						additionalInfoPay = new AdditionalInfoPay(request.getAdditionalInfo().getReserveField1(),
								request.getAdditionalInfo().getReserveField2(),
								request.getAdditionalInfo().getReserveField3(),
								request.getAdditionalInfo().getReserveField4(),
								request.getAdditionalInfo().getReserveField5(),
								request.getAdditionalInfo().getReserveField6(),
								request.getAdditionalInfo().getReserveField7(),
								request.getAdditionalInfo().getReserveField8(),
								request.getAdditionalInfo().getReserveField9(),
								request.getAdditionalInfo().getReserveField10());
						response = new BillPaymentResponse(infoPay, txnInfoPay, additionalInfoPay);
						transactionStatus = Constants.Status.Fail;

					} else {
						infoPay = new InfoPay(Constants.ResponseCodes.SERVICE_FAIL,
								Constants.ResponseDescription.SERVICE_FAIL, rrn, stan);
						txnInfoPay = new TxnInfoPay(request.getTxnInfo().getBillerId(),
								request.getTxnInfo().getBillNumber(), paymentRefrence);// paymentRefrence
						additionalInfoPay = new AdditionalInfoPay(request.getAdditionalInfo().getReserveField1(),
								request.getAdditionalInfo().getReserveField2(),
								request.getAdditionalInfo().getReserveField3(),
								request.getAdditionalInfo().getReserveField4(),
								request.getAdditionalInfo().getReserveField5(),
								request.getAdditionalInfo().getReserveField6(),
								request.getAdditionalInfo().getReserveField7(),
								request.getAdditionalInfo().getReserveField8(),
								request.getAdditionalInfo().getReserveField9(),
								request.getAdditionalInfo().getReserveField10());
						response = new BillPaymentResponse(infoPay, txnInfoPay, additionalInfoPay);
						transactionStatus = Constants.Status.Fail;
					}

				} else {
					infoPay = new InfoPay(Constants.ResponseCodes.BAD_TRANSACTION,
							Constants.ResponseDescription.BAD_TRANSACTION, rrn, stan);
					txnInfoPay = new TxnInfoPay(request.getTxnInfo().getBillerId(),
							request.getTxnInfo().getBillNumber(), paymentRefrence);// paymentRefrence
					additionalInfoPay = new AdditionalInfoPay(request.getAdditionalInfo().getReserveField1(),
							request.getAdditionalInfo().getReserveField2(),
							request.getAdditionalInfo().getReserveField3(),
							request.getAdditionalInfo().getReserveField4(),
							request.getAdditionalInfo().getReserveField5(),
							request.getAdditionalInfo().getReserveField6(),
							request.getAdditionalInfo().getReserveField7(),
							request.getAdditionalInfo().getReserveField8(),
							request.getAdditionalInfo().getReserveField9(),
							request.getAdditionalInfo().getReserveField10());
					response = new BillPaymentResponse(infoPay, txnInfoPay, additionalInfoPay);

					transactionStatus = Constants.Status.Fail;
					LOG.info("Calling Bill payment End");
				}
			} else {
				infoPay = new InfoPay(Constants.ResponseCodes.SERVICE_FAIL, Constants.ResponseDescription.SERVICE_FAIL,
						rrn, stan);
				response = new BillPaymentResponse(infoPay, null, null);

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
						request.getInfo().getRrn(), request.getTxnInfo().getBillerId(),
						request.getTxnInfo().getBillNumber(), channel, username);

			} catch (Exception ex) {
				LOG.error("{}", ex);
			}
			try {

				paymentLoggingService.paymentLog(responseDate, responseDate, rrn, stan,
						response.getInfo().getResponseCode(), response.getInfo().getResponseDesc(), cnic,
						request.getTerminalInfo().getMobile(), depostiroName, request.getTxnInfo().getBillNumber(),
						request.getTxnInfo().getBillerId(), requestTotalAmountbdUp, dbTransactionFees,
						Constants.ACTIVITY.BillPayment, paymentRefrence, request.getTxnInfo().getBillNumber(),
						transactionStatus, address, transactionFees, dbTax, dbTotal, channel, billStatus,
						request.getTxnInfo().getTranDate(), request.getTxnInfo().getTranTime(), province, transAuthId);

			} catch (Exception ex) {
				LOG.error("{}", ex);
			}
		}

		UtilMethods.generalLog("OUT -  Bill Payment Response {}" + response, LOG);

		return response;

	}

	@Override
	public BillPaymentResponse billPaymentFbr(BillPaymentRequest request, HttpServletRequest httpRequestData) {

		LOG.info("Inside method billPaymentFbr");
		BillPaymentResponse response = null;
		Date responseDate = new Date();
		FbrUpdateVoucherResponse updateVoucherResponse = null;
		Date strDate = new Date();

		FbrGetVoucherResponse fbrGetVoucherResponse = null;
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
		BigDecimal amountInDueToDate = null; // dbAmount = 0;
		double dbTax = 0;
		double dbTransactionFees = 0;
		double dbTotal = 0;
		String province = "";
		String reserved = "";
		String rrn = request.getInfo().getRrn();
		LOG.info("RRN :{ }", rrn);
		String stan = request.getInfo().getStan();
		String transAuthId = request.getTxnInfo().getTranAuthId();
		String paymentRefrence = "";
		BigDecimal inquiryTotalAmountbdUp = null;
		String dbBillStatus = "";
		BigDecimal requestAmount = null;
		String dueDateStr = "";
		BigDecimal requestAmountDb = null;

		String channel = "";
		String username = "";

		try {

			String[] result = jwtTokenUtil.getTokenInformation(httpRequestData);
			username = result[0];
			channel = result[1];

			ArrayList<String> inquiryParams = new ArrayList<String>();
			inquiryParams.add(Constants.MPAY_REQUEST_METHODS.PRAL_FBR_BILL_INQUIRY);
			inquiryParams.add("");// Identification_Type
			inquiryParams.add(request.getTxnInfo().getBillNumber().trim());
			inquiryParams.add("NBP");// Bank_Mnemonic
			inquiryParams.add(request.getAdditionalInfo().getReserveField1());// Bank_Mnemonic
			inquiryParams.add(rrn);
			inquiryParams.add(stan);

			fbrGetVoucherResponse = serviceCaller.get(inquiryParams, FbrGetVoucherResponse.class, rrn,
					Constants.ACTIVITY.BillInquiry);

			if (fbrGetVoucherResponse != null) {
				if (fbrGetVoucherResponse.getResponse().getResponseCode().equals(ResponseCodes.OK)) {
					// Setting Values for Db entry

					BigDecimal amountAfterDueDate = null;
					String billstatus = "";
					requestAmountDb = new BigDecimal(request.getTxnInfo().getTranAmount());

					BigDecimal requestAmountafterduedate = null;
					if (fbrGetVoucherResponse.getResponse().getPralFbrGetVoucher() != null) {

						String amountStr = fbrGetVoucherResponse.getResponse().getPralFbrGetVoucher()
								.getAmountWithinDueDate();
						String amountAfterDueDateStr = fbrGetVoucherResponse.getResponse().getPralFbrGetVoucher()
								.getAmountAfterDueDate();

						if (!amountStr.isEmpty()) {
							requestAmount = new BigDecimal(amountStr.replaceFirst("^\\+?0+", ""));
							requestAmount = requestAmount.divide(BigDecimal.valueOf(100));

							// Set scale to 2 and round up
							amountInDueToDate = requestAmount.setScale(2, RoundingMode.UP);

						}

						if (!amountAfterDueDateStr.isEmpty()) {
							requestAmountafterduedate = new BigDecimal(
									amountAfterDueDateStr.replaceFirst("^\\+?0+", ""));

							requestAmountafterduedate = requestAmountafterduedate.divide(BigDecimal.valueOf(100));

							// Set scale to 2 and round up
							amountAfterDueDate = requestAmountafterduedate.setScale(2, RoundingMode.UP);
						}

						//
						name = fbrGetVoucherResponse.getResponse().getPralFbrGetVoucher().getConsumerDetail();
						dueDateStr = fbrGetVoucherResponse.getResponse().getPralFbrGetVoucher().getDueDate();
						reserved = fbrGetVoucherResponse.getResponse().getPralFbrGetVoucher().getReserved();
						if (reserved == null || reserved.isBlank() || reserved.isEmpty()) {
							reserved = request.getAdditionalInfo().getReserveField1();
						}
						billStatus = fbrGetVoucherResponse.getResponse().getPralFbrGetVoucher().getBillStatus().trim()
								.equalsIgnoreCase("U") ? Constants.BILL_STATUS.BILL_UNPAID
										: Constants.BILL_STATUS.BILL_PAID;
						dbBillStatus = billStatus;

						BigDecimal txnAmount = new BigDecimal(request.getTxnInfo().getTranAmount());
						if (utilMethods.isValidInput(dueDateStr)) {
							LocalDate currentDate = LocalDate.now();

							try {
								LocalDate dueDate = utilMethods.parseDueDateWithoutDashes(dueDateStr);

								// Check due date conditions
								if (utilMethods.isPaymentWithinDueDate(currentDate, dueDate)) {
									if (txnAmount.compareTo(amountInDueToDate) != 0) {
										infoPay = new InfoPay(Constants.ResponseCodes.AMMOUNT_MISMATCH,
												Constants.ResponseDescription.AMMOUNT_MISMATCH, rrn, stan);
										response = new BillPaymentResponse(infoPay, null, null);
										return response;
									}
								} else {
									if (txnAmount.compareTo(amountAfterDueDate) != 0) {
										infoPay = new InfoPay(Constants.ResponseCodes.AMMOUNT_MISMATCH,
												Constants.ResponseDescription.AMMOUNT_MISMATCH, rrn, stan);
										response = new BillPaymentResponse(infoPay, null, null);
										return response;
									}
								}
							} catch (DateTimeParseException e) {
								LOG.error("Error parsing due date: " + e.getMessage());
							}
						} else {
							LOG.info("Invalid due date input or empty from pral fbr");
							if (txnAmount.compareTo(amountInDueToDate) != 0) {
								infoPay = new InfoPay(Constants.ResponseCodes.AMMOUNT_MISMATCH,
										Constants.ResponseDescription.AMMOUNT_MISMATCH, rrn, stan);
								response = new BillPaymentResponse(infoPay, null, null);
								return response;
							}
						}

						if (billStatus.equalsIgnoreCase(Constants.BILL_STATUS.BILL_UNPAID)) {

							try {

								String tranDate = request.getTxnInfo().getTranDate();

								if (tranDate.length() == 8) {
									tranDate = utilMethods.transactionDateFormater(tranDate);
								}
								LOG.info("Calling UpdateVoucher for Fbr");

								ArrayList<String> ubpsBillParams = new ArrayList<String>();
								ubpsBillParams.add(Constants.MPAY_REQUEST_METHODS.PRAL_FBR_BILL_PAYMENT);
								ubpsBillParams.add(""); // Identification_Type
								ubpsBillParams.add(request.getTxnInfo().getBillNumber().trim());
								ubpsBillParams.add("NBP"); // Bank_Mnemonic
								ubpsBillParams.add(request.getAdditionalInfo().getReserveField1());
								ubpsBillParams.add(request.getTxnInfo().getTranAuthId());
								ubpsBillParams.add(request.getTxnInfo().getTranDate());
								ubpsBillParams.add(request.getTxnInfo().getTranTime());
								ubpsBillParams.add(utilMethods
										.convertAmountToISOFormat(request.getTxnInfo().getTranAmount().trim()));
								ubpsBillParams.add(rrn);
								ubpsBillParams.add(stan);

								updateVoucherResponse = serviceCaller.get(ubpsBillParams,
										FbrUpdateVoucherResponse.class, rrn, Constants.ACTIVITY.BillPayment);

								if (updateVoucherResponse != null) {
									infoPay = new InfoPay(updateVoucherResponse.getResponse().getResponse_code(),
											updateVoucherResponse.getResponse().getResponse_desc(), rrn, stan);
									if (updateVoucherResponse.getResponse().getResponse_code()
											.equals(ResponseCodes.OK)) {
										paymentRefrence = utilMethods.getRRN();
										txnInfoPay = new TxnInfoPay(request.getTxnInfo().getBillerId(),
												request.getTxnInfo().getBillNumber(), paymentRefrence);

										additionalInfoPay = new AdditionalInfoPay(
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

										response = new BillPaymentResponse(infoPay, txnInfoPay, additionalInfoPay);
										transactionStatus = Constants.Status.Success;
										billStatus = Constants.BILL_STATUS.BILL_PAID;

									} else if (updateVoucherResponse.getResponse().getResponse_code().equals("402")) {
										infoPay = new InfoPay(Constants.ResponseCodes.DUPLICATE_TRANSACTION,
												Constants.ResponseDescription.DUPLICATE_TRANSACTION, rrn, stan);
										txnInfoPay = new TxnInfoPay(request.getTxnInfo().getBillerId(),
												request.getTxnInfo().getBillNumber(), paymentRefrence);// paymentRefrence
										additionalInfoPay = new AdditionalInfoPay(
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
										response = new BillPaymentResponse(infoPay, txnInfoPay, additionalInfoPay);
										transactionStatus = Constants.Status.Fail;
									} else {
										infoPay = new InfoPay(Constants.ResponseCodes.SERVICE_FAIL,
												Constants.ResponseDescription.SERVICE_FAIL, rrn, stan);
										txnInfoPay = new TxnInfoPay(request.getTxnInfo().getBillerId(),
												request.getTxnInfo().getBillNumber(), paymentRefrence);// paymentRefrence
										additionalInfoPay = new AdditionalInfoPay(
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
										response = new BillPaymentResponse(infoPay, txnInfoPay, additionalInfoPay);
									}

								} else { // Second time bill inquiry to pe call from here and if bill is paid then
											// return
											// success

									fbrGetVoucherResponse = serviceCaller.get(inquiryParams,
											FbrGetVoucherResponse.class, rrn, Constants.ACTIVITY.BillInquiry);
									if (fbrGetVoucherResponse != null) {
										if (fbrGetVoucherResponse.getResponse().getResponseCode()
												.equals(ResponseCodes.OK)) {
											if (fbrGetVoucherResponse.getResponse().getPralFbrGetVoucher()
													.getBillStatus().equalsIgnoreCase(Constants.BILL_STATUS.BILL_PAID)
													|| fbrGetVoucherResponse.getResponse().getPralFbrGetVoucher()
															.getBillStatus().equalsIgnoreCase(
																	Constants.BILL_STATUS.BILL_PAID.substring(0))) {
												paymentRefrence = utilMethods.getRRN();
												txnInfoPay = new TxnInfoPay(request.getTxnInfo().getBillerId(),
														request.getTxnInfo().getBillNumber(), paymentRefrence);// paymentRefrence
												additionalInfoPay = new AdditionalInfoPay(reserved,
														request.getAdditionalInfo().getReserveField2(),
														request.getAdditionalInfo().getReserveField3(),
														request.getAdditionalInfo().getReserveField4(),
														request.getAdditionalInfo().getReserveField5(),
														request.getAdditionalInfo().getReserveField6(),
														request.getAdditionalInfo().getReserveField7(),
														request.getAdditionalInfo().getReserveField8(),
														request.getAdditionalInfo().getReserveField9(),
														request.getAdditionalInfo().getReserveField10());

												infoPay = new InfoPay(
														fbrGetVoucherResponse.getResponse().getResponseCode(),
														fbrGetVoucherResponse.getResponse().getResponseDesc(), rrn,
														stan);
												response = new BillPaymentResponse(infoPay, txnInfoPay,
														additionalInfoPay);
												transactionStatus = Constants.Status.Success;
												billStatus = Constants.BILL_STATUS.BILL_PAID;
											} else {
												infoPay = new InfoPay(Constants.ResponseCodes.SERVICE_FAIL,
														Constants.ResponseDescription.SERVICE_FAIL, rrn, stan);
												txnInfoPay = new TxnInfoPay(request.getTxnInfo().getBillerId(),
														request.getTxnInfo().getBillNumber(), paymentRefrence);// paymentRefrence
												additionalInfoPay = new AdditionalInfoPay(
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
												response = new BillPaymentResponse(infoPay, txnInfoPay,
														additionalInfoPay);
											}
										} else {
											infoPay = new InfoPay(Constants.ResponseCodes.SERVICE_FAIL,
													Constants.ResponseDescription.SERVICE_FAIL, rrn, stan);
											txnInfoPay = new TxnInfoPay(request.getTxnInfo().getBillerId(),
													request.getTxnInfo().getBillNumber(), paymentRefrence);// paymentRefrence
											additionalInfoPay = new AdditionalInfoPay(
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
											response = new BillPaymentResponse(infoPay, txnInfoPay, additionalInfoPay);
										}
									} else {
										infoPay = new InfoPay(Constants.ResponseCodes.SERVICE_FAIL,
												Constants.ResponseDescription.SERVICE_FAIL, rrn, stan);
										txnInfoPay = new TxnInfoPay(request.getTxnInfo().getBillerId(),
												request.getTxnInfo().getBillNumber(), paymentRefrence);// paymentRefrence
										additionalInfoPay = new AdditionalInfoPay(
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
										response = new BillPaymentResponse(infoPay, txnInfoPay, additionalInfoPay);
										transactionStatus = Constants.Status.Fail;
									}

								}

							} catch (Exception ex) {
								LOG.error("{}", ex);
							}

						} else if (billStatus.equalsIgnoreCase(Constants.BILL_STATUS.BILL_PAID)) {
							infoPay = new InfoPay(Constants.ResponseCodes.BILL_ALREADY_PAID,
									Constants.ResponseDescription.BILL_ALREADY_PAID, rrn, stan);
							txnInfoPay = new TxnInfoPay(request.getTxnInfo().getBillerId(),
									request.getTxnInfo().getBillNumber(), paymentRefrence);// paymentRefrence
							additionalInfoPay = new AdditionalInfoPay(request.getAdditionalInfo().getReserveField1(),
									request.getAdditionalInfo().getReserveField2(),
									request.getAdditionalInfo().getReserveField3(),
									request.getAdditionalInfo().getReserveField4(),
									request.getAdditionalInfo().getReserveField5(),
									request.getAdditionalInfo().getReserveField6(),
									request.getAdditionalInfo().getReserveField7(),
									request.getAdditionalInfo().getReserveField8(),
									request.getAdditionalInfo().getReserveField9(),
									request.getAdditionalInfo().getReserveField10());

							response = new BillPaymentResponse(infoPay, txnInfoPay, additionalInfoPay);
							transactionStatus = Constants.Status.Success;
						} else if (billStatus.equalsIgnoreCase(Constants.BILL_STATUS.BILL_BLOCK)) {
							infoPay = new InfoPay(Constants.ResponseCodes.CONSUMER_NUMBER_BLOCK,
									Constants.ResponseDescription.CONSUMER_NUMBER_BLOCK, rrn, stan);
							txnInfoPay = new TxnInfoPay(request.getTxnInfo().getBillerId(),
									request.getTxnInfo().getBillNumber(), paymentRefrence);// paymentRefrence
							additionalInfoPay = new AdditionalInfoPay(request.getAdditionalInfo().getReserveField1(),
									request.getAdditionalInfo().getReserveField2(),
									request.getAdditionalInfo().getReserveField3(),
									request.getAdditionalInfo().getReserveField4(),
									request.getAdditionalInfo().getReserveField5(),
									request.getAdditionalInfo().getReserveField6(),
									request.getAdditionalInfo().getReserveField7(),
									request.getAdditionalInfo().getReserveField8(),
									request.getAdditionalInfo().getReserveField9(),
									request.getAdditionalInfo().getReserveField10());
							response = new BillPaymentResponse(infoPay, txnInfoPay, additionalInfoPay);
							transactionStatus = Constants.Status.Fail;
						}
					} else if (fbrGetVoucherResponse.getResponse().getResponseCode()
							.equals(Constants.ResponseCodes.NOT_FOUND)) {
						infoPay = new InfoPay(Constants.ResponseCodes.CONSUMER_NUMBER_NOT_EXISTS,
								Constants.ResponseDescription.CONSUMER_NUMBER_NOT_EXISTS, rrn, stan);
						txnInfoPay = new TxnInfoPay(request.getTxnInfo().getBillerId(),
								request.getTxnInfo().getBillNumber(), paymentRefrence);// paymentRefrence
						additionalInfoPay = new AdditionalInfoPay(request.getAdditionalInfo().getReserveField1(),
								request.getAdditionalInfo().getReserveField2(),
								request.getAdditionalInfo().getReserveField3(),
								request.getAdditionalInfo().getReserveField4(),
								request.getAdditionalInfo().getReserveField5(),
								request.getAdditionalInfo().getReserveField6(),
								request.getAdditionalInfo().getReserveField7(),
								request.getAdditionalInfo().getReserveField8(),
								request.getAdditionalInfo().getReserveField9(),
								request.getAdditionalInfo().getReserveField10());
						response = new BillPaymentResponse(infoPay, txnInfoPay, additionalInfoPay);
						transactionStatus = Constants.Status.Fail;

					} else if (fbrGetVoucherResponse.getResponse().getResponseCode()
							.equals(Constants.ResponseCodes.INVALID_DATA)) {
						infoPay = new InfoPay(Constants.ResponseCodes.INVALID_DATA,
								Constants.ResponseDescription.INVALID_DATA, rrn, stan);
						txnInfoPay = new TxnInfoPay(request.getTxnInfo().getBillerId(),
								request.getTxnInfo().getBillNumber(), paymentRefrence);// paymentRefrence
						additionalInfoPay = new AdditionalInfoPay(request.getAdditionalInfo().getReserveField1(),
								request.getAdditionalInfo().getReserveField2(),
								request.getAdditionalInfo().getReserveField3(),
								request.getAdditionalInfo().getReserveField4(),
								request.getAdditionalInfo().getReserveField5(),
								request.getAdditionalInfo().getReserveField6(),
								request.getAdditionalInfo().getReserveField7(),
								request.getAdditionalInfo().getReserveField8(),
								request.getAdditionalInfo().getReserveField9(),
								request.getAdditionalInfo().getReserveField10());
						response = new BillPaymentResponse(infoPay, txnInfoPay, additionalInfoPay);
						transactionStatus = Constants.Status.Fail;
					} else {
						infoPay = new InfoPay(Constants.ResponseCodes.SERVICE_FAIL,
								Constants.ResponseDescription.SERVICE_FAIL, rrn, stan);
						txnInfoPay = new TxnInfoPay(request.getTxnInfo().getBillerId(),
								request.getTxnInfo().getBillNumber(), paymentRefrence);// paymentRefrence
						additionalInfoPay = new AdditionalInfoPay(request.getAdditionalInfo().getReserveField1(),
								request.getAdditionalInfo().getReserveField2(),
								request.getAdditionalInfo().getReserveField3(),
								request.getAdditionalInfo().getReserveField4(),
								request.getAdditionalInfo().getReserveField5(),
								request.getAdditionalInfo().getReserveField6(),
								request.getAdditionalInfo().getReserveField7(),
								request.getAdditionalInfo().getReserveField8(),
								request.getAdditionalInfo().getReserveField9(),
								request.getAdditionalInfo().getReserveField10());
						response = new BillPaymentResponse(infoPay, txnInfoPay, additionalInfoPay);
						transactionStatus = Constants.Status.Fail;
					}

				}

				else if (fbrGetVoucherResponse.getResponse().getResponseCode()
						.equals(Constants.ResponseCodes.NOT_FOUND)) {
					infoPay = new InfoPay(Constants.ResponseCodes.CONSUMER_NUMBER_NOT_EXISTS,
							Constants.ResponseDescription.CONSUMER_NUMBER_NOT_EXISTS, rrn, stan);
					txnInfoPay = new TxnInfoPay(request.getTxnInfo().getBillerId(),
							request.getTxnInfo().getBillNumber(), paymentRefrence);// paymentRefrence
					additionalInfoPay = new AdditionalInfoPay(request.getAdditionalInfo().getReserveField1(),
							request.getAdditionalInfo().getReserveField2(),
							request.getAdditionalInfo().getReserveField3(),
							request.getAdditionalInfo().getReserveField4(),
							request.getAdditionalInfo().getReserveField5(),
							request.getAdditionalInfo().getReserveField6(),
							request.getAdditionalInfo().getReserveField7(),
							request.getAdditionalInfo().getReserveField8(),
							request.getAdditionalInfo().getReserveField9(),
							request.getAdditionalInfo().getReserveField10());
					response = new BillPaymentResponse(infoPay, txnInfoPay, additionalInfoPay);

					transactionStatus = Constants.Status.Fail;
				}

				else if (fbrGetVoucherResponse.getResponse().getResponseCode()
						.equals(Constants.ResponseCodes.CONSUMER_NUMBER_NOT_EXISTS)) {
					infoPay = new InfoPay(Constants.ResponseCodes.CONSUMER_NUMBER_NOT_EXISTS,
							Constants.ResponseDescription.CONSUMER_NUMBER_NOT_EXISTS, rrn, stan);
					txnInfoPay = new TxnInfoPay(request.getTxnInfo().getBillerId(),
							request.getTxnInfo().getBillNumber(), paymentRefrence);// paymentRefrence
					additionalInfoPay = new AdditionalInfoPay(request.getAdditionalInfo().getReserveField1(),
							request.getAdditionalInfo().getReserveField2(),
							request.getAdditionalInfo().getReserveField3(),
							request.getAdditionalInfo().getReserveField4(),
							request.getAdditionalInfo().getReserveField5(),
							request.getAdditionalInfo().getReserveField6(),
							request.getAdditionalInfo().getReserveField7(),
							request.getAdditionalInfo().getReserveField8(),
							request.getAdditionalInfo().getReserveField9(),
							request.getAdditionalInfo().getReserveField10());
					response = new BillPaymentResponse(infoPay, txnInfoPay, additionalInfoPay);
					transactionStatus = Constants.Status.Fail;
				}

				else {
					infoPay = new InfoPay(Constants.ResponseCodes.BAD_TRANSACTION,
							Constants.ResponseDescription.BAD_TRANSACTION, rrn, stan);
					txnInfoPay = new TxnInfoPay(request.getTxnInfo().getBillerId(),
							request.getTxnInfo().getBillNumber(), paymentRefrence);// paymentRefrence
					additionalInfoPay = new AdditionalInfoPay(request.getAdditionalInfo().getReserveField1(),
							request.getAdditionalInfo().getReserveField2(),
							request.getAdditionalInfo().getReserveField3(),
							request.getAdditionalInfo().getReserveField4(),
							request.getAdditionalInfo().getReserveField5(),
							request.getAdditionalInfo().getReserveField6(),
							request.getAdditionalInfo().getReserveField7(),
							request.getAdditionalInfo().getReserveField8(),
							request.getAdditionalInfo().getReserveField9(),
							request.getAdditionalInfo().getReserveField10());
					response = new BillPaymentResponse(infoPay, txnInfoPay, additionalInfoPay);

					transactionStatus = Constants.Status.Fail;

				}

			} else {
				infoPay = new InfoPay(Constants.ResponseCodes.SERVICE_FAIL, Constants.ResponseDescription.SERVICE_FAIL,
						rrn, stan);
				response = new BillPaymentResponse(infoPay, null, null);

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
						request.getInfo().getRrn(), request.getTxnInfo().getBillerId(),
						request.getTxnInfo().getBillNumber(), channel, username);

			} catch (Exception ex) {
				LOG.error("{}", ex);
			}
			try {

				paymentLoggingService.paymentLog(responseDate, responseDate, rrn, stan,
						response.getInfo().getResponseCode(), response.getInfo().getResponseDesc(), cnic,
						request.getTerminalInfo().getMobile(), name, request.getTxnInfo().getBillNumber(),
						request.getTxnInfo().getBillerId(), requestAmountDb, dbTransactionFees,
						Constants.ACTIVITY.BillPayment, paymentRefrence, request.getTxnInfo().getBillNumber(),
						transactionStatus, address, transactionFees, dbTax, dbTotal, channel, dbBillStatus,
						request.getTxnInfo().getTranDate(), request.getTxnInfo().getTranTime(), province, transAuthId);

			} catch (Exception ex) {
				LOG.error("{}", ex);
			}
		}

		UtilMethods.generalLog("OUT -  Bill Payment Response {}" + response, LOG);

		return response;

	}

	@Override
	public BillPaymentResponse billPaymentAiou(BillPaymentRequest request, HttpServletRequest httpRequestData) {

		LOG.info("Inside method billPaymentAiou: {}");

		BillPaymentResponse response = null;
		Date responseDate = new Date();
		AiouUpdateVoucherResponse updateVoucherResponse = null;
		Date strDate = new Date();

		AiouGetVoucherResponse aiouGetVoucherResponse = null;
		InfoPay infoPay = null;
		TxnInfoPay txnInfoPay = null;
		AdditionalInfoPay additionalInfoPay = null;
		Info info = null;
		String transactionStatus = "";
		double transactionFees = 0;
		String cnic = "";
		String mobile = "";
		String address = "";
		String name = "";
		String billStatus = "";
		double amountInDueToDate = 0; // dbAmount = 0;
		double dbTax = 0;
		double dbTransactionFees = 0;
		double dbTotal = 0;
		String province = "";
		String reserved = "";
		String rrn = request.getInfo().getRrn();
		LOG.info("RRN :{ }", rrn);
		String stan = request.getInfo().getStan();
		String transAuthId = request.getTxnInfo().getTranAuthId();
		String paymentRefrence = "";
		BigDecimal inquiryTotalAmountbdUp = null;
		String dbBillStatus = "";
		BigDecimal requestAmount = null;
		String dueDateStr = "";
//		String dueDate = "";

		String channel = "";
		String username = "";

		try {

			String[] result = jwtTokenUtil.getTokenInformation(httpRequestData);
			username = result[0];
			channel = result[1];

			ArrayList<String> inquiryParams = new ArrayList<String>();
			inquiryParams.add(Constants.MPAY_REQUEST_METHODS.AIOU_BILL_INQUIRY);
			inquiryParams.add(Constants.BankMnemonic.ABL);
			inquiryParams.add(request.getTxnInfo().getBillNumber().trim());
			inquiryParams.add(rrn);
			inquiryParams.add(stan);

			// Bank_Mnemonic,ChallanNumber,Semester,Name,FatherName,
			// CNIC,Programme,RollNumber,RegistrationNumber,ContactNumber,
			// PaymentDate,AmountPaid,BankName,BankCode,BranchName,BranchCode,rrn,stan

			// Bank_Mnemonic,ChallanNumber,ContactNumber,PaymentDate,AmountPaid,Semester,Programme,BankName,BankCode,BranchName,BranchCode,rrn,stan

			aiouGetVoucherResponse = serviceCaller.get(inquiryParams, AiouGetVoucherResponse.class, rrn,
					Constants.ACTIVITY.BillInquiry);

			if (aiouGetVoucherResponse != null) {
				info = new Info(aiouGetVoucherResponse.getResponse().getResponseCode(),
						aiouGetVoucherResponse.getResponse().getResponseDesc(), rrn, stan);
				if (aiouGetVoucherResponse.getResponse().getResponseCode().equals(ResponseCodes.OK)) {

					double amountAfterDueDate = 0;
					String billstatus = "";

					BigDecimal requestAmountafterduedate = null;
					if (aiouGetVoucherResponse.getResponse().getAiouGetVoucher() != null) {

						String amountStr = aiouGetVoucherResponse.getResponse().getAiouGetVoucher()
								.getResponseBillInquiry().getAmountWithinDueDate();
						String amountAfterDueDateStr = aiouGetVoucherResponse.getResponse().getAiouGetVoucher()
								.getResponseBillInquiry().getAmountAfterDueDate();

						if (!amountStr.isEmpty()) {
							requestAmount = BigDecimal.valueOf(Double.parseDouble(amountStr)).setScale(2,
									RoundingMode.UP);
							amountInDueToDate = utilMethods.bigDecimalToDouble(requestAmount);
							// amountPaidInDueDate = utilMethods.formatAmount(requestAmount, 12);

							// dbAmount = requestAmount.doubleValue();
						}

						if (!amountAfterDueDateStr.isEmpty()) {
							requestAmountafterduedate = BigDecimal.valueOf(Double.parseDouble(amountAfterDueDateStr))
									.setScale(2, RoundingMode.UP);
							amountAfterDueDate = utilMethods.bigDecimalToDouble(requestAmountafterduedate);
							// amountPaidAfterDueDate = utilMethods.formatAmount(requestAmountafterduedate,
							// 12);

						}

						name = aiouGetVoucherResponse.getResponse().getAiouGetVoucher().getResponseBillInquiry()
								.getName();
						dueDateStr = aiouGetVoucherResponse.getResponse().getAiouGetVoucher().getResponseBillInquiry()
								.getDueDate();

						billStatus = aiouGetVoucherResponse.getResponse().getAiouGetVoucher().getResponseBillInquiry()
								.getBillStatus().trim().equalsIgnoreCase("U") ? Constants.BILL_STATUS.BILL_UNPAID
										: Constants.BILL_STATUS.BILL_PAID;
						// dbBillStatus = billStatus;

						if (utilMethods.isValidInput(dueDateStr)) {
							LocalDate currentDate = LocalDate.now();

							try {
								LocalDate dueDate = utilMethods.parseDueDateWithoutDashes(dueDateStr);

								// Check due date conditions
								if (utilMethods.isPaymentWithinDueDate(currentDate, dueDate)) {
									if (Double.valueOf(request.getTxnInfo().getTranAmount())
											.compareTo(amountInDueToDate) != 0) {
										infoPay = new InfoPay(Constants.ResponseCodes.AMMOUNT_MISMATCH,
												Constants.ResponseDescription.AMMOUNT_MISMATCH, rrn, stan);
										response = new BillPaymentResponse(infoPay, null, null);
										return response;
									}
								} else {
									if (Double.valueOf(request.getTxnInfo().getTranAmount())
											.compareTo(amountAfterDueDate) != 0) {
										infoPay = new InfoPay(Constants.ResponseCodes.AMMOUNT_MISMATCH,
												Constants.ResponseDescription.AMMOUNT_MISMATCH, rrn, stan);
										response = new BillPaymentResponse(infoPay, null, null);
										return response;
									}
								}
							} catch (DateTimeParseException e) {
								LOG.error("Error parsing due date: " + e.getMessage());
							}
						} else {
							LOG.info("Invalid due date input");
							if (Double.valueOf(request.getTxnInfo().getTranAmount())
									.compareTo(amountInDueToDate) != 0) {
								infoPay = new InfoPay(Constants.ResponseCodes.AMMOUNT_MISMATCH,
										Constants.ResponseDescription.AMMOUNT_MISMATCH, rrn, stan);
								response = new BillPaymentResponse(infoPay, null, null);
								return response;
							}
						}

						if (billStatus.equalsIgnoreCase(Constants.BILL_STATUS.BILL_UNPAID)) {

							try {

								String tranDate = request.getTxnInfo().getTranDate();

								if (tranDate.length() == 8) {
									tranDate = utilMethods.transactionDateFormater(tranDate);
								}
								LOG.info("Calling UpdateVoucher for Aiou");

								ArrayList<String> ubpsBillParams = new ArrayList<String>();
								ubpsBillParams.add(Constants.MPAY_REQUEST_METHODS.AIOU_BILL_PAYMENT);
								ubpsBillParams.add(Constants.BankMnemonic.ABL);// Bank_Mnemonic
								ubpsBillParams.add(request.getTxnInfo().getBillNumber().trim());
								ubpsBillParams.add(request.getTerminalInfo().getMobile().trim());
								ubpsBillParams.add(request.getTxnInfo().getTranDate().trim());
								ubpsBillParams.add(request.getTxnInfo().getTranAmount().trim());
								ubpsBillParams.add(request.getAdditionalInfo().getReserveField1());
								ubpsBillParams.add(request.getAdditionalInfo().getReserveField2());
								ubpsBillParams.add(request.getAdditionalInfo().getReserveField3());
								ubpsBillParams.add(request.getAdditionalInfo().getReserveField4());
								ubpsBillParams.add(request.getAdditionalInfo().getReserveField5());
								ubpsBillParams.add(request.getAdditionalInfo().getReserveField6());
								ubpsBillParams.add(rrn);
								ubpsBillParams.add(stan);

								updateVoucherResponse = serviceCaller.get(ubpsBillParams,
										AiouUpdateVoucherResponse.class, rrn, Constants.ACTIVITY.BillPayment);

								if (updateVoucherResponse != null) {
									infoPay = new InfoPay(updateVoucherResponse.getResponse().getResponse_code(),
											updateVoucherResponse.getResponse().getResponse_desc(), rrn, stan);
									if (updateVoucherResponse.getResponse().getResponse_code()
											.equals(ResponseCodes.OK)) {
										paymentRefrence = utilMethods.getRRN();
										txnInfoPay = new TxnInfoPay(request.getTxnInfo().getBillerId(),
												request.getTxnInfo().getBillNumber(), paymentRefrence);

										additionalInfoPay = new AdditionalInfoPay(
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

										response = new BillPaymentResponse(infoPay, txnInfoPay, additionalInfoPay);
										transactionStatus = Constants.Status.Success;
										billStatus = Constants.BILL_STATUS.BILL_PAID;

									} else if (updateVoucherResponse.getResponse().getResponse_code().equals("402")) {
										infoPay = new InfoPay(Constants.ResponseCodes.DUPLICATE_TRANSACTION,
												Constants.ResponseDescription.DUPLICATE_TRANSACTION, rrn, stan);
										txnInfoPay = new TxnInfoPay(request.getTxnInfo().getBillerId(),
												request.getTxnInfo().getBillNumber(), paymentRefrence);// paymentRefrence
										additionalInfoPay = new AdditionalInfoPay(
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
										response = new BillPaymentResponse(infoPay, txnInfoPay, additionalInfoPay);
										transactionStatus = Constants.Status.Fail;
									} else {
										infoPay = new InfoPay(Constants.ResponseCodes.SERVICE_FAIL,
												Constants.ResponseDescription.SERVICE_FAIL, rrn, stan);
										txnInfoPay = new TxnInfoPay(request.getTxnInfo().getBillerId(),
												request.getTxnInfo().getBillNumber(), paymentRefrence);// paymentRefrence
										additionalInfoPay = new AdditionalInfoPay(
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
										response = new BillPaymentResponse(infoPay, txnInfoPay, additionalInfoPay);
									}

								} else { // Second time bill inquiry to pe call from here and if bill is paid then
											// return
											// success

									aiouGetVoucherResponse = serviceCaller.get(inquiryParams,
											AiouGetVoucherResponse.class, rrn, Constants.ACTIVITY.BillInquiry);

									if (aiouGetVoucherResponse != null) {
										if (aiouGetVoucherResponse.getResponse().getResponseCode()
												.equals(ResponseCodes.OK)) {
											if (aiouGetVoucherResponse.getResponse().getAiouGetVoucher()
													.getResponseBillInquiry().getBillStatus()
													.equalsIgnoreCase(Constants.BILL_STATUS.BILL_PAID)
													|| aiouGetVoucherResponse.getResponse().getAiouGetVoucher()
															.getResponseBillInquiry().getBillStatus().equalsIgnoreCase(
																	Constants.BILL_STATUS.BILL_PAID.substring(0))) {
												paymentRefrence = utilMethods.getRRN();
												txnInfoPay = new TxnInfoPay(request.getTxnInfo().getBillerId(),
														request.getTxnInfo().getBillNumber(), paymentRefrence);// paymentRefrence
												additionalInfoPay = new AdditionalInfoPay(reserved,
														request.getAdditionalInfo().getReserveField2(),
														request.getAdditionalInfo().getReserveField3(),
														request.getAdditionalInfo().getReserveField4(),
														request.getAdditionalInfo().getReserveField5(),
														request.getAdditionalInfo().getReserveField6(),
														request.getAdditionalInfo().getReserveField7(),
														request.getAdditionalInfo().getReserveField8(),
														request.getAdditionalInfo().getReserveField9(),
														request.getAdditionalInfo().getReserveField10());

												infoPay = new InfoPay(
														aiouGetVoucherResponse.getResponse().getResponseCode(),
														aiouGetVoucherResponse.getResponse().getResponseDesc(), rrn,
														stan);
												response = new BillPaymentResponse(infoPay, txnInfoPay,
														additionalInfoPay);
												transactionStatus = Constants.Status.Success;
												billStatus = Constants.BILL_STATUS.BILL_PAID;
											} else {
												infoPay = new InfoPay(Constants.ResponseCodes.SERVICE_FAIL,
														Constants.ResponseDescription.SERVICE_FAIL, rrn, stan);
												txnInfoPay = new TxnInfoPay(request.getTxnInfo().getBillerId(),
														request.getTxnInfo().getBillNumber(), paymentRefrence);// paymentRefrence
												additionalInfoPay = new AdditionalInfoPay(
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
												response = new BillPaymentResponse(infoPay, txnInfoPay,
														additionalInfoPay);
											}
										} else {
											infoPay = new InfoPay(Constants.ResponseCodes.SERVICE_FAIL,
													Constants.ResponseDescription.SERVICE_FAIL, rrn, stan);
											txnInfoPay = new TxnInfoPay(request.getTxnInfo().getBillerId(),
													request.getTxnInfo().getBillNumber(), paymentRefrence);// paymentRefrence
											additionalInfoPay = new AdditionalInfoPay(
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
											response = new BillPaymentResponse(infoPay, txnInfoPay, additionalInfoPay);
										}
									} else {
										infoPay = new InfoPay(Constants.ResponseCodes.SERVICE_FAIL,
												Constants.ResponseDescription.SERVICE_FAIL, rrn, stan);
										txnInfoPay = new TxnInfoPay(request.getTxnInfo().getBillerId(),
												request.getTxnInfo().getBillNumber(), paymentRefrence);// paymentRefrence
										additionalInfoPay = new AdditionalInfoPay(
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
										response = new BillPaymentResponse(infoPay, txnInfoPay, additionalInfoPay);
										transactionStatus = Constants.Status.Fail;
									}

								}

							} catch (Exception ex) {
								LOG.error("{}", ex);
							}

						} else if (billStatus.equalsIgnoreCase(Constants.BILL_STATUS.BILL_PAID)) {
							infoPay = new InfoPay(Constants.ResponseCodes.BILL_ALREADY_PAID,
									Constants.ResponseDescription.BILL_ALREADY_PAID, rrn, stan);
							txnInfoPay = new TxnInfoPay(request.getTxnInfo().getBillerId(),
									request.getTxnInfo().getBillNumber(), paymentRefrence);// paymentRefrence
							additionalInfoPay = new AdditionalInfoPay(request.getAdditionalInfo().getReserveField1(),
									request.getAdditionalInfo().getReserveField2(),
									request.getAdditionalInfo().getReserveField3(),
									request.getAdditionalInfo().getReserveField4(),
									request.getAdditionalInfo().getReserveField5(),
									request.getAdditionalInfo().getReserveField6(),
									request.getAdditionalInfo().getReserveField7(),
									request.getAdditionalInfo().getReserveField8(),
									request.getAdditionalInfo().getReserveField9(),
									request.getAdditionalInfo().getReserveField10());

							response = new BillPaymentResponse(infoPay, txnInfoPay, additionalInfoPay);
							transactionStatus = Constants.Status.Success;
						} else if (billStatus.equalsIgnoreCase(Constants.BILL_STATUS.BILL_BLOCK)) {
							infoPay = new InfoPay(Constants.ResponseCodes.CONSUMER_NUMBER_BLOCK,
									Constants.ResponseDescription.CONSUMER_NUMBER_BLOCK, rrn, stan);
							txnInfoPay = new TxnInfoPay(request.getTxnInfo().getBillerId(),
									request.getTxnInfo().getBillNumber(), paymentRefrence);// paymentRefrence
							additionalInfoPay = new AdditionalInfoPay(request.getAdditionalInfo().getReserveField1(),
									request.getAdditionalInfo().getReserveField2(),
									request.getAdditionalInfo().getReserveField3(),
									request.getAdditionalInfo().getReserveField4(),
									request.getAdditionalInfo().getReserveField5(),
									request.getAdditionalInfo().getReserveField6(),
									request.getAdditionalInfo().getReserveField7(),
									request.getAdditionalInfo().getReserveField8(),
									request.getAdditionalInfo().getReserveField9(),
									request.getAdditionalInfo().getReserveField10());
							response = new BillPaymentResponse(infoPay, txnInfoPay, additionalInfoPay);
							transactionStatus = Constants.Status.Fail;
						}
					} else if (aiouGetVoucherResponse.getResponse().getResponseCode()
							.equals(Constants.ResponseCodes.NOT_FOUND)) {
						infoPay = new InfoPay(Constants.ResponseCodes.CONSUMER_NUMBER_NOT_EXISTS,
								Constants.ResponseDescription.CONSUMER_NUMBER_NOT_EXISTS, rrn, stan);
						txnInfoPay = new TxnInfoPay(request.getTxnInfo().getBillerId(),
								request.getTxnInfo().getBillNumber(), paymentRefrence);// paymentRefrence
						additionalInfoPay = new AdditionalInfoPay(request.getAdditionalInfo().getReserveField1(),
								request.getAdditionalInfo().getReserveField2(),
								request.getAdditionalInfo().getReserveField3(),
								request.getAdditionalInfo().getReserveField4(),
								request.getAdditionalInfo().getReserveField5(),
								request.getAdditionalInfo().getReserveField6(),
								request.getAdditionalInfo().getReserveField7(),
								request.getAdditionalInfo().getReserveField8(),
								request.getAdditionalInfo().getReserveField9(),
								request.getAdditionalInfo().getReserveField10());
						response = new BillPaymentResponse(infoPay, txnInfoPay, additionalInfoPay);
						transactionStatus = Constants.Status.Fail;

					} else if (aiouGetVoucherResponse.getResponse().getResponseCode()
							.equals(Constants.ResponseCodes.INVALID_DATA)) {
						infoPay = new InfoPay(Constants.ResponseCodes.INVALID_DATA,
								Constants.ResponseDescription.INVALID_DATA, rrn, stan);
						txnInfoPay = new TxnInfoPay(request.getTxnInfo().getBillerId(),
								request.getTxnInfo().getBillNumber(), paymentRefrence);// paymentRefrence
						additionalInfoPay = new AdditionalInfoPay(request.getAdditionalInfo().getReserveField1(),
								request.getAdditionalInfo().getReserveField2(),
								request.getAdditionalInfo().getReserveField3(),
								request.getAdditionalInfo().getReserveField4(),
								request.getAdditionalInfo().getReserveField5(),
								request.getAdditionalInfo().getReserveField6(),
								request.getAdditionalInfo().getReserveField7(),
								request.getAdditionalInfo().getReserveField8(),
								request.getAdditionalInfo().getReserveField9(),
								request.getAdditionalInfo().getReserveField10());
						response = new BillPaymentResponse(infoPay, txnInfoPay, additionalInfoPay);
						transactionStatus = Constants.Status.Fail;
					} else {
						infoPay = new InfoPay(Constants.ResponseCodes.SERVICE_FAIL,
								Constants.ResponseDescription.SERVICE_FAIL, rrn, stan);
						txnInfoPay = new TxnInfoPay(request.getTxnInfo().getBillerId(),
								request.getTxnInfo().getBillNumber(), paymentRefrence);// paymentRefrence
						additionalInfoPay = new AdditionalInfoPay(request.getAdditionalInfo().getReserveField1(),
								request.getAdditionalInfo().getReserveField2(),
								request.getAdditionalInfo().getReserveField3(),
								request.getAdditionalInfo().getReserveField4(),
								request.getAdditionalInfo().getReserveField5(),
								request.getAdditionalInfo().getReserveField6(),
								request.getAdditionalInfo().getReserveField7(),
								request.getAdditionalInfo().getReserveField8(),
								request.getAdditionalInfo().getReserveField9(),
								request.getAdditionalInfo().getReserveField10());
						response = new BillPaymentResponse(infoPay, txnInfoPay, additionalInfoPay);
						transactionStatus = Constants.Status.Fail;
					}

				}

				else if (aiouGetVoucherResponse.getResponse().getResponseCode()
						.equals(Constants.ResponseCodes.NOT_FOUND)) {
					infoPay = new InfoPay(Constants.ResponseCodes.CONSUMER_NUMBER_NOT_EXISTS,
							Constants.ResponseDescription.CONSUMER_NUMBER_NOT_EXISTS, rrn, stan);
					txnInfoPay = new TxnInfoPay(request.getTxnInfo().getBillerId(),
							request.getTxnInfo().getBillNumber(), paymentRefrence);// paymentRefrence
					additionalInfoPay = new AdditionalInfoPay(request.getAdditionalInfo().getReserveField1(),
							request.getAdditionalInfo().getReserveField2(),
							request.getAdditionalInfo().getReserveField3(),
							request.getAdditionalInfo().getReserveField4(),
							request.getAdditionalInfo().getReserveField5(),
							request.getAdditionalInfo().getReserveField6(),
							request.getAdditionalInfo().getReserveField7(),
							request.getAdditionalInfo().getReserveField8(),
							request.getAdditionalInfo().getReserveField9(),
							request.getAdditionalInfo().getReserveField10());
					response = new BillPaymentResponse(infoPay, txnInfoPay, additionalInfoPay);

					transactionStatus = Constants.Status.Fail;
				}

				else if (aiouGetVoucherResponse.getResponse().getResponseCode()
						.equals(Constants.ResponseCodes.CONSUMER_NUMBER_NOT_EXISTS)) {
					infoPay = new InfoPay(Constants.ResponseCodes.CONSUMER_NUMBER_NOT_EXISTS,
							Constants.ResponseDescription.CONSUMER_NUMBER_NOT_EXISTS, rrn, stan);
					txnInfoPay = new TxnInfoPay(request.getTxnInfo().getBillerId(),
							request.getTxnInfo().getBillNumber(), paymentRefrence);// paymentRefrence
					additionalInfoPay = new AdditionalInfoPay(request.getAdditionalInfo().getReserveField1(),
							request.getAdditionalInfo().getReserveField2(),
							request.getAdditionalInfo().getReserveField3(),
							request.getAdditionalInfo().getReserveField4(),
							request.getAdditionalInfo().getReserveField5(),
							request.getAdditionalInfo().getReserveField6(),
							request.getAdditionalInfo().getReserveField7(),
							request.getAdditionalInfo().getReserveField8(),
							request.getAdditionalInfo().getReserveField9(),
							request.getAdditionalInfo().getReserveField10());
					response = new BillPaymentResponse(infoPay, txnInfoPay, additionalInfoPay);
					transactionStatus = Constants.Status.Fail;
				}

				else {
					infoPay = new InfoPay(Constants.ResponseCodes.BAD_TRANSACTION,
							Constants.ResponseDescription.BAD_TRANSACTION, rrn, stan);
					txnInfoPay = new TxnInfoPay(request.getTxnInfo().getBillerId(),
							request.getTxnInfo().getBillNumber(), paymentRefrence);// paymentRefrence
					additionalInfoPay = new AdditionalInfoPay(request.getAdditionalInfo().getReserveField1(),
							request.getAdditionalInfo().getReserveField2(),
							request.getAdditionalInfo().getReserveField3(),
							request.getAdditionalInfo().getReserveField4(),
							request.getAdditionalInfo().getReserveField5(),
							request.getAdditionalInfo().getReserveField6(),
							request.getAdditionalInfo().getReserveField7(),
							request.getAdditionalInfo().getReserveField8(),
							request.getAdditionalInfo().getReserveField9(),
							request.getAdditionalInfo().getReserveField10());
					response = new BillPaymentResponse(infoPay, txnInfoPay, additionalInfoPay);

					transactionStatus = Constants.Status.Fail;

				}

			} else {
				infoPay = new InfoPay(Constants.ResponseCodes.SERVICE_FAIL, Constants.ResponseDescription.SERVICE_FAIL,
						rrn, stan);
				response = new BillPaymentResponse(infoPay, null, null);

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
						request.getInfo().getRrn(), request.getTxnInfo().getBillerId(),
						request.getTxnInfo().getBillNumber(), channel, username);

			} catch (Exception ex) {
				LOG.error("{}", ex);
			}
			try {

				paymentLoggingService.paymentLog(responseDate, responseDate, rrn, stan,
						response.getInfo().getResponseCode(), response.getInfo().getResponseDesc(), cnic,
						request.getTerminalInfo().getMobile(), name, request.getTxnInfo().getBillNumber(),
						request.getTxnInfo().getBillerId(), inquiryTotalAmountbdUp, dbTransactionFees,
						Constants.ACTIVITY.BillPayment, paymentRefrence, request.getTxnInfo().getBillNumber(),
						transactionStatus, address, transactionFees, dbTax, dbTotal, channel, billStatus,
						request.getTxnInfo().getTranDate(), request.getTxnInfo().getTranTime(), province, transAuthId);

			} catch (Exception ex) {
				LOG.error("{}", ex);
			}
		}

		UtilMethods.generalLog("OUT -  Bill Payment Response {}" + response, LOG);

		return response;

	}
}
