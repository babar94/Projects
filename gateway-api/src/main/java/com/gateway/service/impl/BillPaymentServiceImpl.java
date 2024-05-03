package com.gateway.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import com.gateway.model.mpay.response.billinquiry.pitham.PithamGetVoucherResponse;
import com.gateway.model.mpay.response.billinquiry.pta.DataWrapper;
import com.gateway.model.mpay.response.billinquiry.pta.PtaGetVoucherResponse;
import com.gateway.model.mpay.response.billinquiry.thardeep.ThardeepGetVoucherResponse;
import com.gateway.model.mpay.response.billinquiry.uom.UomGetVoucherResponse;
import com.gateway.model.mpay.response.billpayment.UpdateVoucherResponse;
import com.gateway.model.mpay.response.billpayment.aiou.AiouUpdateVoucherResponse;
import com.gateway.model.mpay.response.billpayment.fbr.FbrUpdateVoucherResponse;
import com.gateway.model.mpay.response.billpayment.offline.OfflineUpdateVoucherResponse;
import com.gateway.model.mpay.response.billpayment.pitham.PithamUpdateVoucherResponse;
import com.gateway.model.mpay.response.billpayment.pta.PtaUpdateVoucherResponse;
import com.gateway.model.mpay.response.billpayment.thardeep.ThardeepUpdateVoucherResponse;
import com.gateway.model.mpay.response.billpayment.uom.UomUpdateVoucherResponse;
import com.gateway.repository.BillerConfigurationRepo;
import com.gateway.repository.PaymentLogRepository;
import com.gateway.repository.SubBillerListRepository;
import com.gateway.repository.TransactionParamsDao;
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
	private BillerConfigurationRepo billerConfigurationRepo;

	@Autowired
	private PaymentLogRepository paymentLogRepository;

	@Autowired
	private ParamsValidatorService paramsValidatorService;

	@Autowired
	private JwtTokenUtil jwtTokenUtil;

	@Autowired
	private SubBillerListRepository subBillerListRepository;

	@Autowired
	private ReservedFieldsValidationService reservedFieldsValidationService;

	@Autowired
	private ObjectMapper objectMapper;

	@Value("${fbr.identification.type}")
	private String identificationType;

	@Value("${uom.bank.mnemonic}")
	private String bankMnemonic;

	@Value("${uom.reserved}")
	private String reserved;

	@Autowired
	private TransactionParamsDao transactionParamsDao;

	@Autowired
	private ObjectMapper mapper;

	@Override
	public BillPaymentResponse billPayment(HttpServletRequest httpRequestData, BillPaymentRequest request) {

		LOG.info("Inside method Bill Payment");
		BillPaymentResponse billPaymentResponse = null;
		BillPaymentValidationResponse billPaymentValidationResponse = null;
		try {
			LOG.info("Request =>:", mapper.writerWithDefaultPrettyPrinter().writeValueAsString(request));

			InfoPay infoPay = null;

			String rrn = request.getInfo().getRrn();
			String stan = request.getInfo().getStan();
			String parentBillerId = null;
			String subBillerId = null;

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
										case BillerConstant.Beoe.BEOE:
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

										case BillerConstant.Pral.KPPSC:
											billPaymentResponse = billPaymentPral(request, httpRequestData);
											break;

										case BillerConstant.Pral.FBR:
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

										case BillerConstant.Pta.PTA:
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

									////////// PITHAM ///////

									else if (billerDetail.getBillerName().equalsIgnoreCase(BillerConstant.Pithm.PITHM)
											&& type.equalsIgnoreCase(Constants.BillerType.ONLINE_BILLER)) {

										switch (subBillerDetail.getSubBillerName()) {

										case BillerConstant.Pithm.PITHM:
											billPaymentResponse = billPaymentPitham(request, httpRequestData);
											break;

										default:
											LOG.info("subBiller does not exists.");
											infoPay = new InfoPay(Constants.ResponseCodes.INVALID_BILLER_ID,
													Constants.ResponseDescription.INVALID_BILLER_ID, rrn, stan);
											billPaymentResponse = new BillPaymentResponse(infoPay, null, null);

											break;
										}
									}

									////////// PITHAM ///////

									////////// THARDEEP ///////

									else if (billerDetail.getBillerName()
											.equalsIgnoreCase(BillerConstant.THARDEEP.THARDEEP)
											&& type.equalsIgnoreCase(Constants.BillerType.ONLINE_BILLER)) {

										switch (subBillerDetail.getSubBillerName()) {

										case BillerConstant.THARDEEP.THARDEEP:
											billPaymentResponse = billPaymentThardeep(request, httpRequestData);
											break;

										default:
											LOG.info("subBiller does not exists.");
											infoPay = new InfoPay(Constants.ResponseCodes.INVALID_BILLER_ID,
													Constants.ResponseDescription.INVALID_BILLER_ID, rrn, stan);
											billPaymentResponse = new BillPaymentResponse(infoPay, null, null);

											break;
										}
									}

									////////// THARDEEP //////
									
									
							        ////////// UOM ///////

									else if (billerDetail.getBillerName()
									.equalsIgnoreCase(BillerConstant.UOM.UOM)
									&& type.equalsIgnoreCase(Constants.BillerType.ONLINE_BILLER)) {

								switch (subBillerDetail.getSubBillerName()) {

								case BillerConstant.UOM.UOM:
									billPaymentResponse = billPaymentUom(request, httpRequestData);
									break;

								default:
									LOG.info("subBiller does not exists.");
									infoPay = new InfoPay(Constants.ResponseCodes.INVALID_BILLER_ID,
											Constants.ResponseDescription.INVALID_BILLER_ID, rrn, stan);
									billPaymentResponse = new BillPaymentResponse(infoPay, null, null);

									break;
								}
							 }

								   ////////// UOM //////////
									

									// AIOU
									// PTA
									else if (billerDetail.getBillerName().equalsIgnoreCase(BillerConstant.Aiou.AIOU)
											&& type.equalsIgnoreCase(Constants.BillerType.ONLINE_BILLER)) { // PRAL

										switch (subBillerDetail.getSubBillerName()) {

										case BillerConstant.Aiou.AIOU:

//											reservedFieldsValidationService.validateReservedFields(request)
//											billPaymentResponse = billPaymentAiou(request, httpRequestData);
//											break;

											// Validate the additional fields of AIOU from the request
											if (reservedFieldsValidationService.validateReservedFields(request,
													parentBillerId)) {
												// Proceed with AIOU bill payment
												billPaymentResponse = billPaymentAiou(request, httpRequestData);
											}

											else {
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
									Constants.ResponseDescription.BILLER_NOT_FOUND_DISABLED, rrn, stan);
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

			UtilMethods.generalLog("IN - BillPayment  " + strDate, LOG);
			LOG.info("Bill Payment Request {}", request);
			LOG.info("Calling GetVoucher");

			ObjectMapper reqMapper = new ObjectMapper();
			String requestAsString = reqMapper.writeValueAsString(request);

			ProvinceTransaction provinceTransaction = null;

//			if (request.getTxnInfo().getBillNumber() == null
//					|| request.getTxnInfo().getBillNumber().equalsIgnoreCase("")) {
//
//				response = new BillPaymentValidationResponse(Constants.ResponseCodes.INVALID_DATA,
//						Constants.ResponseDescription.INVALID_BILLER_NUMBER, rrn, stan);
//				return response;
//
//			}
//
//			if (request.getTxnInfo().getTranAuthId() == null
//					|| request.getTxnInfo().getTranAuthId().equalsIgnoreCase("")
//					|| !Pattern.matches(pattern, request.getTxnInfo().getTranAuthId())) {
//
//				response = new BillPaymentValidationResponse(Constants.ResponseCodes.INVALID_DATA,
//						Constants.ResponseDescription.INVALID_AUTH_ID, rrn, stan);
//				return response;
//
//			}

			Pair<Boolean, String> validationResponse = paramsValidatorService.validateRequestParams(requestAsString);
			if (!validationResponse.getLeft()) {
				response = new BillPaymentValidationResponse(Constants.ResponseCodes.INVALID_DATA,
						validationResponse.getRight(), rrn, stan);
				return response;
			}

//			if (!paramsValidatorService.validateRequestParams(requestAsString)) {
//				response = new BillPaymentValidationResponse(Constants.ResponseCodes.INVALID_DATA,
//						Constants.ResponseDescription.INVALID_DATA, rrn, stan);
//				return response;
//			}

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
				LOG.info("Duplicate Tran-Auth Id ");
				response = new BillPaymentValidationResponse(Constants.ResponseCodes.DUPLICATE_TRANSACTION_AUTH_ID,
						Constants.ResponseDescription.DUPLICATE_TRANSACTION_AUTH_ID, rrn, stan);
				return response;
			}

//			if (request.getTxnInfo().getBillerId() != null || !request.getTxnInfo().getBillerId().isEmpty()) {
//
//				Optional<BillerConfiguration> billerConfiguration = billerConfigurationRepo
//						.findByBillerId(request.getTxnInfo().getBillerId().substring(0, 2));
//
//				if (!billerConfiguration.isPresent()) {
//
//					response = new BillPaymentValidationResponse(Constants.ResponseCodes.INVALID_DATA,
//							Constants.ResponseDescription.INVALID_DATA, rrn, stan);
//					return response;
//				}
//			}

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
		String bankName = "", bankCode = "", branchName = "", branchCode = "";

		try {

			if (request.getBranchInfo() != null) {
				bankName = request.getBranchInfo().getBankName();
				bankCode = request.getBranchInfo().getBankCode();
				branchName = request.getBranchInfo().getBranchName();
				branchCode = request.getBranchInfo().getBranchCode();
			}

			String[] result = jwtTokenUtil.getTokenInformation(httpRequestData);
			username = result[0];
			channel = result[1];

			ArrayList<String> inquiryParams = new ArrayList<String>();
			inquiryParams.add(Constants.MPAY_REQUEST_METHODS.BEOE_BILL_INQUIRY);
			inquiryParams.add(request.getTxnInfo().getBillNumber().trim());
			inquiryParams.add(rrn);
			inquiryParams.add(stan);

			getVoucherResponse = serviceCaller.get(inquiryParams, GetVoucherResponse.class, stan,
					Constants.ACTIVITY.BillInquiry, BillerConstant.Beoe.BEOE);

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
									Constants.ACTIVITY.BillPayment, BillerConstant.Beoe.BEOE);

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
										Constants.ACTIVITY.BillInquiry, BillerConstant.Beoe.BEOE);

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
						request.getTxnInfo().getTranDate(), request.getTxnInfo().getTranTime(), province, transAuthId,
						bankName, bankCode, branchName, branchCode);

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
		String bankName = "", bankCode = "", branchName = "", branchCode = "";

		try {

			if (request.getBranchInfo() != null) {
				bankName = request.getBranchInfo().getBankName();
				bankCode = request.getBranchInfo().getBankCode();
				branchName = request.getBranchInfo().getBranchName();
				branchCode = request.getBranchInfo().getBranchCode();
			}

			String[] result = jwtTokenUtil.getTokenInformation(httpRequestData);
			username = result[0];
			channel = result[1];

			ArrayList<String> inquiryParams = new ArrayList<String>();
			inquiryParams.add(Constants.MPAY_REQUEST_METHODS.PRAL_BILL_PAYMENT);
			inquiryParams.add(request.getTxnInfo().getBillNumber().trim());
			inquiryParams.add(rrn);
			inquiryParams.add(stan);

			getVoucherResponse = serviceCaller.get(inquiryParams, GetVoucherResponse.class, stan,
					Constants.ACTIVITY.BillInquiry, BillerConstant.Pral.KPPSC);

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
									Constants.ACTIVITY.BillPayment, BillerConstant.Pral.KPPSC);

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
										Constants.ACTIVITY.BillInquiry, BillerConstant.Pral.KPPSC);

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
						request.getTxnInfo().getTranDate(), request.getTxnInfo().getTranTime(), province, transAuthId,
						bankName, bankCode, branchName, branchCode);

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
		String transAuthId = request.getTxnInfo().getTranAuthId();
		String paymentRefrence = "";
		String billingDate = "";
		String billingMonth = "";
		String dueDateStr = "";
		String expiryDate = "";
		String amountPaidInDueDate = "";
		String amountPaidAfterDueDate = "";
		String paymentwithinduedateflag = "false";
		String bankName = "", bankCode = "", branchName = "", branchCode = "";

		String channel = "";
		String username = "";
		BigDecimal requestAmount = null;
		try {

			if (request.getBranchInfo() != null) {
				bankName = request.getBranchInfo().getBankName();
				bankCode = request.getBranchInfo().getBankCode();
				branchName = request.getBranchInfo().getBranchName();
				branchCode = request.getBranchInfo().getBranchCode();
			}

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
					Constants.ACTIVITY.BillInquiry, "");

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
										OfflineUpdateVoucherResponse.class, rrn, Constants.ACTIVITY.BillPayment, "");

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
											OfflineGetVoucherResponse.class, stan, Constants.ACTIVITY.BillInquiry, "");

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
						request.getTxnInfo().getTranDate(), request.getTxnInfo().getTranTime(), province, transAuthId,
						bankName, bankCode, branchName, branchCode);

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
		String bankName = "", bankCode = "", branchName = "", branchCode = "";

		try {
			if (request.getBranchInfo() != null) {
				bankName = request.getBranchInfo().getBankName();
				bankCode = request.getBranchInfo().getBankCode();
				branchName = request.getBranchInfo().getBranchName();
				branchCode = request.getBranchInfo().getBranchCode();
			}

			String[] result = jwtTokenUtil.getTokenInformation(httpRequestData);
			username = result[0];
			channel = result[1];

			ArrayList<String> inquiryParams = new ArrayList<String>();
			inquiryParams.add(Constants.MPAY_REQUEST_METHODS.PTA_BILL_INQUIRY);
			inquiryParams.add(request.getTxnInfo().getBillNumber().trim());
			inquiryParams.add(rrn);
			inquiryParams.add(stan);

			getVoucherResponse = serviceCaller.get(inquiryParams, PtaGetVoucherResponse.class, rrn,
					Constants.ACTIVITY.BillInquiry, BillerConstant.Pta.PTA);

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
										PtaUpdateVoucherResponse.class, rrn, Constants.ACTIVITY.BillPayment,
										BillerConstant.Pta.PTA);

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
											stan, Constants.ACTIVITY.BillInquiry, BillerConstant.Pta.PTA);

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
						request.getTxnInfo().getTranDate(), request.getTxnInfo().getTranTime(), province, transAuthId,
						bankName, bankCode, branchName, branchCode);

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
		String bankName = "", bankCode = "", branchName = "", branchCode = "";

		String channel = "";
		String username = "";

		try {

			if (request.getBranchInfo() != null) {
				bankName = request.getBranchInfo().getBankName();
				bankCode = request.getBranchInfo().getBankCode();
				branchName = request.getBranchInfo().getBranchName();
				branchCode = request.getBranchInfo().getBranchCode();
			}

			String[] result = jwtTokenUtil.getTokenInformation(httpRequestData);
			username = result[0];
			channel = result[1];

			ArrayList<String> inquiryParams = new ArrayList<String>();
			inquiryParams.add(Constants.MPAY_REQUEST_METHODS.PRAL_FBR_BILL_INQUIRY);
			inquiryParams.add(identificationType);// Identification_Type
			inquiryParams.add(request.getTxnInfo().getBillNumber().trim());
			inquiryParams.add(bankMnemonic);// Bank_Mnemonic
			inquiryParams.add(request.getAdditionalInfo().getReserveField1());// Bank_Mnemonic
			inquiryParams.add(rrn);
			inquiryParams.add(stan);

			fbrGetVoucherResponse = serviceCaller.get(inquiryParams, FbrGetVoucherResponse.class, rrn,
					Constants.ACTIVITY.BillInquiry, BillerConstant.Pral.FBR);

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

//								
//								
								ArrayList<String> ubpsBillParams = new ArrayList<String>();
								ubpsBillParams.add(Constants.MPAY_REQUEST_METHODS.PRAL_FBR_BILL_PAYMENT);
								ubpsBillParams.add(identificationType); // Identification_Type
								ubpsBillParams.add(request.getTxnInfo().getBillNumber().trim());
								ubpsBillParams.add(bankMnemonic); // Bank_Mnemonic
								ubpsBillParams.add("");// RESERVED
								ubpsBillParams.add(request.getTxnInfo().getTranAuthId());
								ubpsBillParams.add(request.getTxnInfo().getTranDate());
								ubpsBillParams.add(request.getTxnInfo().getTranTime());
								ubpsBillParams.add(utilMethods.convertAmountToISOFormatWithoutPlusSign(
										request.getTxnInfo().getTranAmount().trim()));
								ubpsBillParams.add(rrn);
								ubpsBillParams.add(stan);

								updateVoucherResponse = serviceCaller.get(ubpsBillParams,
										FbrUpdateVoucherResponse.class, rrn, Constants.ACTIVITY.BillPayment,
										BillerConstant.Pral.FBR);

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
											FbrGetVoucherResponse.class, rrn, Constants.ACTIVITY.BillInquiry,
											BillerConstant.Pral.FBR);
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
						transactionStatus, address, transactionFees, dbTax, dbTotal, channel, billStatus,
						request.getTxnInfo().getTranDate(), request.getTxnInfo().getTranTime(), province, transAuthId,
						bankName, bankCode, branchName, branchCode);

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
		String paymentRefrence = utilMethods.getRRN();
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

		BigDecimal inquiryTotalAmountbdUp = null;
		String dbBillStatus = "";
		BigDecimal requestAmount = null;
		String dueDateStr = "";
//		String dueDate = "";

		String channel = "";
		String username = "";
		BigDecimal amountAfterDueDate = null;
		BigDecimal txnAmount = null;
		String bankName = "", bankCode = "", branchName = "", branchCode = "";
		try {

			if (request.getBranchInfo() != null) {
				bankName = request.getBranchInfo().getBankName();
				bankCode = request.getBranchInfo().getBankCode();
				branchName = request.getBranchInfo().getBranchName();
				branchCode = request.getBranchInfo().getBranchCode();
			}

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
					Constants.ACTIVITY.BillInquiry, BillerConstant.Aiou.AIOU);

			if (aiouGetVoucherResponse != null) {
				info = new Info(aiouGetVoucherResponse.getResponse().getResponseCode(),
						aiouGetVoucherResponse.getResponse().getResponseDesc(), rrn, stan);
				if (aiouGetVoucherResponse.getResponse().getResponseCode().equals(ResponseCodes.OK)) {

					name = aiouGetVoucherResponse.getResponse().getAiouGetVoucher().getResponseBillInquiry().getName();
					dueDateStr = aiouGetVoucherResponse.getResponse().getAiouGetVoucher().getResponseBillInquiry()
							.getDueDate();

					String billstatus = "";

					BigDecimal requestAmountafterduedate = null;
					if (aiouGetVoucherResponse.getResponse().getAiouGetVoucher() != null) {

						billStatus = aiouGetVoucherResponse.getResponse().getAiouGetVoucher().getResponseBillInquiry()
								.getBillStatus().trim().equalsIgnoreCase("U") ? Constants.BILL_STATUS.BILL_UNPAID
										: Constants.BILL_STATUS.BILL_PAID;
						// dbBillStatus = billStatus;

						if (billStatus.equalsIgnoreCase(Constants.BILL_STATUS.BILL_UNPAID)) {

							String amountStr = aiouGetVoucherResponse.getResponse().getAiouGetVoucher()
									.getResponseBillInquiry().getAmountWithinDueDate();
							String amountAfterDueDateStr = aiouGetVoucherResponse.getResponse().getAiouGetVoucher()
									.getResponseBillInquiry().getAmountAfterDueDate();

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
							txnAmount = new BigDecimal(request.getTxnInfo().getTranAmount());
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
								LOG.info("Invalid due date input or empty from Aiou ");
								if (txnAmount.compareTo(amountInDueToDate) != 0) {
									infoPay = new InfoPay(Constants.ResponseCodes.AMMOUNT_MISMATCH,
											Constants.ResponseDescription.AMMOUNT_MISMATCH, rrn, stan);
									response = new BillPaymentResponse(infoPay, null, null);
									return response;
								}
							}

							try {

								String tranDate = request.getTxnInfo().getTranDate();

								if (tranDate.length() == 8) {
									tranDate = utilMethods.transactionDateFormater(tranDate);
								}
								LOG.info("Calling UpdateVoucher for Aiou");

								ArrayList<String> ubpsBillParams = new ArrayList<String>();
//		   String urlParameters = "aiou-updatevoucher-call,ABL,9990000003332,A21,Waseem Abbas,Naeem Abbas,1234567890123,0201,A203450,12PRI12345,
								// 03068575572,20240220,0000000000200,Allied Bank Limited,1234,G10
								// Islamabad,3254,654321,"+ GenerateRRN() + ",001345";

								ubpsBillParams.add(Constants.MPAY_REQUEST_METHODS.AIOU_BILL_PAYMENT);
								ubpsBillParams.add(Constants.BankMnemonic.ABL);// Bank_Mnemonic
								ubpsBillParams.add(request.getTxnInfo().getBillNumber().trim());
								ubpsBillParams.add(request.getAdditionalInfo().getReserveField1());// semester
								ubpsBillParams.add(request.getAdditionalInfo().getReserveField3());// name
								ubpsBillParams.add(request.getAdditionalInfo().getReserveField4());// fname
								ubpsBillParams.add(request.getAdditionalInfo().getReserveField5());// cnic
								ubpsBillParams.add(request.getAdditionalInfo().getReserveField2());// programme//								
															
								ubpsBillParams.add(request.getAdditionalInfo().getReserveField6());// rollNumber
								ubpsBillParams.add(request.getAdditionalInfo().getReserveField8());// registrationNumber
								ubpsBillParams.add(request.getAdditionalInfo().getReserveField7());// Contact No

								

//								ubpsBillParams.add(request.getTerminalInfo().getMobile());//
								ubpsBillParams.add(request.getTxnInfo().getTranDate());// dueDate
								ubpsBillParams.add(utilMethods.formatAmountAn13(
										Double.parseDouble(request.getTxnInfo().getTranAmount().trim())));// AmountPaid

//								////////   Sajid old work - Start ///////
//								
//								ubpsBillParams.add(request.getAdditionalInfo().getReserveField3());// BankName
//								ubpsBillParams.add(request.getAdditionalInfo().getReserveField4());// BankCode
//								ubpsBillParams.add(request.getAdditionalInfo().getReserveField5());// Branchname
//								ubpsBillParams.add(request.getAdditionalInfo().getReserveField6());// BankCode
//								

								//////// Sajid old work - End ///////

								//////// Babar Work New - Start ///////

								ubpsBillParams.add(request.getBranchInfo().getBankName());// BankName
								ubpsBillParams.add(request.getBranchInfo().getBankCode());// BankCode
								ubpsBillParams.add(request.getBranchInfo().getBranchName());// BranchName
								ubpsBillParams.add(request.getBranchInfo().getBranchCode());// BranchCode

								//////// Babar Work New - End ///////

								//ubpsBillParams.add("123456");// Stan

								ubpsBillParams.add(rrn);
								ubpsBillParams.add(stan);

								updateVoucherResponse = serviceCaller.get(ubpsBillParams,
										AiouUpdateVoucherResponse.class, rrn, Constants.ACTIVITY.BillPayment,
										BillerConstant.Aiou.AIOU);

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
											AiouGetVoucherResponse.class, rrn, Constants.ACTIVITY.BillInquiry,
											BillerConstant.Aiou.AIOU);

									if (aiouGetVoucherResponse != null) {
										if (aiouGetVoucherResponse.getResponse().getResponseCode()
												.equals(ResponseCodes.OK)) {
											if (aiouGetVoucherResponse.getResponse().getAiouGetVoucher()
													.getResponseBillInquiry().getBillStatus()
													.equalsIgnoreCase(Constants.BILL_STATUS.BILL_PAID)
													|| aiouGetVoucherResponse.getResponse().getAiouGetVoucher()
															.getResponseBillInquiry().getBillStatus().equalsIgnoreCase(
																	Constants.BILL_STATUS.BILL_PAID.substring(0))) {

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
						request.getTxnInfo().getBillerId(), txnAmount, amountInDueToDate, amountAfterDueDate,
						dbTransactionFees, Constants.ACTIVITY.BillPayment, paymentRefrence,
						request.getTxnInfo().getBillNumber(), transactionStatus, address, transactionFees, dbTax,
						dbTotal, channel, billStatus, request.getTxnInfo().getTranDate(),
						request.getTxnInfo().getTranTime(), province, transAuthId, bankName, bankCode, branchName,
						branchCode);

			} catch (Exception ex) {
				LOG.error("{}", ex);
			}
		}

		UtilMethods.generalLog("OUT -  Bill Payment Response {}" + response, LOG);

		return response;

	}

	@Override
	public BillPaymentResponse billPaymentPitham(BillPaymentRequest request, HttpServletRequest httpRequestData) {

		LOG.info("PITHAM Bill Payment Request {} ", request.toString());

		BillPaymentResponse response = null;
		PithamGetVoucherResponse pithamgetVoucherResponse = null;
		PithamUpdateVoucherResponse pithanUpdateVoucherResponse = null;
		Date requestedDate = new Date();
		InfoPay infoPay = null;
		TxnInfoPay txnInfoPay = null;
		AdditionalInfoPay additionalInfoPay = null;
		String transactionStatus = "";
		String rrn = request.getInfo().getRrn(); // utilMethods.getRRN();
		String rrnReq = request.getInfo().getRrn().substring(0, 10); // utilMethods.getRRN();

		LOG.info("RRN :{ }", rrn);
		String stan = request.getInfo().getStan();
		String transAuthId = request.getTxnInfo().getTranAuthId();
		String amountPaidInDueDate = "" , amountPaid = "";
		String channel = "";
		String username = "";

		ArrayList<String> inquiryParams = new ArrayList<String>();
		ArrayList<String> paymentParams = new ArrayList<String>();

		
		String amountWithInDueDateRes = "" , amountAfterDueDateRes= "";
		String billerNameRes = "" , dueDateRes = "" , billingMonthRes = "" , billStatusCodeRes = "" , billStatusDescRes = "";
		String pattern = "\\d+\\.\\d{2}";
		String billerId = "" , billerNumber = "";
		String paymentRefrence = utilMethods.getRRN();

		String bankName = "", bankCode = "", branchName = "", branchCode = "";

		try {

			if (request.getBranchInfo() != null) {
				bankName = request.getBranchInfo().getBankName();
				bankCode = request.getBranchInfo().getBankCode();
				branchName = request.getBranchInfo().getBranchName();
				branchCode = request.getBranchInfo().getBranchCode();
			}

			String[] result = jwtTokenUtil.getTokenInformation(httpRequestData);

			username = result[0];
			channel = result[1];

			inquiryParams.add(Constants.MPAY_REQUEST_METHODS.PITHAM_BILL_INQUIRY);
			inquiryParams.add(request.getAdditionalInfo().getReserveField1().trim());
			inquiryParams.add(request.getTxnInfo().getBillNumber().trim());
			inquiryParams.add(rrnReq);
			
			if (!Pattern.matches(pattern, request.getTxnInfo().getTranAmount())) {

				infoPay = new InfoPay(Constants.ResponseCodes.AMMOUNT_MISMATCH,
						Constants.ResponseDescription.AMMOUNT_MISMATCH, rrn, stan);
				response = new BillPaymentResponse(infoPay, null, null);
				return response;

			}
			
			pithamgetVoucherResponse = serviceCaller.get(inquiryParams, PithamGetVoucherResponse.class, rrn,
					Constants.ACTIVITY.BillInquiry, BillerConstant.Pithm.PITHM);

			if (pithamgetVoucherResponse != null) {

				if (pithamgetVoucherResponse.getPithmGetVoucher() == null) {

					infoPay = new InfoPay(Constants.ResponseCodes.SERVICE_FAIL,
							Constants.ResponseDescription.SERVICE_FAIL, rrn, stan);

					response = new BillPaymentResponse(infoPay, null, null);

					transactionStatus = Constants.Status.Fail;

					return response;

				}

			  else if (pithamgetVoucherResponse.getResponseCode()
						.equalsIgnoreCase(Constants.ResponseCodes.CONSUMER_NUMBER_NOT_EXISTS)) {

					infoPay = new InfoPay(Constants.ResponseCodes.CONSUMER_NUMBER_NOT_EXISTS,
							Constants.ResponseDescription.CONSUMER_NUMBER_NOT_EXISTS, rrn, stan);

					response = new BillPaymentResponse(infoPay, null, null);

					return response;
				}

				else if (pithamgetVoucherResponse.getResponseCode()
						.equalsIgnoreCase(Constants.ResponseCodes.UNKNOWN_ERROR)) {

					infoPay = new InfoPay(pithamgetVoucherResponse.getResponseCode(),
							pithamgetVoucherResponse.getResponseDesc(), rrn, stan);

					response = new BillPaymentResponse(infoPay, null, null);

					return response;
				}

				else if (pithamgetVoucherResponse.getResponseCode()
						.equalsIgnoreCase(Constants.ResponseCodes.UNAUTHORISED_USER)) {

					infoPay = new InfoPay(pithamgetVoucherResponse.getResponseCode(),
							pithamgetVoucherResponse.getResponseDesc(), rrn, stan);

					response = new BillPaymentResponse(infoPay, null, null);

					return response;
				}

				
				else if (pithamgetVoucherResponse.getResponseCode().equalsIgnoreCase(ResponseCodes.BILL_ALREADY_PAID)) {


					billerId = request.getTxnInfo().getBillerId();
					billerNumber = request.getTxnInfo().getBillNumber();

					infoPay = new InfoPay(pithamgetVoucherResponse.getResponseCode(),
							Constants.ResponseDescription.BILL_ALREADY_PAID, rrn, stan);

					txnInfoPay = new TxnInfoPay(billerId, billerNumber, paymentRefrence);

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

					transactionStatus = Constants.Status.Success;

					response = new BillPaymentResponse(infoPay, txnInfoPay, additionalInfoPay);
					return response;

				}
				
				else if (pithamgetVoucherResponse.getResponseCode().equalsIgnoreCase(Constants.ResponseCodes.OK)) {

				
				billerNameRes = pithamgetVoucherResponse.getPithmGetVoucher().getGetInquiryResult().getStudentName();
				billingMonthRes = pithamgetVoucherResponse.getPithmGetVoucher().getGetInquiryResult().getBillingMonth();
				billStatusCodeRes = pithamgetVoucherResponse.getPithmGetVoucher().getGetInquiryResult().getStatus();
				billStatusDescRes = pithamgetVoucherResponse.getPithmGetVoucher().getGetInquiryResult().getStatusDesc();
				dueDateRes = pithamgetVoucherResponse.getPithmGetVoucher().getGetInquiryResult().getDueDate();
				amountWithInDueDateRes = pithamgetVoucherResponse.getPithmGetVoucher().getGetInquiryResult()
						.getAmountWidDate();
				amountAfterDueDateRes = pithamgetVoucherResponse.getPithmGetVoucher().getGetInquiryResult()
						.getAmountAdDate();

				 if (billStatusDescRes.equalsIgnoreCase(Constants.BILL_STATUS.BILL_EXPIRED)) {

					infoPay = new InfoPay(Constants.ResponseCodes.INVALID_DATA, Constants.ResponseDescription.EXPIRED,
							rrn, stan);

					response = new BillPaymentResponse(infoPay, null, null);

					transactionStatus = Constants.Status.Expired;

					return response;

				}

				else if (billStatusDescRes.equalsIgnoreCase(Constants.BILL_STATUS.BILL_BLOCK)) {

					infoPay = new InfoPay(Constants.ResponseCodes.INVALID_DATA, Constants.ResponseDescription.BLOCK,
							rrn, stan);

					response = new BillPaymentResponse(infoPay, null, null);

					transactionStatus = Constants.Status.Block;

					return response;

				}


				paymentParams.add(Constants.MPAY_REQUEST_METHODS.PITHAM_BILL_PAYMENT);
				paymentParams.add(request.getTxnInfo().getBillNumber().trim());
				paymentParams.add(rrn);
				paymentParams.add(request.getAdditionalInfo().getReserveField1());
				paymentParams.add(request.getTxnInfo().getTranAmount().trim());
				paymentParams.add(request.getTxnInfo().getTranDate().trim());
				paymentParams.add(request.getAdditionalInfo().getReserveField2());
				paymentParams.add(rrnReq);

				if (utilMethods.isValidInput(dueDateRes)) {
					LocalDate currentDate = LocalDate.now();

					try {
						LocalDate dueDate = utilMethods.parseDueDateWithoutDashes(dueDateRes);

						///// Check due date conditions///

						if (utilMethods.isPaymentWithinDueDate(currentDate, dueDate)) {
							if (Double.valueOf(request.getTxnInfo().getTranAmount())
									.compareTo(Double.valueOf(amountWithInDueDateRes)) != 0) {
								infoPay = new InfoPay(Constants.ResponseCodes.AMMOUNT_MISMATCH,
										Constants.ResponseDescription.AMMOUNT_MISMATCH, rrn, stan);
								response = new BillPaymentResponse(infoPay, null, null);
								return response;
							}
						} else {
							if (Double.valueOf(request.getTxnInfo().getTranAmount())
									.compareTo(Double.valueOf(amountAfterDueDateRes)) != 0) {
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
							.compareTo(Double.valueOf(amountWithInDueDateRes)) != 0) {
						infoPay = new InfoPay(Constants.ResponseCodes.AMMOUNT_MISMATCH,
								Constants.ResponseDescription.AMMOUNT_MISMATCH, rrn, stan);
						response = new BillPaymentResponse(infoPay, null, null);
						return response;
					}
				}

				pithanUpdateVoucherResponse = serviceCaller.get(paymentParams, PithamUpdateVoucherResponse.class, rrn,
						Constants.ACTIVITY.BillPayment, BillerConstant.Pithm.PITHM);


					infoPay = new InfoPay(
							pithanUpdateVoucherResponse.getPithmUpdateVoucher().getGetpayvoucherresult()
									.getStatusCode(),
							pithanUpdateVoucherResponse.getPithmUpdateVoucher().getGetpayvoucherresult()
									.getStatusDesc(),
							rrn, stan);

					txnInfoPay = new TxnInfoPay(request.getTxnInfo().getBillerId(),
							request.getTxnInfo().getBillNumber(), paymentRefrence);

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

					return response;
				}

				else {


					infoPay = new InfoPay(Constants.ResponseCodes.SERVICE_FAIL, Constants.ResponseDescription.SERVICE_FAIL,
							rrn, stan);

					response = new BillPaymentResponse(infoPay, null, null);
					
					return response;


				}
			}

			else {

				infoPay = new InfoPay(Constants.ResponseCodes.SERVICE_FAIL, Constants.ResponseDescription.SERVICE_FAIL,
						rrn, stan);

				response = new BillPaymentResponse(infoPay, null, null);

				transactionStatus = Constants.Status.Fail;

				return response;
			}
		}

		catch (Exception e) {

			LOG.info("Exception in bill payment ");
		}

		finally {

			LOG.info("Bill Payment Response {}", response);

			try {

				String requestAsString = objectMapper.writeValueAsString(request);
				String responseAsString = objectMapper.writeValueAsString(response);

				auditLoggingService.auditLog(Constants.ACTIVITY.BillPayment, response.getInfo().getResponseCode(),
						response.getInfo().getResponseDesc(), requestAsString, responseAsString, requestedDate,
						new Date(), request.getInfo().getRrn(), request.getTxnInfo().getBillerId(),
						request.getTxnInfo().getBillNumber(), channel, username);

			} catch (Exception ex) {
				LOG.error("{Exception Audit Logs}", ex);
			}

			try {

				paymentLoggingService.paymentLog(requestedDate, new Date(), rrn, stan,
						response.getInfo().getResponseCode(), response.getInfo().getResponseDesc(),
						billerNameRes, request.getTxnInfo().getBillNumber(),
						request.getTxnInfo().getBillerId(),
						new BigDecimal(amountWithInDueDateRes),
						new BigDecimal(amountAfterDueDateRes), Constants.ACTIVITY.BillPayment,
						transactionStatus, channel, Constants.BILL_STATUS.BILL_PAID, request.getTxnInfo().getTranDate(),
						request.getTxnInfo().getTranTime(), transAuthId,
						new BigDecimal(request.getTxnInfo().getTranAmount()), dueDateRes,
						billingMonthRes, paymentRefrence, bankName, bankCode,
						branchName, branchCode, "", "");

				LOG.info(" --- Bill Payment Method End --- ");

			} catch (Exception ex) {
				LOG.error("{Exception payment Logs}", ex);
			}

		}
		return response;
	}

	@Override
	public BillPaymentResponse billPaymentThardeep(BillPaymentRequest request, HttpServletRequest httpRequestData) {

		LOG.info("THARDEEP Bill Payment Request {} ", request.toString());

		BillPaymentResponse response = null;
		ThardeepGetVoucherResponse thardeepgetVoucherResponse = null;
		ThardeepUpdateVoucherResponse thardeepUpdateVoucherResponse = null;
		Date requestedDate = new Date();
		InfoPay infoPay = null;
		TxnInfoPay txnInfoPay = null;
		AdditionalInfoPay additionalInfoPay = null;
		String transactionStatus = "";
		String rrn = request.getInfo().getRrn(); // utilMethods.getRRN();

		LOG.info("RRN :{ }", rrn);
		String stan = request.getInfo().getStan();
		String transAuthId = request.getTxnInfo().getTranAuthId();
		String channel = "";
		String username = "";

		ArrayList<String> inquiryParams = new ArrayList<String>();
		ArrayList<String> paymentParams = new ArrayList<String>();

	
		BigDecimal amountInDueToDate = null , amountAfterDate = null ;
		String pattern = "\\d+\\.\\d{2}" ; 
		String paymentRefrence = utilMethods.getRRN();

		String bankName = "", bankCode = "", branchName = "", branchCode = "";

		String billerNameRes = "", billInquiryCode = "", billInquiryDesc = "", dueDateRes = "", billingMonthRes = "",
				billStatusRes = "", tranAuthIdRes = "", amountRes = "", cnicRes = "" , formattedDueDate = "";

		try {

			if (request.getBranchInfo() != null) {
				bankName = request.getBranchInfo().getBankName();
				bankCode = request.getBranchInfo().getBankCode();
				branchName = request.getBranchInfo().getBranchName();
				branchCode = request.getBranchInfo().getBranchCode();
			}

			String[] result = jwtTokenUtil.getTokenInformation(httpRequestData);

			username = result[0];
			channel = result[1];

			inquiryParams.add(Constants.MPAY_REQUEST_METHODS.THARDEEP_BILL_INQUIRY);
			inquiryParams.add(request.getTxnInfo().getBillNumber().trim());
			inquiryParams.add(rrn);
			inquiryParams.add(stan);

			thardeepgetVoucherResponse = serviceCaller.get(inquiryParams, ThardeepGetVoucherResponse.class, rrn,
					Constants.ACTIVITY.BillInquiry, BillerConstant.THARDEEP.THARDEEP);

			
			if (!Pattern.matches(pattern, request.getTxnInfo().getTranAmount())) {

				infoPay = new InfoPay(Constants.ResponseCodes.AMMOUNT_MISMATCH,
						Constants.ResponseDescription.AMMOUNT_MISMATCH, rrn, stan);
				response = new BillPaymentResponse(infoPay, null, null);
				return response;

			}
			
			if (thardeepgetVoucherResponse != null) {
				
				if (thardeepgetVoucherResponse.getResponse() == null) {

					infoPay = new InfoPay(Constants.ResponseCodes.SERVICE_FAIL,
							Constants.ResponseDescription.SERVICE_FAIL, rrn, stan);

					response = new BillPaymentResponse(infoPay, null, null);

					transactionStatus = Constants.Status.Fail;

					return response;

				}
				
				
				else if (thardeepgetVoucherResponse.getResponse().getResponseCode()
						.equalsIgnoreCase(Constants.ResponseCodes.CONSUMER_NUMBER_NOT_EXISTS)) {

					infoPay = new InfoPay(Constants.ResponseCodes.CONSUMER_NUMBER_NOT_EXISTS,
							Constants.ResponseDescription.CONSUMER_NUMBER_NOT_EXISTS, rrn, stan);

					response = new BillPaymentResponse(infoPay, null, null);

					return response;
				}

				else if (thardeepgetVoucherResponse.getResponse().getResponseCode()
						.equalsIgnoreCase(Constants.ResponseCodes.CONSUMER_NUMBER_BLOCK)) {

					infoPay = new InfoPay(Constants.ResponseCodes.CONSUMER_NUMBER_BLOCK,
							Constants.ResponseDescription.CONSUMER_NUMBER_BLOCK, rrn, stan);

					response = new BillPaymentResponse(infoPay, null, null);

					return response;
				}

				else if (thardeepgetVoucherResponse.getResponse().getResponseCode()
						.equalsIgnoreCase(Constants.ResponseCodes.UNKNOWN_ERROR)) {

					infoPay = new InfoPay(thardeepgetVoucherResponse.getResponse().getResponseCode(),
							thardeepgetVoucherResponse.getResponse().getResponseDesc(), rrn, stan);

					response = new BillPaymentResponse(infoPay, null, null);

					return response;
				}

				
				else if (thardeepgetVoucherResponse.getResponse().getResponseCode()
							.equalsIgnoreCase(Constants.ResponseCodes.OK)) {

					
				billerNameRes = thardeepgetVoucherResponse.getResponse().getThardeepGetVoucher().getConsumerName();
				billingMonthRes = thardeepgetVoucherResponse.getResponse().getThardeepGetVoucher().getBillingMonth();
				billInquiryCode = thardeepgetVoucherResponse.getResponse().getThardeepGetVoucher().getResponseCode();
				billInquiryDesc = thardeepgetVoucherResponse.getResponse().getThardeepGetVoucher().getStatusResponse();
				billStatusRes = thardeepgetVoucherResponse.getResponse().getThardeepGetVoucher().getBillStatus();
				dueDateRes = thardeepgetVoucherResponse.getResponse().getThardeepGetVoucher().getDueDate();

				formattedDueDate = utilMethods.formatDueDate(dueDateRes);

				tranAuthIdRes = thardeepgetVoucherResponse.getResponse().getThardeepGetVoucher().getAuthId();
				amountRes = thardeepgetVoucherResponse.getResponse().getThardeepGetVoucher().getAmount();
				cnicRes = thardeepgetVoucherResponse.getResponse().getThardeepGetVoucher().getCnicNo();

			
				if (billStatusRes.equalsIgnoreCase(Constants.BILL_STATUS_SINGLE_ALPHABET.BILL_EXPIRED)) {

					infoPay = new InfoPay(Constants.ResponseCodes.INVALID_DATA, Constants.ResponseDescription.EXPIRED,
							rrn, stan);

					response = new BillPaymentResponse(infoPay, null, null);

					transactionStatus = Constants.Status.Expired;

					return response;

				}

				else if (billStatusRes.equalsIgnoreCase(Constants.BILL_STATUS_SINGLE_ALPHABET.BILL_BLOCK)) {

					infoPay = new InfoPay(Constants.ResponseCodes.INVALID_DATA, Constants.ResponseDescription.BLOCK,
							rrn, stan);

					response = new BillPaymentResponse(infoPay, null, null);

					transactionStatus = Constants.Status.Block;

					return response;

				}


				paymentParams.add(Constants.MPAY_REQUEST_METHODS.THARDEEP_BILL_PAYMENT);
				paymentParams.add(request.getTxnInfo().getBillNumber().trim());
				paymentParams.add(tranAuthIdRes);
				paymentParams.add(request.getTxnInfo().getTranAmount().trim());
				paymentParams.add(request.getTxnInfo().getTranDate().trim());
				paymentParams.add(request.getTxnInfo().getTranTime().trim());
				paymentParams.add(rrn);
				paymentParams.add(stan);

				if (Double.valueOf(request.getTxnInfo().getTranAmount()).compareTo(Double.valueOf(amountRes)) != 0) {
					infoPay = new InfoPay(Constants.ResponseCodes.AMMOUNT_MISMATCH,
							Constants.ResponseDescription.AMMOUNT_MISMATCH, rrn, stan);
					response = new BillPaymentResponse(infoPay, null, null);
					return response;
				}
				
				
				thardeepUpdateVoucherResponse = serviceCaller.get(paymentParams, ThardeepUpdateVoucherResponse.class,
						rrn, Constants.ACTIVITY.BillPayment, BillerConstant.THARDEEP.THARDEEP);
	

					infoPay = new InfoPay(thardeepUpdateVoucherResponse.getResponse().getResponseCode(),
							thardeepUpdateVoucherResponse.getResponse().getResponseDesc(), rrn, stan);

					txnInfoPay = new TxnInfoPay(request.getTxnInfo().getBillerId(),
							request.getTxnInfo().getBillNumber(), paymentRefrence);

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

					return response;
				

				}
				
				else {


					infoPay = new InfoPay(Constants.ResponseCodes.SERVICE_FAIL, Constants.ResponseDescription.SERVICE_FAIL,
							rrn, stan);
					
					response = new BillPaymentResponse(infoPay, null, null);
					
					return response;


				}

			}

			else {

				infoPay = new InfoPay(Constants.ResponseCodes.SERVICE_FAIL, Constants.ResponseDescription.SERVICE_FAIL,
						rrn, stan);

				response = new BillPaymentResponse(infoPay, null, null);

				transactionStatus = Constants.Status.Fail;

				return response;
			}
		}

		catch (Exception e) {

			LOG.info("Exception in bill payment ");
		}

		finally {

			LOG.info("Bill Payment Response {}", response);

			try {

				String requestAsString = objectMapper.writeValueAsString(request);
				String responseAsString = objectMapper.writeValueAsString(response);

				auditLoggingService.auditLog(Constants.ACTIVITY.BillPayment, response.getInfo().getResponseCode(),
						response.getInfo().getResponseDesc(), requestAsString, responseAsString, requestedDate,
						new Date(), request.getInfo().getRrn(), request.getTxnInfo().getBillerId(),
						request.getTxnInfo().getBillNumber(), channel, username);

			} catch (Exception ex) {
				LOG.error("{Exception Audit Logs}", ex);
			}

			try {

				paymentLoggingService.paymentLog(requestedDate, new Date(), rrn, stan,
						response.getInfo().getResponseCode(), response.getInfo().getResponseDesc(),
						billerNameRes, request.getTxnInfo().getBillNumber(),
						request.getTxnInfo().getBillerId(),amountInDueToDate,amountAfterDate, Constants.ACTIVITY.BillPayment,
						transactionStatus, channel, Constants.BILL_STATUS.BILL_PAID, request.getTxnInfo().getTranDate(),
						request.getTxnInfo().getTranTime(), transAuthId,
						new BigDecimal(request.getTxnInfo().getTranAmount()),
					    formattedDueDate,
						billingMonthRes, paymentRefrence, bankName, bankCode,
						branchName, branchCode, tranAuthIdRes, "");

				LOG.info(" --- Bill Payment Method End --- ");

			} catch (Exception ex) {
				LOG.error("{Exception payment Logs}", ex);
			}

		}
		return response;
	}
	
	
	@Override
	public BillPaymentResponse billPaymentUom(BillPaymentRequest request, HttpServletRequest httpRequestData) {

		LOG.info("Uom Bill Payment Request {} ", request.toString());

		BillPaymentResponse response = null;
		UomGetVoucherResponse uomgetVoucherResponse = null;
		UomUpdateVoucherResponse uomUpdateVoucherResponse = null;
		Date requestedDate = new Date();
		InfoPay infoPay = null;
		TxnInfoPay txnInfoPay = null;
		AdditionalInfoPay additionalInfoPay = null;
		String transactionStatus = "";
		String rrn = request.getInfo().getRrn(); // utilMethods.getRRN();

		LOG.info("RRN :{ }", rrn);
		String stan = request.getInfo().getStan();
		String transAuthId = request.getTxnInfo().getTranAuthId();
		String channel = "";
		String username = "";

		ArrayList<String> inquiryParams = new ArrayList<String>();
		ArrayList<String> paymentParams = new ArrayList<String>();

		BigDecimal amountInDueToDate = null ,amountAfterDate = null , requestAmountWithinDueDate=null , requestAmountafterduedate=null , txnAmount = null;
		String amountWithInDueDateRes = "" , amountAfterDueDateRes , billerNameRes = "" , dueDateRes = "" , billingMonthRes = "" , billStatusRes = "";
		String pattern = "\\d+\\.\\d{2}";
		String billerId = "", billerNumber = "";
		String paymentRefrence = utilMethods.getRRN();
		LocalDate dueDate = null;
		String bankName = "", bankCode = "", branchName = "", branchCode = "";

		try {

			if (request.getBranchInfo() != null) {
				bankName = request.getBranchInfo().getBankName();
				bankCode = request.getBranchInfo().getBankCode();
				branchName = request.getBranchInfo().getBranchName();
				branchCode = request.getBranchInfo().getBranchCode();
			}

			String[] result = jwtTokenUtil.getTokenInformation(httpRequestData);

			username = result[0];
			channel = result[1];

			inquiryParams.add(Constants.MPAY_REQUEST_METHODS.UOM_BILL_INQUIRY);
			inquiryParams.add(request.getTxnInfo().getBillNumber().trim());
			inquiryParams.add(bankMnemonic);
			inquiryParams.add(request.getAdditionalInfo().getReserveField1().trim());
			inquiryParams.add(rrn);
			inquiryParams.add(stan);

			uomgetVoucherResponse = serviceCaller.get(inquiryParams, UomGetVoucherResponse.class, rrn,
					Constants.ACTIVITY.BillInquiry, BillerConstant.UOM.UOM);

			
			if (!Pattern.matches(pattern, request.getTxnInfo().getTranAmount())) {

				infoPay = new InfoPay(Constants.ResponseCodes.AMMOUNT_MISMATCH,
						Constants.ResponseDescription.AMMOUNT_MISMATCH, rrn, stan);
				response = new BillPaymentResponse(infoPay, null, null);
				return response;

			}
		
			if (uomgetVoucherResponse != null) {

				if (uomgetVoucherResponse.getResponse() == null) {

					infoPay = new InfoPay(Constants.ResponseCodes.SERVICE_FAIL,
							Constants.ResponseDescription.SERVICE_FAIL, rrn, stan);

					response = new BillPaymentResponse(infoPay, null, null);

					transactionStatus = Constants.Status.Fail;

					return response;

				}

				else if (uomgetVoucherResponse.getResponse().getResponseCode()
						.equalsIgnoreCase(Constants.ResponseCodes.CONSUMER_NUMBER_NOT_EXISTS)) {

					infoPay = new InfoPay(Constants.ResponseCodes.CONSUMER_NUMBER_NOT_EXISTS,
							Constants.ResponseDescription.CONSUMER_NUMBER_NOT_EXISTS, rrn, stan);

					response = new BillPaymentResponse(infoPay, null, null);

					return response;
				}

				else if (uomgetVoucherResponse.getResponse().getResponseCode()
						.equalsIgnoreCase(Constants.ResponseCodes.UNKNOWN_ERROR)) {

					infoPay = new InfoPay(uomgetVoucherResponse.getResponse().getResponseCode(),
							uomgetVoucherResponse.getResponse().getResponseDesc(), rrn, stan);

					response = new BillPaymentResponse(infoPay, null, null);

					return response;
				}

				
				else if (uomgetVoucherResponse.getResponse().getResponseCode().equalsIgnoreCase(ResponseCodes.BILL_ALREADY_PAID)) {

					    billerId =request.getTxnInfo().getBillerId();
						billerNumber = request.getTxnInfo().getBillNumber(); 
						
						infoPay = new InfoPay(uomgetVoucherResponse.getResponse().getResponseCode(),
								Constants.ResponseDescription.BILL_ALREADY_PAID, rrn, stan);

						txnInfoPay = new TxnInfoPay(billerId, billerNumber, paymentRefrence);

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

						transactionStatus = Constants.Status.Success;

						response = new BillPaymentResponse(infoPay, txnInfoPay, additionalInfoPay);
						return response;

					}

				
				else if (uomgetVoucherResponse.getResponse().getResponseCode().equalsIgnoreCase(Constants.ResponseCodes.OK)) {
	
				
				billerNameRes = uomgetVoucherResponse.getResponse().getUomgetvoucher().getConsumerDetail();
				billingMonthRes = uomgetVoucherResponse.getResponse().getUomgetvoucher().getBilling_Month();
				billStatusRes = uomgetVoucherResponse.getResponse().getUomgetvoucher().getBillStatus();
				dueDateRes = uomgetVoucherResponse.getResponse().getUomgetvoucher().getDueDate();
				amountWithInDueDateRes = uomgetVoucherResponse.getResponse().getUomgetvoucher().getAmount_Within_DueDate();
				amountAfterDueDateRes = uomgetVoucherResponse.getResponse().getUomgetvoucher().getAmount_After_DueDate();

				
				
				////////// Within due date conversion
				
				if (!amountWithInDueDateRes.isEmpty()) {
					
					requestAmountWithinDueDate = new BigDecimal(amountWithInDueDateRes.replaceFirst("^\\+?0+", ""));
					requestAmountWithinDueDate = requestAmountWithinDueDate.divide(BigDecimal.valueOf(100));

					// Set scale to 2 and round up
					amountInDueToDate = requestAmountWithinDueDate.setScale(2, RoundingMode.UP);

				}
				

				//////// After due date conversion
				
				if (!amountAfterDueDateRes.isEmpty()) {
					
					requestAmountafterduedate = new BigDecimal(amountAfterDueDateRes.replaceFirst("^\\+?0+", ""));
					requestAmountafterduedate = requestAmountafterduedate.divide(BigDecimal.valueOf(100));

					// Set scale to 2 and round up
					amountAfterDate = requestAmountafterduedate.setScale(2, RoundingMode.UP);
				}

				if (billStatusRes.equalsIgnoreCase(Constants.BILL_STATUS_SINGLE_ALPHABET.BILL_EXPIRED)) {

					infoPay = new InfoPay(Constants.ResponseCodes.INVALID_DATA, Constants.ResponseDescription.EXPIRED,
							rrn, stan);

					response = new BillPaymentResponse(infoPay, null, null);

					transactionStatus = Constants.Status.Expired;

					return response;

				}

				else if (billStatusRes.equalsIgnoreCase(Constants.BILL_STATUS_SINGLE_ALPHABET.BILL_BLOCK)) {

					infoPay = new InfoPay(Constants.ResponseCodes.INVALID_DATA, Constants.ResponseDescription.BLOCK,
							rrn, stan);

					response = new BillPaymentResponse(infoPay, null, null);

					transactionStatus = Constants.Status.Block;

					return response;

				}


				paymentParams.add(Constants.MPAY_REQUEST_METHODS.UOM_BILL_PAYMENT);
				paymentParams.add(request.getTxnInfo().getBillNumber().trim());
				paymentParams.add(request.getTxnInfo().getTranAmount().trim());
				paymentParams.add(request.getTxnInfo().getTranDate().trim());
				paymentParams.add(request.getTxnInfo().getTranTime().trim());
				paymentParams.add(transAuthId);
				paymentParams.add(bankMnemonic);
				paymentParams.add(request.getAdditionalInfo().getReserveField1());
				paymentParams.add(rrn);
				paymentParams.add(stan);


				txnAmount = new BigDecimal(request.getTxnInfo().getTranAmount());				

				
				if (utilMethods.isValidInput(dueDateRes)) {
					LocalDate currentDate = LocalDate.now();

					try {
						
						dueDate = utilMethods.parseDueDateWithoutDashes(dueDateRes);

						///// Check due date conditions///

						if (utilMethods.isPaymentWithinDueDate(currentDate, dueDate)) {
							if (txnAmount.compareTo(amountInDueToDate) != 0) {
								infoPay = new InfoPay(Constants.ResponseCodes.AMMOUNT_MISMATCH,
										Constants.ResponseDescription.AMMOUNT_MISMATCH, rrn, stan);
								response = new BillPaymentResponse(infoPay, null, null);
								return response;
							}
						} else {
							if (txnAmount.compareTo(amountAfterDate) != 0) {
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
					if (txnAmount.compareTo(amountInDueToDate) != 0) {
						infoPay = new InfoPay(Constants.ResponseCodes.AMMOUNT_MISMATCH,
								Constants.ResponseDescription.AMMOUNT_MISMATCH, rrn, stan);
						response = new BillPaymentResponse(infoPay, null, null);
						return response;
					}
				}

				uomUpdateVoucherResponse = serviceCaller.get(paymentParams, UomUpdateVoucherResponse.class, rrn,
						Constants.ACTIVITY.BillPayment, BillerConstant.UOM.UOM);

				
				}	
				
				if(uomUpdateVoucherResponse!=null) {
				
				if (uomUpdateVoucherResponse.getResponse().getUomUpdateVoucher().getResponseCode().equalsIgnoreCase(Constants.ResponseCodes.OK)) {

					infoPay = new InfoPay(
							uomUpdateVoucherResponse.getResponse().getUomUpdateVoucher().getResponseCode(),
							Constants.ResponseDescription.OPERATION_SUCCESSFULL,
							rrn, stan);

					txnInfoPay = new TxnInfoPay(request.getTxnInfo().getBillerId(),
							request.getTxnInfo().getBillNumber(), paymentRefrence);

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

					return response;
				
				  }
				}
				else {

				
					infoPay = new InfoPay(Constants.ResponseCodes.SERVICE_FAIL, Constants.ResponseDescription.SERVICE_FAIL,
							rrn, stan);
					
					response = new BillPaymentResponse(infoPay, null, null);
					
					return response;

				}
				
			}

			else {

				infoPay = new InfoPay(Constants.ResponseCodes.SERVICE_FAIL, Constants.ResponseDescription.SERVICE_FAIL,
						rrn, stan);

				response = new BillPaymentResponse(infoPay, null, null);

				transactionStatus = Constants.Status.Fail;

				return response;
			}
		}

		catch (Exception e) {

			LOG.info("Exception in bill payment ");
		}

		finally {

			LOG.info("Bill Payment Response {}", response);

			try {

				String requestAsString = objectMapper.writeValueAsString(request);
				String responseAsString = objectMapper.writeValueAsString(response);

				auditLoggingService.auditLog(Constants.ACTIVITY.BillPayment, response.getInfo().getResponseCode(),
						response.getInfo().getResponseDesc(), requestAsString, responseAsString, requestedDate,
						new Date(), request.getInfo().getRrn(), request.getTxnInfo().getBillerId(),
						request.getTxnInfo().getBillNumber(), channel, username);

			} catch (Exception ex) {
				LOG.error("{Exception Audit Logs}", ex);
			}

			try {

				paymentLoggingService.paymentLog(requestedDate, new Date(), rrn, stan,
						response.getInfo().getResponseCode(), response.getInfo().getResponseDesc(),
						billerNameRes, request.getTxnInfo().getBillNumber(),
						request.getTxnInfo().getBillerId(),
						amountInDueToDate,amountAfterDate, Constants.ACTIVITY.BillPayment,
						transactionStatus, channel, Constants.BILL_STATUS.BILL_PAID, request.getTxnInfo().getTranDate(),
						request.getTxnInfo().getTranTime(), transAuthId,
						new BigDecimal(request.getTxnInfo().getTranAmount()),String.valueOf(dueDate),
						billingMonthRes, paymentRefrence, bankName, bankCode,
						branchName, branchCode, "", "");

				LOG.info(" --- Bill Payment Method End --- ");

			} catch (Exception ex) {
				LOG.error("{Exception payment Logs}", ex);
			}

		}
		return response;
	}
	

}
