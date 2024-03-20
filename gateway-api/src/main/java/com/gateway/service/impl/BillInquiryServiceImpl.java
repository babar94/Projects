package com.gateway.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gateway.entity.BillerConfiguration;
import com.gateway.entity.PaymentLog;
import com.gateway.entity.ProvinceTransaction;
import com.gateway.entity.SubBillersList;
import com.gateway.entity.TransactionParams;
import com.gateway.model.mpay.response.billinquiry.GetVoucherResponse;
import com.gateway.model.mpay.response.billinquiry.aiou.AiouGetVoucherResponse;
import com.gateway.model.mpay.response.billinquiry.fbr.FbrGetVoucherResponse;
import com.gateway.model.mpay.response.billinquiry.offline.OfflineGetVoucherResponse;
import com.gateway.model.mpay.response.billinquiry.pitham.PithamGetVoucherResponse;
import com.gateway.model.mpay.response.billinquiry.pta.DataWrapper;
import com.gateway.model.mpay.response.billinquiry.pta.PtaGetVoucherResponse;
import com.gateway.model.mpay.response.billinquiry.thardeep.ThardeepGetVoucherResponse;
import com.gateway.repository.BillerConfigurationRepo;
import com.gateway.repository.PaymentLogRepository;
import com.gateway.repository.SubBillerListRepository;
import com.gateway.repository.TransactionParamsDao;
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

import jakarta.servlet.http.HttpServletRequest;

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
	private ObjectMapper objectMapper;

	@Autowired
	private PaymentLogRepository paymentloggingRepository;

	@Autowired
	private TransactionParamsDao transactionParamsDao;

	@Autowired
	private ParamsValidatorServiceImpl validatorServiceImpl;

	@Override
	public BillInquiryResponse billInquiry(HttpServletRequest httpRequestData, BillInquiryRequest request) {

		LOG.info("================ REQUEST billInquiry ================");
		LOG.info("===>> REQUEST ::" + request.toString());
		BillInquiryResponse billInquiryResponse = null;
		BillInquiryValidationResponse billInquiryValidationResponse = null;
		Info info = null;
		String parentBillerId = null;
		String subBillerId = null;
		String rrn = request.getInfo().getRrn();
		String stan = request.getInfo().getStan();


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
					Boolean parentBillerIsActive = billerDetail.getIsActive();

					if (parentBillerIsActive) {
						Optional<SubBillersList> subBiller = subBillerListRepository
								.findBySubBillerIdAndBillerConfiguration(subBillerId, billerDetail);
						if (subBiller.isPresent()) {
							SubBillersList subBillerDetail = subBiller.get();
							billInquiryValidationResponse = billInquiryValidations(httpRequestData, request,
									parentBillerId);
							Boolean subBillerIsActive = subBillerDetail.getIsActive();
							if (subBillerIsActive) {
								if (billInquiryValidationResponse != null
										&& billInquiryValidationResponse.getResponseCode().equalsIgnoreCase("00")) {
									if (billerDetail.getBillerName().equalsIgnoreCase(BillerConstant.Beoe.BEOE)
											&& type.equalsIgnoreCase(Constants.BillerType.ONLINE_BILLER)) {
										switch (subBillerDetail.getSubBillerName()) {
										case BillerConstant.Beoe.BEOE:
											billInquiryResponse = billInquiryBEOE(request, httpRequestData);
											break;

										default:
											LOG.info("subBiller does not exists.");
											info = new Info(Constants.ResponseCodes.INVALID_BILLER_ID,
													Constants.ResponseDescription.INVALID_BILLER_ID, rrn, stan);
											billInquiryResponse = new BillInquiryResponse(info, null, null);
											break;

										}
									} else if (billerDetail.getBillerName().equalsIgnoreCase("PRAL")
											&& type.equalsIgnoreCase(Constants.BillerType.ONLINE_BILLER)) {

										switch (subBillerDetail.getSubBillerName()) {

										case BillerConstant.Pral.KPPSC:
											billInquiryResponse = billInquiryKppsc(request,
													billInquiryValidationResponse, httpRequestData);
											break;
										case BillerConstant.Pral.FBR:
											billInquiryResponse = billInquiryFbr(request, httpRequestData);
											break;

										default:
											LOG.info("subBiller does not exists.");
											info = new Info(Constants.ResponseCodes.INVALID_BILLER_ID,
													Constants.ResponseDescription.INVALID_BILLER_ID, rrn, stan);
											billInquiryResponse = new BillInquiryResponse(info, null, null);

											break;
										}
									}

									else if (billerDetail.getBillerName().equalsIgnoreCase(BillerConstant.Pta.PTA)
											&& type.equalsIgnoreCase(Constants.BillerType.ONLINE_BILLER)) {

										switch (subBillerDetail.getSubBillerName()) {

										case BillerConstant.Pta.PTA:
											billInquiryResponse = billInquiryPta(request, httpRequestData);
											break;

										default:
											LOG.info("subBiller does not exists.");
											info = new Info(Constants.ResponseCodes.INVALID_BILLER_ID,
													Constants.ResponseDescription.INVALID_BILLER_ID, rrn, stan);
											billInquiryResponse = new BillInquiryResponse(info, null, null);

											break;
										}
									}

									else if (billerDetail.getBillerName().equalsIgnoreCase(BillerConstant.Aiou.AIOU)
											&& type.equalsIgnoreCase(Constants.BillerType.ONLINE_BILLER)) {

										switch (subBillerDetail.getSubBillerName()) {

										case BillerConstant.Aiou.AIOU:
											billInquiryResponse = billInquiryAiou(request, httpRequestData);
											break;

										default:
											LOG.info("subBiller does not exists.");
											info = new Info(Constants.ResponseCodes.INVALID_BILLER_ID,
													Constants.ResponseDescription.INVALID_BILLER_ID, rrn, stan);
											billInquiryResponse = new BillInquiryResponse(info, null, null);

											break;
										}
									}

									////////// PITHAM ///////

									else if (billerDetail.getBillerName().equalsIgnoreCase(BillerConstant.Pithm.PITHM)
											&& type.equalsIgnoreCase(Constants.BillerType.ONLINE_BILLER)) {

										switch (subBillerDetail.getSubBillerName()) {

										case BillerConstant.Pithm.PITHM:
											billInquiryResponse = billInquiryPITHAM(request, httpRequestData);
											break;

										default:
											LOG.info("subBiller does not exists.");
											info = new Info(Constants.ResponseCodes.INVALID_BILLER_ID,
													Constants.ResponseDescription.INVALID_BILLER_ID, rrn, stan);
											billInquiryResponse = new BillInquiryResponse(info, null, null);

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
											billInquiryResponse = billInquiryTHARDEEP(request, httpRequestData);
											break;

										default:
											LOG.info("subBiller does not exists.");
											info = new Info(Constants.ResponseCodes.INVALID_BILLER_ID,
													Constants.ResponseDescription.INVALID_BILLER_ID, rrn, stan);
											billInquiryResponse = new BillInquiryResponse(info, null, null);

											break;
										}
									}

									////////// THARDEEP ///////

									else if (type.equalsIgnoreCase(Constants.BillerType.OFFLINE_BILLER)) {
										// offline apis
										billInquiryResponse = billInquiryOffline(httpRequestData, request,
												parentBillerId, subBillerId);

									} else {
										info = new Info(Constants.ResponseCodes.BILLER_NOT_FOUND_DISABLED,
												Constants.ResponseDescription.BILLER_NOT_FOUND_DISABLED, rrn, stan);
										billInquiryResponse = new BillInquiryResponse(info, null, null);
									}
								} else {

									info = new Info(Constants.ResponseCodes.INVALID_DATA,
											billInquiryValidationResponse.getResponseDesc(),
											billInquiryValidationResponse.getRrn(),
											billInquiryValidationResponse.getStan());
									billInquiryResponse = new BillInquiryResponse(info, null, null);
								}
							} else {
								info = new Info(Constants.ResponseCodes.BILLER_DISABLED,
										Constants.ResponseDescription.Biller_Disabled, rrn, stan);
								billInquiryResponse = new BillInquiryResponse(info, null, null);
							}
						} else {
							info = new Info(Constants.ResponseCodes.BILLER_NOT_FOUND_DISABLED,
									Constants.ResponseDescription.BILLER_NOT_FOUND_DISABLED, rrn, stan);
							billInquiryResponse = new BillInquiryResponse(info, null, null);
						}
					} else {
						info = new Info(Constants.ResponseCodes.BILLER_DISABLED,
								Constants.ResponseDescription.Biller_Disabled, rrn, stan);
						billInquiryResponse = new BillInquiryResponse(info, null, null);
					}
				} else {
					info = new Info(Constants.ResponseCodes.BILLER_NOT_FOUND_DISABLED,
							Constants.ResponseDescription.BILLER_NOT_FOUND_DISABLED, rrn, stan);
					billInquiryResponse = new BillInquiryResponse(info, null, null);
				}
			} else {
				info = new Info(Constants.ResponseCodes.INVALID_BILLER_ID,
						Constants.ResponseDescription.INVALID_BILLER_ID, rrn, stan);
				billInquiryResponse = new BillInquiryResponse(info, null, null);
			}

		} catch (Exception ex) {
			LOG.error("Exception in billInquiry method", ex);
		}

		return billInquiryResponse;
	}

	public BillInquiryValidationResponse billInquiryValidations(HttpServletRequest httpRequestData,
			BillInquiryRequest request, String parentBillerId) {

		LOG.info("billInquiryValidations {} ", request.toString());

		BillInquiryValidationResponse response = new BillInquiryValidationResponse();
		String channel = "";
		String username = "";
		String rrn = request.getInfo().getRrn();
		String stan = request.getInfo().getStan();

		TransactionParams paramsDaoRrn = transactionParamsDao.findByParamName("rrn");
		TransactionParams paramsDaoStan = transactionParamsDao.findByParamName("stan");

		
		
		////// rrn regex match
		
		String regexRrn = paramsDaoRrn.getRegex();

		boolean matchRrn = rrn.matches(regexRrn);

		if (!matchRrn) {
			if (!StringUtils.isNumeric(rrn))
				rrn = "";
		}
		
		
		///// stan regex match
		
		String regexStan = paramsDaoStan.getRegex();

		boolean matchStan = rrn.matches(regexStan);

		if (!matchStan) {
			if (!StringUtils.isNumeric(stan))
				stan = "";
		}

		try {

			ObjectMapper reqMapper = new ObjectMapper();
			String requestAsString = reqMapper.writeValueAsString(request);
			ProvinceTransaction provinceTransaction = null;

			if (request.getTxnInfo().getBillNumber() == null
					|| request.getTxnInfo().getBillNumber().equalsIgnoreCase("")) {

				response = new BillInquiryValidationResponse(Constants.ResponseCodes.INVALID_DATA,
						Constants.ResponseDescription.INVALID_BILLER_NUMBER, rrn, stan);
				return response;

			}

			if (!paramsValidatorService.validateRequestParams(requestAsString)) {
				response = new BillInquiryValidationResponse(Constants.ResponseCodes.INVALID_DATA,
						Constants.ResponseDescription.INVALID_DATA, rrn, stan);
				return response;
			}

			if (request.getTxnInfo().getBillerId() != null || !request.getTxnInfo().getBillerId().isEmpty()) {
				// billersList = billerListRepository.findByBillerId(billerId).orElse(null);//
				// biller id
				Optional<BillerConfiguration> billersList = billerConfigurationRepo.findByBillerId(parentBillerId);
				if (!billersList.isPresent()) {
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
				Optional<BillerConfiguration> billersList = billerConfigurationRepo.findByBillerId(parentBillerId);
				if (!billersList.isPresent()) {
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

	public BillInquiryResponse billInquiryBEOE(BillInquiryRequest request, HttpServletRequest httpRequestData) {

		LOG.info("BEOE Bill Inquiry Request {} ", request.toString());

		BillInquiryResponse response = null;
		GetVoucherResponse getVoucherResponse = null;
		Info info = null;
		Date strDate = new Date();
		String rrn = request.getInfo().getRrn(); // utilMethods.getRRN();
		String stan = request.getInfo().getStan(); // utilMethods.getStan();
		String transAuthId = ""; // utilMethods.getStan();
		String transactionStatus = "";

		double transactionFees = 0;
		String cnic = "";
		String mobile = "";
		String address = "";
		String name = "";
		String billStatus = "";
		Double dbAmount = null;
		double dbTax = 0;
		double dbTransactionFees = 0;
		double dbTotal = 0;
		String province = "";
		String channel = "";
		String username = "";
		String amountPaidInDueDate = "";
		String datePaid = "";

		String billingMonth = "";
		String dueDAte = "";
		String oneBillNumber = "";
		BigDecimal requestTotalAmountbdUp = null, amountPaid = null;
		Double amountInDueToDate = null;
		String amountAfterDueDate = "";
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

			getVoucherResponse = serviceCaller.get(inquiryParams, GetVoucherResponse.class, rrn,
					Constants.ACTIVITY.BillInquiry);

			if (getVoucherResponse != null) {
				info = new Info(getVoucherResponse.getResponse().getResponse_code(),
						getVoucherResponse.getResponse().getResponse_desc(), rrn, stan);
				if (getVoucherResponse.getResponse().getResponse_code().equals(ResponseCodes.OK)) {

					String billstatus = "";

					if (getVoucherResponse.getResponse().getGetvoucher() != null) {

						requestTotalAmountbdUp = BigDecimal // 450.0
								.valueOf(
										Double.parseDouble(getVoucherResponse.getResponse().getGetvoucher().getTotal()))
								.setScale(2, RoundingMode.UP);

						amountInDueToDate = utilMethods.bigDecimalToDouble(requestTotalAmountbdUp);
						// amountPaidInDueDate = utilMethods.formatAmount(requestTotalAmountbdUp, 12);
						amountAfterDueDate = String.valueOf(amountInDueToDate);

						cnic = getVoucherResponse.getResponse().getGetvoucher().getCnic();
						mobile = getVoucherResponse.getResponse().getGetvoucher().getMobile();
						address = getVoucherResponse.getResponse().getGetvoucher().getAddress();
						name = getVoucherResponse.getResponse().getGetvoucher().getName();
						address = getVoucherResponse.getResponse().getGetvoucher().getAddress();
						billStatus = getVoucherResponse.getResponse().getGetvoucher().getStatus();
						dbAmount = amountInDueToDate;
						oneBillNumber = getVoucherResponse.getResponse().getGetvoucher().getOneBillNumber();
						if (oneBillNumber == null) {
							oneBillNumber = "";
						}
//						amountPaid = String.format("%012d",
//								Integer.parseInt(getVoucherResponse.getResponse().getGetvoucher().getTotal()));

						dbTotal = requestTotalAmountbdUp.doubleValue();
						if (getVoucherResponse.getResponse().getGetvoucher().getStatus()
								.equalsIgnoreCase(Constants.BILL_STATUS.BILL_PAID)) {
							PaymentLog paymentLog = paymentLogRepository
									.findFirstByBillerIdAndBillerNumberAndBillStatusIgnoreCaseAndActivityAndResponseCodeOrderByIDDesc(
											request.getTxnInfo().getBillerId().trim(),
											request.getTxnInfo().getBillNumber().trim(),
											Constants.BILL_STATUS.BILL_PAID, Constants.ACTIVITY.BillPayment,
											Constants.ResponseCodes.OK);

//							PaymentLog paymentLog = paymentLogRepository.findFirstByBillerNumberAndBillStatus(request.getTxnInfo().getBillNumber().trim(), Constants.BILL_STATUS.BILL_PAID);
							if (paymentLog != null) {
								// datePaid = paymentLog.getTranDate();
								// billingMonth = utilMethods.formatDateString(datePaid);
								billstatus = "P";
								transAuthId = paymentLog.getTranAuthId();
								amountPaid = paymentLog.getAmountPaid();

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
							String.valueOf(requestTotalAmountbdUp), String.valueOf(amountAfterDueDate), transAuthId,
							oneBillNumber);

					AdditionalInfo additionalInfo = new AdditionalInfo(request.getAdditionalInfo().getReserveField1(),

							request.getAdditionalInfo().getReserveField2(),
							request.getAdditionalInfo().getReserveField3(),
							request.getAdditionalInfo().getReserveField4(),
							request.getAdditionalInfo().getReserveField5(),
							request.getAdditionalInfo().getReserveField6(),
							request.getAdditionalInfo().getReserveField7(),
							request.getAdditionalInfo().getReserveField8(),
							request.getAdditionalInfo().getReserveField9(),
							request.getAdditionalInfo().getReserveField10());

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
						request.getTxnInfo().getBillerId(), request.getTxnInfo().getBillNumber(), channel, username);

			} catch (Exception ex) {
				LOG.error("{}", ex);
			}
			try {

				paymentLoggingService.paymentLog(responseDate, responseDate, rrn, stan,
						response.getInfo().getResponseCode(), response.getInfo().getResponseDesc(), cnic,
						request.getTerminalInfo().getMobile(), name, request.getTxnInfo().getBillNumber(),
						request.getTxnInfo().getBillerId(), amountPaid, new BigDecimal(amountInDueToDate),
						new BigDecimal(amountAfterDueDate), dbTransactionFees, Constants.ACTIVITY.BillInquiry, "",
						request.getTxnInfo().getBillNumber(), transactionStatus, address, transactionFees, dbTax,
						dbTotal, channel, billStatus, request.getTxnInfo().getTranDate(),
						request.getTxnInfo().getTranTime(), province, transAuthId, bankName, bankCode, branchName,
						branchCode);

			} catch (Exception ex) {
				LOG.error("{}", ex);
			}

		}
		return response;

	}

	public BillInquiryResponse billInquiryKppsc(BillInquiryRequest request,
			BillInquiryValidationResponse billInquiryValidationResponse, HttpServletRequest httpRequestData) {

		LOG.info("PRAL Bill Inquiry Request {} ", request.toString());

		BillInquiryResponse response = null;
		GetVoucherResponse getVoucherResponse = null;
		Date strDate = new Date();
		String rrn = request.getInfo().getRrn();
		String stan = request.getInfo().getStan();
		String transAuthId = "";
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
		String bankName = "", bankCode = "", branchName = "", branchCode = "";

		BigDecimal requestTotalAmountbdUp = null;
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
			inquiryParams.add(Constants.MPAY_REQUEST_METHODS.PRAL_BILL_INQUIRY);
			inquiryParams.add("1000042312130000002");
			inquiryParams.add(rrn);
			inquiryParams.add(stan);

			getVoucherResponse = serviceCaller.get(inquiryParams, GetVoucherResponse.class, rrn,
					Constants.ACTIVITY.BillInquiry);

		} catch (Exception ex) {

			LOG.error("{}", ex);

		} finally {

			LOG.info("PRAL KPPSC Bill Inquiry Response {}", response);
			Date responseDate = new Date();
			try {

				String requestAsString = objectMapper.writeValueAsString(request);
				String responseAsString = objectMapper.writeValueAsString(response);

				auditLoggingService.auditLog(Constants.ACTIVITY.BillInquiry, response.getInfo().getResponseCode(),
						response.getInfo().getResponseDesc(), requestAsString, responseAsString, strDate, strDate, rrn,
						request.getTxnInfo().getBillerId(), request.getTxnInfo().getBillNumber(), channel, username);

			} catch (Exception ex) {
				LOG.error("{}", ex);
			}
			try {

//				paymentLoggingService.paymentLog(responseDate, responseDate, rrn, stan,
//						response.getInfo().getResponseCode(), response.getInfo().getResponseDesc(), cnic,
//						request.getTerminalInfo().getMobile(), name, request.getTxnInfo().getBillNumber(),
//						request.getTxnInfo().getBillerId(), requestTotalAmountbdUp, dbTransactionFees,
//						Constants.ACTIVITY.BillInquiry, "", request.getTxnInfo().getBillNumber(), transactionStatus,
//						address, transactionFees, dbTax, dbTotal, channel, billStatus,
//						request.getTxnInfo().getTranDate(), request.getTxnInfo().getTranTime(), province, transAuthId);

			} catch (Exception ex) {
				LOG.error("Exception {}", ex);
			}

		}
		return response;
	}

	@Override
	public BillInquiryResponse billInquiryOffline(HttpServletRequest httpRequestData, BillInquiryRequest request,
			String parentBiller, String subBiller) {
		// TODO Auto-generated method stub

		LOG.info("Offline Bill Inquiry Request {} {} ", request.toString());

		BillInquiryResponse response = null;
		OfflineGetVoucherResponse getVoucherResponse = null;
		Info info = null;
		Date strDate = new Date();
		String rrn = request.getInfo().getRrn(); // utilMethods.getRRN();
		String stan = request.getInfo().getStan(); // utilMethods.getStan();
		// String transAuthId = request.getInfo().getStan(); // utilMethods.getStan();
		String transAuthId = "";
		String transactionStatus = "";

		double transactionFees = 0;
		String cnic = "";
		String mobile = "";
		String address = "";
		String name = "";
		String billStatus = "";
		double amountInDueToDate = 0;// double dbAmount = 0;

		double dbTax = 0;
		double dbTransactionFees = 0;
		double dbTotal = 0;
		String province = "";
		String channel = "";
		String username = "";
		String amountPaidInDueDate = "";
		String amountPaidAfterDueDate = "";
		String datePaid = "";
		String billingMonth = "";
		String dueDate = "";
		String billingDate = "";
		String expiryDate = "";
		String oneBillNumber = "";
		BigDecimal requestAmount = null, amountPaid = null;
		double amountAfterDueDate = 0;
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
				info = new Info(getVoucherResponse.getResponse().getResponseCode(),
						getVoucherResponse.getResponse().getResponseDesc(), rrn, stan);
				if (getVoucherResponse.getResponse().getResponseCode().equals(ResponseCodes.OK)) {

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

							// dbAmount = requestAmount.doubleValue();
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
						billingMonth = utilMethods.removeHyphen(getVoucherResponse.getResponse()
								.getOfflineBillerGetvoucher().getGetvoucher().getBillingMonth());

						dueDate = utilMethods.removeHyphen(getVoucherResponse.getResponse().getOfflineBillerGetvoucher()
								.getGetvoucher().getDueDate());
						expiryDate = getVoucherResponse.getResponse().getOfflineBillerGetvoucher().getGetvoucher()
								.getExpiryDate();
						billStatus = getVoucherResponse.getResponse().getOfflineBillerGetvoucher().getGetvoucher()
								.getBillStatus();

						oneBillNumber = getVoucherResponse.getResponse().getOfflineBillerGetvoucher().getGetvoucher()
								.getOneBillNumber();
						if (oneBillNumber == null) {
							oneBillNumber = "";
						}

//						amountPaid = String.format("%012d", Integer
//						.parseInt(getVoucherResponse.getResponse().getOfflineBillerGetvoucher().getGetvoucher().getAmount()));

						if (getVoucherResponse.getResponse().getOfflineBillerGetvoucher().getGetvoucher()
								.getBillStatus().equalsIgnoreCase(Constants.BILL_STATUS.BILL_PAID)) {

							PaymentLog paymentLog = paymentLogRepository
									.findFirstByBillerIdAndBillerNumberAndBillStatusIgnoreCaseAndActivityAndResponseCodeOrderByIDDesc(
											request.getTxnInfo().getBillerId().trim(),
											request.getTxnInfo().getBillNumber().trim(),
											Constants.BILL_STATUS.BILL_PAID, Constants.ACTIVITY.BillPayment,
											Constants.ResponseCodes.OK);
//						PaymentLog paymentLog = paymentLogRepository.findFirstByBillerNumberAndBillStatus(request.getTxnInfo().getBillNumber().trim(), Constants.BILL_STATUS.BILL_PAID);
							if (paymentLog != null) {
								datePaid = paymentLog.getTranDate();
								// billingMonth = utilMethods.formatDateString(datePaid);
								billstatus = "P";
								// amountPaid = String.valueOf(paymentLog.getAmount());
								transAuthId = paymentLog.getTranAuthId();
								amountPaid = paymentLog.getAmountPaid();
								// requestAmount = paymentLog.getAmount();

//								String duedate = getVoucherResponse.getResponse().getOfflineBillerGetvoucher()
//										.getGetvoucher().getDuedate();
//								Date parsePaidDate = utilMethods.parseDate(datePaid);
//								Date parseDueDate = utilMethods.parseDate(duedate);
//								

//								if (parsePaidDate.before(parseDueDate)) {
//									amountPaid = amountPaidInDueDate;
//								} else {
//									amountPaid = amountPaidAfterDueDate;
//								}

							} else {
								info = new Info(Constants.ResponseCodes.PAYMENT_NOT_FOUND,
										Constants.ResponseDescription.PAYMENT_NOT_FOUND, rrn, stan);
								response = new BillInquiryResponse(info, null, null);
								return response;
							}
							transactionStatus = Constants.Status.Success;

						} else if (getVoucherResponse.getResponse().getOfflineBillerGetvoucher().getGetvoucher()
								.getBillStatus().equalsIgnoreCase(Constants.BILL_STATUS.BILL_UNPAID)) {
							billstatus = "U";
							transAuthId = "";
							// PaymentLog paymentLog =
							// paymentLogRepository.findFirstByBillerNumberAndBillStatus(request.getTxnInfo().getBillNumber().trim(),Constants.BILL_STATUS.BILL_PAID);
							// amountPaid = "";
							// datePaid = paymentLog.getTranDate();
							// billingMonth= utilMethods.formatDateString(datePaid);
							datePaid = "";

							transactionStatus = Constants.Status.Pending;

						} else if (getVoucherResponse.getResponse().getOfflineBillerGetvoucher().getGetvoucher()
								.getBillStatus().equalsIgnoreCase(Constants.BILL_STATUS.BILL_EXPIRED)) {
							transactionStatus = Constants.Status.Fail;
							billstatus = "E";
						}
					}

					TxnInfo txnInfo = new TxnInfo(request.getTxnInfo().getBillerId(),
							request.getTxnInfo().getBillNumber(), name, billstatus, dueDate,
							String.valueOf(requestAmount), String.valueOf(requestAmountafterduedate), transAuthId,
							oneBillNumber);

					AdditionalInfo additionalInfo = new AdditionalInfo(request.getAdditionalInfo().getReserveField1(),
							request.getAdditionalInfo().getReserveField2(),
							request.getAdditionalInfo().getReserveField3(),
							request.getAdditionalInfo().getReserveField4(),
							request.getAdditionalInfo().getReserveField5());

//							request.getAdditionalInfo().getReserveField6(),
//							request.getAdditionalInfo().getReserveField7(),
//							request.getAdditionalInfo().getReserveField8(),
//							request.getAdditionalInfo().getReserveField9(),
//							request.getAdditionalInfo().getReserveField10());

					response = new BillInquiryResponse(info, txnInfo, additionalInfo);

				} else if (getVoucherResponse.getResponse().getResponseCode()
						.equals(Constants.ResponseCodes.NOT_FOUND)) {
					info = new Info(Constants.ResponseCodes.CONSUMER_NUMBER_NOT_EXISTS,
							Constants.ResponseDescription.CONSUMER_NUMBER_NOT_EXISTS, rrn, stan);
					response = new BillInquiryResponse(info, null, null);
					transactionStatus = Constants.Status.Fail;

				} else if (getVoucherResponse.getResponse().getResponseCode()
						.equals(Constants.ResponseCodes.OFFLINE_SERVICE_FAIL)) {
					info = new Info(Constants.ResponseCodes.SERVICE_FAIL, Constants.ResponseDescription.SERVICE_FAIL,
							rrn, stan);
					response = new BillInquiryResponse(info, null, null);
					transactionStatus = Constants.Status.Fail;

				}

				else {
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
						request.getTxnInfo().getBillerId(), request.getTxnInfo().getBillNumber(), channel, username);

			} catch (Exception ex) {
				LOG.error("{}", ex);
			}
			try {

				paymentLoggingService.paymentLog(responseDate, responseDate, rrn, stan,
						response.getInfo().getResponseCode(), response.getInfo().getResponseDesc(), cnic,
						request.getTerminalInfo().getMobile(), name, request.getTxnInfo().getBillNumber(),
						request.getTxnInfo().getBillerId(), amountPaid, new BigDecimal(amountInDueToDate),
						new BigDecimal(amountAfterDueDate), dbTransactionFees, Constants.ACTIVITY.BillInquiry, "",
						request.getTxnInfo().getBillNumber(), transactionStatus, address, transactionFees, dbTax,
						dbTotal, channel, billStatus, request.getTxnInfo().getTranDate(),
						request.getTxnInfo().getTranTime(), province, transAuthId, bankName, bankCode, branchName,
						branchCode);

			} catch (Exception ex) {
				LOG.error("{}", ex);
			}

		}
		return response;
	}

	@Override
	public BillInquiryResponse billInquiryPta(BillInquiryRequest request, HttpServletRequest httpRequestData) {

		LOG.info("PTA Bill Inquiry Request {}  ", request.toString());

		BillInquiryResponse response = null;
		PtaGetVoucherResponse getVoucherResponse = null;
		Info info = null;
		Date strDate = new Date();
		String rrn = request.getInfo().getRrn(); // utilMethods.getRRN();
		String stan = request.getInfo().getStan(); // utilMethods.getStan();
		// String transAuthId = request.getInfo().getStan(); // utilMethods.getStan();
		String transAuthId = "";
		String transactionStatus = "";

		double transactionFees = 0;
		String cnic = "";
		String mobile = "";
		String address = "";
		String depostiroName = "";
		String billStatus = "";
		double dbAmount = 0;
		double dbTax = 0;
		double dbTransactionFees = 0;
		double dbTotal = 0;
		String province = "";
		String channel = "";
		String username = "";
		String amountPaidInDueDate = "";
		String datePaid = "";
		String billingMonth = "";
		String dueDAte = "";
		String oneBillNumber = "";
		BigDecimal requestTotalAmountbdUp = null, amountPaid = null;
		BigDecimal amountInDueToDate = null;
		String amountAfterDueDate = "";
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
					Constants.ACTIVITY.BillInquiry);

			if (getVoucherResponse != null) {
				info = new Info(getVoucherResponse.getResponse().getResponseCode(),
						getVoucherResponse.getResponse().getResponseDesc(), rrn, stan);
				if (getVoucherResponse.getResponse().getResponseCode().equals(ResponseCodes.OK)) {

					String status = "";

					if (getVoucherResponse.getResponse().getPtaGetVoucher() != null) {

						DataWrapper dataWrapper = getVoucherResponse.getResponse().getPtaGetVoucher().getDataWrapper()
								.get(0);

						requestTotalAmountbdUp = BigDecimal.valueOf(Double.parseDouble(dataWrapper.getTotalAmount()))
								.setScale(2, RoundingMode.UP);
						amountInDueToDate = requestTotalAmountbdUp;
						// amountPaidInDueDate = utilMethods.formatAmount(requestTotalAmountbdUp, 12);
						amountAfterDueDate = String.valueOf(requestTotalAmountbdUp);
						// amountPaid = amountPaidInDueDate;

						depostiroName = dataWrapper.getDepositorName();
						mobile = dataWrapper.getDepositorContactNo();
						billStatus = dataWrapper.getStatus();

						oneBillNumber = dataWrapper.getOneBillNumber();
						oneBillNumber = dataWrapper.getOneBillNumber();
						if (oneBillNumber == null) {
							oneBillNumber = "";
						}

						dbTotal = requestTotalAmountbdUp.doubleValue();

						billStatus = dataWrapper.getStatus().trim().equals("0") ? Constants.BILL_STATUS.BILL_UNPAID
								: Constants.BILL_STATUS.BILL_PAID;
						if (billStatus.equalsIgnoreCase(Constants.BILL_STATUS.BILL_PAID)) {
							PaymentLog paymentLog = paymentLogRepository
									.findFirstByBillerIdAndBillerNumberAndBillStatusIgnoreCaseAndActivityAndResponseCodeOrderByIDDesc(
											request.getTxnInfo().getBillerId().trim(),
											request.getTxnInfo().getBillNumber().trim(),
											Constants.BILL_STATUS.BILL_PAID, Constants.ACTIVITY.BillPayment,
											Constants.ResponseCodes.OK);
//							PaymentLog paymentLog = paymentLogRepository.findFirstByBillerNumberAndBillStatus(
//									request.getTxnInfo().getBillNumber().trim(), Constants.BILL_STATUS.BILL_PAID);
							if (paymentLog != null) {
								datePaid = paymentLog.getTranDate();
								// billingMonth = utilMethods.formatDateString(datePaid);
								status = "P";
								transAuthId = paymentLog.getTranAuthId();
								amountPaid = paymentLog.getAmountPaid();

							} else {
								info = new Info(Constants.ResponseCodes.PAYMENT_NOT_FOUND,
										Constants.ResponseDescription.PAYMENT_NOT_FOUND, rrn, stan);
								response = new BillInquiryResponse(info, null, null);
								return response;
							}

							transactionStatus = Constants.Status.Success;
						} else if (billStatus.equalsIgnoreCase(Constants.BILL_STATUS.BILL_UNPAID)) {
							status = "U";
							transAuthId = "";
							// PaymentLog paymentLog =
							// paymentLogRepository.findFirstByBillerNumberAndBillStatus(request.getTxnInfo().getBillNumber().trim(),Constants.BILL_STATUS.BILL_PAID);
							// amountPaid = "";
							// datePaid = paymentLog.getTranDate();

							// billingMonth= utilMethods.formatDateString(datePaid);
							datePaid = "";

							transactionStatus = Constants.Status.Pending;

						} else {
							transactionStatus = Constants.Status.Fail;
							status = "B";
						}

						TxnInfo txnInfo = new TxnInfo(request.getTxnInfo().getBillerId(),
								request.getTxnInfo().getBillNumber(), depostiroName, status, dueDAte,
								String.valueOf(amountInDueToDate), amountAfterDueDate, transAuthId, oneBillNumber);

						AdditionalInfo additionalInfo = new AdditionalInfo(
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

						response = new BillInquiryResponse(info, txnInfo, additionalInfo);

					} else if (getVoucherResponse.getResponse().getResponseCode()
							.equals(Constants.ResponseCodes.NOT_FOUND)) {
						info = new Info(Constants.ResponseCodes.CONSUMER_NUMBER_NOT_EXISTS,
								Constants.ResponseDescription.CONSUMER_NUMBER_NOT_EXISTS, rrn, stan);
						response = new BillInquiryResponse(info, null, null);
						transactionStatus = Constants.Status.Fail;

					} else {
						info = new Info(Constants.ResponseCodes.UNKNOWN_ERROR,
								Constants.ResponseDescription.UNKNOWN_ERROR, rrn, stan);
						response = new BillInquiryResponse(info, null, null);
						transactionStatus = Constants.Status.Fail;
					}

				} else if (getVoucherResponse.getResponse().getResponseCode()
						.equals(Constants.ResponseCodes.NOT_FOUND)) {
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
						request.getTxnInfo().getBillerId(), request.getTxnInfo().getBillNumber(), channel, username);

			} catch (Exception ex) {
				LOG.error("{}", ex);
			}
			try {

				paymentLoggingService.paymentLog(responseDate, responseDate, rrn, stan,
						response.getInfo().getResponseCode(), response.getInfo().getResponseDesc(), cnic,
						request.getTerminalInfo().getMobile(), depostiroName, request.getTxnInfo().getBillNumber(),
						request.getTxnInfo().getBillerId(), amountPaid, amountInDueToDate,
						new BigDecimal(amountAfterDueDate), dbTransactionFees, Constants.ACTIVITY.BillInquiry, "",
						request.getTxnInfo().getBillNumber(), transactionStatus, address, transactionFees, dbTax,
						dbTotal, channel, billStatus, request.getTxnInfo().getTranDate(),
						request.getTxnInfo().getTranTime(), province, transAuthId, bankName, bankCode, branchName,
						branchCode);

			} catch (Exception ex) {
				LOG.error("{}", ex);
			}

		}
		return response;

	}

	@Override
	public BillInquiryResponse billInquiryFbr(BillInquiryRequest request, HttpServletRequest httpRequestData) {

		LOG.info("billInquiryFbr Request {} ", request.toString());

		BillInquiryResponse response = null;
		FbrGetVoucherResponse fbrGetVoucherResponse = null;
		Info info = null;
		Date strDate = new Date();
		String rrn = request.getInfo().getRrn(); // utilMethods.getRRN();
		String stan = request.getInfo().getStan(); // utilMethods.getStan();
		String transAuthId = ""; // utilMethods.getStan();
		String transactionStatus = "";

		double transactionFees = 0;
		String cnic = "";
		String mobile = "";
		String address = "";
		String name = "";
		String billStatus = "";
		BigDecimal amountInDueToDate = null;// double dbAmount = 0;
		double dbTax = 0;
		double dbTransactionFees = 0;
		double dbTotal = 0;
		String province = "";
		String channel = "";
		String username = "";
		String amountPaidInDueDate = "";
		String datePaid = "";
		String dbBillStatus = "";
		String billingMonth = "";
		String dueDate = "";
		String oneBillNumber = "";
		BigDecimal requestTotalAmountbdUp = null;
		BigDecimal requestAmount = null, amountPaid = null;
		String reserved = "";
		BigDecimal amountAfterDueDate = null;
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
				info = new Info(fbrGetVoucherResponse.getResponse().getResponseCode(),
						fbrGetVoucherResponse.getResponse().getResponseDesc(), rrn, stan);
				if (fbrGetVoucherResponse.getResponse().getResponseCode().equals(ResponseCodes.OK)) {

					BigDecimal requestAmountafterduedate = null;
					if (fbrGetVoucherResponse.getResponse().getPralFbrGetVoucher() != null) {

						String amountStr = fbrGetVoucherResponse.getResponse().getPralFbrGetVoucher()
								.getAmountWithinDueDate();
						String amountAfterDueDateStr = fbrGetVoucherResponse.getResponse().getPralFbrGetVoucher()
								.getAmountAfterDueDate();

						if ((amountStr.length() >= 1 && amountStr.length() <= 14)
								&& (amountAfterDueDateStr.length() >= 1 && amountAfterDueDateStr.length() <= 14)
								&& !amountStr.contains(".")) {

							if (!amountStr.isEmpty()) {
								// Remove leading zeros and convert to BigDecimal
								requestAmount = new BigDecimal(amountStr.replaceFirst("^\\+?0+", ""));
								requestAmount = requestAmount.divide(BigDecimal.valueOf(100));

								// Set scale to 2 and round up
								amountInDueToDate = requestAmount.setScale(2, RoundingMode.UP);

							}

							if (!amountAfterDueDateStr.isEmpty()) {
								// Remove leading zeros and convert to BigDecimal

								requestAmountafterduedate = new BigDecimal(
										amountAfterDueDateStr.replaceFirst("^\\+?0+", ""));

								requestAmountafterduedate = requestAmountafterduedate.divide(BigDecimal.valueOf(100));

								// Set scale to 2 and round up
								amountAfterDueDate = requestAmountafterduedate.setScale(2, RoundingMode.UP);

								// Continue with your logic using amountAfterDueDate...
							}

							name = fbrGetVoucherResponse.getResponse().getPralFbrGetVoucher().getConsumerDetail();
							dueDate = fbrGetVoucherResponse.getResponse().getPralFbrGetVoucher().getDueDate();
							reserved = fbrGetVoucherResponse.getResponse().getPralFbrGetVoucher().getReserved();
							if (reserved == null || reserved.isBlank() || reserved.isEmpty()) {
								reserved = request.getAdditionalInfo().getReserveField1();
							}
							billStatus = fbrGetVoucherResponse.getResponse().getPralFbrGetVoucher().getBillStatus()
									.trim().equalsIgnoreCase("U") ? Constants.BILL_STATUS.BILL_UNPAID
											: Constants.BILL_STATUS.BILL_PAID;
							dbBillStatus = billStatus;
							// dbAmount = amountInDueToDate;
							oneBillNumber = fbrGetVoucherResponse.getResponse().getPralFbrGetVoucher()
									.getOneBillNumber();
							if (oneBillNumber == null) {
								oneBillNumber = "";
							}
//						amountPaid = String.format("%012d",
//								Integer.parseInt(getVoucherResponse.getResponse().getGetvoucher().getTotal()));

							// dbTotal = requestTotalAmountbdUp.doubleValue();

							if (billStatus.equalsIgnoreCase(Constants.BILL_STATUS.BILL_PAID)) {

								PaymentLog paymentLog = paymentLogRepository
										.findFirstByBillerIdAndBillerNumberAndBillStatusIgnoreCaseAndActivityAndResponseCodeOrderByIDDesc(
												request.getTxnInfo().getBillerId().trim(),
												request.getTxnInfo().getBillNumber().trim(),
												Constants.BILL_STATUS.BILL_PAID, Constants.ACTIVITY.BillPayment,
												Constants.ResponseCodes.OK);
//							PaymentLog paymentLog = paymentLogRepository.findFirstByBillerNumberAndBillStatus(
//									request.getTxnInfo().getBillNumber().trim(), Constants.BILL_STATUS.BILL_PAID);
								if (paymentLog != null) {
									// datePaid = paymentLog.getTranDate();
									// billingMonth = utilMethods.formatDateString(datePaid);
									billStatus = "P";
									transAuthId = paymentLog.getTranAuthId();
									amountPaid = paymentLog.getAmountPaid();

								} else {
									info = new Info(Constants.ResponseCodes.PAYMENT_NOT_FOUND,
											Constants.ResponseDescription.PAYMENT_NOT_FOUND, rrn, stan);
									response = new BillInquiryResponse(info, null, null);
									return response;
								}

								transactionStatus = Constants.Status.Success;
							} else if (billStatus.equalsIgnoreCase(Constants.BILL_STATUS.BILL_UNPAID)) {
								billStatus = "U";
								transAuthId = "";
								// PaymentLog paymentLog =
								// paymentLogRepository.findFirstByBillerNumberAndBillStatus(request.getTxnInfo().getBillNumber().trim(),Constants.BILL_STATUS.BILL_PAID);

								// datePaid = paymentLog.getTranDate();

								// billingMonth= utilMethods.formatDateString(datePaid);
								datePaid = "";

								transactionStatus = Constants.Status.Pending;

							} else if (billStatus.equalsIgnoreCase(Constants.BILL_STATUS.BILL_BLOCK)
									|| fbrGetVoucherResponse.getResponse().getPralFbrGetVoucher().getBillStatus()
											.equalsIgnoreCase(Constants.BILL_STATUS.BILL_BLOCK.substring(0))) {
								transactionStatus = Constants.Status.Fail;
								billStatus = "B";
							}
							// hardcoded date
							// dueDAte = utilMethods.getDueDate("20220825");

						} else {
							info = new Info(Constants.ResponseCodes.TRANSACTION_CAN_NOT_BE_PROCESSED,
									Constants.ResponseDescription.TRANSACTION_CAN_NOT_BE_PROCESSED, rrn, stan);
							response = new BillInquiryResponse(info, null, null);
							transactionStatus = Constants.Status.Pending;
							return response;

						}
					}

					TxnInfo txnInfo = new TxnInfo(request.getTxnInfo().getBillerId(),
							request.getTxnInfo().getBillNumber(), name, billStatus, dueDate,
							String.valueOf(amountInDueToDate), String.valueOf(amountAfterDueDate), transAuthId,
							oneBillNumber);

					AdditionalInfo additionalInfo = new AdditionalInfo(reserved,

							request.getAdditionalInfo().getReserveField2(),
							request.getAdditionalInfo().getReserveField3(),
							request.getAdditionalInfo().getReserveField4(),
							request.getAdditionalInfo().getReserveField5(),
							request.getAdditionalInfo().getReserveField6(),
							request.getAdditionalInfo().getReserveField7(),
							request.getAdditionalInfo().getReserveField8(),
							request.getAdditionalInfo().getReserveField9(),
							request.getAdditionalInfo().getReserveField10());

					response = new BillInquiryResponse(info, txnInfo, additionalInfo);

				} else if (fbrGetVoucherResponse.getResponse().getResponseCode()
						.equals(Constants.ResponseCodes.NOT_FOUND)) {
					info = new Info(Constants.ResponseCodes.CONSUMER_NUMBER_NOT_EXISTS,
							Constants.ResponseDescription.CONSUMER_NUMBER_NOT_EXISTS, rrn, stan);
					response = new BillInquiryResponse(info, null, null);
					transactionStatus = Constants.Status.Fail;

				} else if (fbrGetVoucherResponse.getResponse().getResponseCode()
						.equals(Constants.ResponseCodes.INVALID_DATA)) {
					// 04 Invalid Data
					info = new Info(Constants.ResponseCodes.INVALID_DATA, Constants.ResponseDescription.INVALID_DATA,
							rrn, stan);
					response = new BillInquiryResponse(info, null, null);
					transactionStatus = Constants.Status.Fail;

				} else if (fbrGetVoucherResponse.getResponse().getResponseCode()
						.equals(Constants.ResponseCodes.SERVICE_FAIL)) {

					// 05 Processing Failed
					info = new Info(Constants.ResponseCodes.SERVICE_FAIL, Constants.ResponseDescription.SERVICE_FAIL,
							rrn, stan);
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
						request.getTxnInfo().getBillerId(), request.getTxnInfo().getBillNumber(), channel, username);

			} catch (Exception ex) {
				LOG.error("{}", ex);
			}
			try {

				paymentLoggingService.paymentLog(responseDate, responseDate, rrn, stan,
						response.getInfo().getResponseCode(), response.getInfo().getResponseDesc(), cnic,
						request.getTerminalInfo().getMobile(), name, request.getTxnInfo().getBillNumber(),
						request.getTxnInfo().getBillerId(), amountPaid, amountInDueToDate, amountInDueToDate,
						dbTransactionFees, Constants.ACTIVITY.BillInquiry, "", request.getTxnInfo().getBillNumber(),
						transactionStatus, address, transactionFees, dbTax, dbTotal, channel, dbBillStatus,
						request.getTxnInfo().getTranDate(), request.getTxnInfo().getTranTime(), province, transAuthId,
						bankName, bankCode, branchName, branchCode);

			} catch (Exception ex) {
				LOG.error("{}", ex);
			}
		}
		return response;
	}

	@Override
	public BillInquiryResponse billInquiryAiou(BillInquiryRequest request, HttpServletRequest httpRequestData) {
		LOG.info("billInquiryAiou Request {} ", request.toString());

		BillInquiryResponse response = null;
		AiouGetVoucherResponse aiouGetVoucherResponse = null;
		Info info = null;
		Date strDate = new Date();
		String rrn = request.getInfo().getRrn(); // utilMethods.getRRN();
		String stan = request.getInfo().getStan(); // utilMethods.getStan();
		String transAuthId = ""; // utilMethods.getStan();
		String transactionStatus = "";

		double transactionFees = 0;
		String cnic = "";
		String contactNumber = "";
		String address = "";
		String name = "";
		String fatherName = "";
		String billStatus = "";

		BigDecimal amountInDueToDate = null;// double dbAmount = 0;
		double dbTax = 0;
		double dbTransactionFees = 0;
		double dbTotal = 0;
		String province = "";
		String channel = "";
		String username = "";
		BigDecimal amountPaid = null;
		String datePaid = "";
		String dbBillStatus = "";
		String billingMonth = "";
		String dueDate = "";
		String oneBillNumber = "";
		BigDecimal requestTotalAmountbdUp = null;
		BigDecimal requestAmount = null;
		String reserved = "";
		BigDecimal amountAfterDueDate = null;
		String semester = "";
		String Programme;
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

			aiouGetVoucherResponse = serviceCaller.get(inquiryParams, AiouGetVoucherResponse.class, rrn,
					Constants.ACTIVITY.BillInquiry);

			if (aiouGetVoucherResponse != null) {
				info = new Info(aiouGetVoucherResponse.getResponse().getResponseCode(),
						aiouGetVoucherResponse.getResponse().getResponseDesc(), rrn, stan);
				if (aiouGetVoucherResponse.getResponse().getResponseCode().equals(ResponseCodes.OK)) {

					name = String.format("%-30s", aiouGetVoucherResponse.getResponse().getAiouGetVoucher()
							.getResponseBillInquiry().getName());
					fatherName = String.format("%-30s", aiouGetVoucherResponse.getResponse().getAiouGetVoucher()
							.getResponseBillInquiry().getFatherName());
					cnic = String.format("%-30s", aiouGetVoucherResponse.getResponse().getAiouGetVoucher()
							.getResponseBillInquiry().getCnic());
					contactNumber = String.format("%-30s", aiouGetVoucherResponse.getResponse().getAiouGetVoucher()
							.getResponseBillInquiry().getContactNumber());
					dueDate = aiouGetVoucherResponse.getResponse().getAiouGetVoucher().getResponseBillInquiry()
							.getDueDate();
					semester = aiouGetVoucherResponse.getResponse().getAiouGetVoucher().getResponseBillInquiry()
							.getSemester();
					Programme = aiouGetVoucherResponse.getResponse().getAiouGetVoucher().getResponseBillInquiry()
							.getProgramme();

					BigDecimal requestAmountafterduedate = null;
					if (aiouGetVoucherResponse.getResponse().getAiouGetVoucher() != null) {

						if (semester == null || semester.isBlank() || semester.isEmpty()) {
							semester = request.getAdditionalInfo().getReserveField1();
						}
						if (Programme == null || Programme.isBlank() || Programme.isEmpty()) {
							Programme = request.getAdditionalInfo().getReserveField2();
						}

						billStatus = aiouGetVoucherResponse.getResponse().getAiouGetVoucher().getResponseBillInquiry()
								.getBillStatus().trim().equalsIgnoreCase("U") ? Constants.BILL_STATUS.BILL_UNPAID
										: Constants.BILL_STATUS.BILL_PAID;
						dbBillStatus = billStatus;
						// dbAmount = amountInDueToDate;
						oneBillNumber = aiouGetVoucherResponse.getResponse().getAiouGetVoucher()
								.getResponseBillInquiry().getOneBillNumber();
						if (oneBillNumber == null) {
							oneBillNumber = "";
						}
//						amountPaid = String.format("%012d",
//								Integer.parseInt(getVoucherResponse.getResponse().getGetvoucher().getTotal()));

						// dbTotal = requestTotalAmountbdUp.doubleValue();

						if (billStatus.equalsIgnoreCase(Constants.BILL_STATUS.BILL_PAID)) {

							PaymentLog paymentLog = paymentLogRepository
									.findFirstByBillerIdAndBillerNumberAndBillStatusIgnoreCaseAndActivityAndResponseCodeOrderByIDDesc(
											request.getTxnInfo().getBillerId().trim(),
											request.getTxnInfo().getBillNumber().trim(),
											Constants.BILL_STATUS.BILL_PAID, Constants.ACTIVITY.BillPayment,
											Constants.ResponseCodes.OK);
//							PaymentLog paymentLog = paymentLogRepository.findFirstByBillerNumberAndBillStatus(
//									request.getTxnInfo().getBillNumber().trim(), Constants.BILL_STATUS.BILL_PAID);
							if (paymentLog != null) {
								// datePaid = paymentLog.getTranDate();
								// billingMonth = utilMethods.formatDateString(datePaid);
								billStatus = "P";
								transAuthId = paymentLog.getTranAuthId();
								amountInDueToDate = paymentLog.getAmountwithinduedate();
								amountAfterDueDate = paymentLog.getAmountafterduedate();
								amountPaid = paymentLog.getAmountPaid();

							} else {
								info = new Info(Constants.ResponseCodes.PAYMENT_NOT_FOUND,
										Constants.ResponseDescription.PAYMENT_NOT_FOUND, rrn, stan);
								response = new BillInquiryResponse(info, null, null);
								return response;
							}

							transactionStatus = Constants.Status.Success;
						} else if (billStatus.equalsIgnoreCase(Constants.BILL_STATUS.BILL_UNPAID)) {

							String amountStr = aiouGetVoucherResponse.getResponse().getAiouGetVoucher()
									.getResponseBillInquiry().getAmountWithinDueDate();
							String amountAfterDueDateStr = aiouGetVoucherResponse.getResponse().getAiouGetVoucher()
									.getResponseBillInquiry().getAmountAfterDueDate();

							if (!amountStr.isEmpty()) {
								// Remove leading zeros and convert to BigDecimal
								requestAmount = new BigDecimal(amountStr.replaceFirst("^\\+?0+", ""));
								requestAmount = requestAmount.divide(BigDecimal.valueOf(100));

								// Set scale to 2 and round up
								amountInDueToDate = requestAmount.setScale(2, RoundingMode.UP);

							}

							if (!amountAfterDueDateStr.isEmpty()) {
								// Remove leading zeros and convert to BigDecimal

								requestAmountafterduedate = new BigDecimal(
										amountAfterDueDateStr.replaceFirst("^\\+?0+", ""));

								requestAmountafterduedate = requestAmountafterduedate.divide(BigDecimal.valueOf(100));

								// Set scale to 2 and round up
								amountAfterDueDate = requestAmountafterduedate.setScale(2, RoundingMode.UP);

								// Continue with your logic using amountAfterDueDate...
							}

							billStatus = "U";
							transAuthId = "";
							// PaymentLog paymentLog =
							// paymentLogRepository.findFirstByBillerNumberAndBillStatus(request.getTxnInfo().getBillNumber().trim(),Constants.BILL_STATUS.BILL_PAID);

							// datePaid = paymentLog.getTranDate();

							// billingMonth= utilMethods.formatDateString(datePaid);
							datePaid = "";

							transactionStatus = Constants.Status.Pending;

						} else if (billStatus.equalsIgnoreCase(Constants.BILL_STATUS.BILL_BLOCK)
								|| aiouGetVoucherResponse.getResponse().getAiouGetVoucher().getResponseBillInquiry()
										.getBillStatus()
										.equalsIgnoreCase(Constants.BILL_STATUS.BILL_BLOCK.substring(0, 1))) {
							transactionStatus = Constants.Status.Fail;
							billStatus = "B";
						}
						// hardcoded date
						// dueDAte = utilMethods.getDueDate("20220825");

					}

					TxnInfo txnInfo = new TxnInfo(request.getTxnInfo().getBillerId(),
							request.getTxnInfo().getBillNumber(), name, billStatus, dueDate,
							String.valueOf(amountInDueToDate), String.valueOf(amountAfterDueDate), transAuthId,
							oneBillNumber);

					AdditionalInfo additionalInfo = new AdditionalInfo(semester,

							Programme, name, fatherName, cnic, contactNumber,
							request.getAdditionalInfo().getReserveField7(),
							request.getAdditionalInfo().getReserveField8(),
							request.getAdditionalInfo().getReserveField9(),
							request.getAdditionalInfo().getReserveField10());

					response = new BillInquiryResponse(info, txnInfo, additionalInfo);

				} else if (aiouGetVoucherResponse.getResponse().getResponseCode()
						.equals(Constants.ResponseCodes.NOT_FOUND)) {
					info = new Info(Constants.ResponseCodes.CONSUMER_NUMBER_NOT_EXISTS,
							Constants.ResponseDescription.CONSUMER_NUMBER_NOT_EXISTS, rrn, stan);
					response = new BillInquiryResponse(info, null, null);
					transactionStatus = Constants.Status.Fail;

				} else if (aiouGetVoucherResponse.getResponse().getResponseCode()
						.equals(Constants.ResponseCodes.INVALID_DATA)) {
					// 04 Invalid Data
					info = new Info(Constants.ResponseCodes.INVALID_DATA, Constants.ResponseDescription.INVALID_DATA,
							rrn, stan);
					response = new BillInquiryResponse(info, null, null);
					transactionStatus = Constants.Status.Fail;

				} else if (aiouGetVoucherResponse.getResponse().getResponseCode()
						.equals(Constants.ResponseCodes.SERVICE_FAIL)) {

					// 05 Processing Failed
					info = new Info(Constants.ResponseCodes.SERVICE_FAIL, Constants.ResponseDescription.SERVICE_FAIL,
							rrn, stan);
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
						request.getTxnInfo().getBillerId(), request.getTxnInfo().getBillNumber(), channel, username);

			} catch (Exception ex) {
				LOG.error("{}", ex);
			}
			try {

				paymentLoggingService.paymentLog(responseDate, responseDate, rrn, stan,
						response.getInfo().getResponseCode(), response.getInfo().getResponseDesc(), cnic,
						request.getTerminalInfo().getMobile(), name, request.getTxnInfo().getBillNumber(),
						request.getTxnInfo().getBillerId(), amountPaid, amountInDueToDate, amountAfterDueDate,
						dbTransactionFees, Constants.ACTIVITY.BillInquiry, "", request.getTxnInfo().getBillNumber(),
						transactionStatus, address, transactionFees, dbTax, dbTotal, channel, dbBillStatus,
						request.getTxnInfo().getTranDate(), request.getTxnInfo().getTranTime(), province, transAuthId,
						bankName, bankCode, branchName, branchCode);

			} catch (Exception ex) {
				LOG.error("{}", ex);
			}
		}
		return response;

	}

	public BillInquiryResponse billInquiryPITHAM(BillInquiryRequest request, HttpServletRequest httpRequestData) {

		LOG.info("PITHAM Bill Inquiry Request {} ", request.toString());

		BillInquiryResponse response = null;
		PithamGetVoucherResponse pithamgetVoucherResponse = null;
		Info info = null;
		Date strDate = new Date();
		String rrn = request.getInfo().getRrn(); // utilMethods.getRRN();
		String rrnReq = request.getInfo().getRrn().substring(0, 10); // utilMethods.getRRN();

		String stan = request.getInfo().getStan(); // utilMethods.getStan();
		String transAuthId = ""; // utilMethods.getStan();
		String transactionStatus = "";
		String billstatus = "";
		String billStatus = "";
		String username = "";
		String channel = "";
		BigDecimal amountInDueToDate = null;
		BigDecimal amountAfterDate = null;
		BigDecimal amountPaid;
		String billerName = "";
		String dueDate = "";
		String billingMonth = "";

		String amountWithInDueDateRes = "";
		BigDecimal amountDueDateRes = null;
		String amountAfterDueDateRes;
		BigDecimal amounAfterDateRes = null;
		String billerNameRes = "";
		String dueDateRes = "";
		String billingMonthRes = "";
		String billStatusCodeRes = "";
		String billStatusDescRes = "";
		String bankName = "", bankCode = "", branchName = "", branchCode = "";

		Date requestedDate = new Date();

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
			inquiryParams.add(Constants.MPAY_REQUEST_METHODS.PITHAM_BILL_INQUIRY);
			inquiryParams.add(request.getAdditionalInfo().getReserveField1().trim());
			inquiryParams.add(request.getTxnInfo().getBillNumber().trim());
			inquiryParams.add(rrnReq);

			pithamgetVoucherResponse = serviceCaller.get(inquiryParams, PithamGetVoucherResponse.class, rrn,
					Constants.ACTIVITY.BillInquiry);

			if (pithamgetVoucherResponse != null) {

				if (pithamgetVoucherResponse.getResponseCode()
						.equalsIgnoreCase(Constants.ResponseCodes.CONSUMER_NUMBER_NOT_EXISTS)) {

					info = new Info(Constants.ResponseCodes.CONSUMER_NUMBER_NOT_EXISTS,
							Constants.ResponseDescription.CONSUMER_NUMBER_NOT_EXISTS, rrn, stan);

					TxnInfo txnInfo = new TxnInfo(request.getTxnInfo().getBillerId(),
							request.getTxnInfo().getBillNumber(), billerName, billstatus, dueDate,
							String.valueOf(amountInDueToDate), String.valueOf(amountAfterDate), transAuthId, "");

					AdditionalInfo additionalInfo = new AdditionalInfo(request.getAdditionalInfo().getReserveField1(),
							request.getAdditionalInfo().getReserveField2(),
							request.getAdditionalInfo().getReserveField3(),
							request.getAdditionalInfo().getReserveField4(),
							request.getAdditionalInfo().getReserveField5(),
							request.getAdditionalInfo().getReserveField6(),
							request.getAdditionalInfo().getReserveField7(),
							request.getAdditionalInfo().getReserveField8(),
							request.getAdditionalInfo().getReserveField9(),
							request.getAdditionalInfo().getReserveField10());

					response = new BillInquiryResponse(info, txnInfo, additionalInfo);

					return response;
				}

				else if (pithamgetVoucherResponse.getResponseCode()
						.equalsIgnoreCase(Constants.ResponseCodes.UNKNOWN_ERROR)) {

					info = new Info(pithamgetVoucherResponse.getResponseCode(),
							pithamgetVoucherResponse.getResponseDesc(), rrn, stan);

					TxnInfo txnInfo = new TxnInfo(request.getTxnInfo().getBillerId(),
							request.getTxnInfo().getBillNumber(), billerName, billstatus, dueDate,
							String.valueOf(amountInDueToDate), String.valueOf(amountAfterDate), transAuthId, "");

					AdditionalInfo additionalInfo = new AdditionalInfo(request.getAdditionalInfo().getReserveField1(),
							request.getAdditionalInfo().getReserveField2(),
							request.getAdditionalInfo().getReserveField3(),
							request.getAdditionalInfo().getReserveField4(),
							request.getAdditionalInfo().getReserveField5(),
							request.getAdditionalInfo().getReserveField6(),
							request.getAdditionalInfo().getReserveField7(),
							request.getAdditionalInfo().getReserveField8(),
							request.getAdditionalInfo().getReserveField9(),
							request.getAdditionalInfo().getReserveField10());

					response = new BillInquiryResponse(info, txnInfo, additionalInfo);

					return response;
				}

				else if (pithamgetVoucherResponse.getResponseCode()
						.equalsIgnoreCase(Constants.ResponseCodes.UNAUTHORISED_USER)) {

					info = new Info(pithamgetVoucherResponse.getResponseCode(),
							pithamgetVoucherResponse.getResponseDesc(), rrn, stan);

					TxnInfo txnInfo = new TxnInfo(request.getTxnInfo().getBillerId(),
							request.getTxnInfo().getBillNumber(), billerName, billstatus, dueDate,
							String.valueOf(amountInDueToDate), String.valueOf(amountAfterDate), transAuthId, "");

					AdditionalInfo additionalInfo = new AdditionalInfo(request.getAdditionalInfo().getReserveField1(),
							request.getAdditionalInfo().getReserveField2(),
							request.getAdditionalInfo().getReserveField3(),
							request.getAdditionalInfo().getReserveField4(),
							request.getAdditionalInfo().getReserveField5(),
							request.getAdditionalInfo().getReserveField6(),
							request.getAdditionalInfo().getReserveField7(),
							request.getAdditionalInfo().getReserveField8(),
							request.getAdditionalInfo().getReserveField9(),
							request.getAdditionalInfo().getReserveField10());

					response = new BillInquiryResponse(info, txnInfo, additionalInfo);

					return response;
				}

				else if (pithamgetVoucherResponse.getResponseCode().equalsIgnoreCase(ResponseCodes.BILL_ALREADY_PAID)) {
					PaymentLog paymentLog = paymentLogRepository
							.findFirstByBillerIdAndBillerNumberAndBillStatusIgnoreCaseAndActivityAndResponseCodeOrderByIDDesc(
									request.getTxnInfo().getBillerId().trim(),
									request.getTxnInfo().getBillNumber().trim(), Constants.BILL_STATUS.BILL_PAID,
									Constants.ACTIVITY.BillPayment, Constants.ResponseCodes.OK);

					if (paymentLog != null) {

						billstatus = "P";
						billStatus = "Paid";

						transAuthId = paymentLog.getTranAuthId();
						amountInDueToDate = paymentLog.getAmountwithinduedate();
						amountAfterDate = paymentLog.getAmountafterduedate();
						billerName = paymentLog.getName();
						amountPaid = paymentLog.getAmountPaid();
						dueDate = paymentLog.getDuedate();
						billingMonth = paymentLog.getBillingMonth();

					} else {

						info = new Info(Constants.ResponseCodes.PAYMENT_NOT_FOUND,
								Constants.ResponseDescription.PAYMENT_NOT_FOUND, rrn, stan);

						TxnInfo txnInfo = new TxnInfo(request.getTxnInfo().getBillerId(),
								request.getTxnInfo().getBillNumber(), billerName, billstatus, dueDate,
								String.valueOf(amountInDueToDate), String.valueOf(amountAfterDate), transAuthId, "");

						AdditionalInfo additionalInfo = new AdditionalInfo(
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

						response = new BillInquiryResponse(info, txnInfo, additionalInfo);

						return response;
					}

					info = new Info(Constants.ResponseCodes.OK, Constants.ResponseDescription.OPERATION_SUCCESSFULL,
							rrn, stan);

					TxnInfo txnInfo = new TxnInfo(request.getTxnInfo().getBillerId(),
							request.getTxnInfo().getBillNumber(), billerName, billstatus, dueDate,
							String.valueOf(amountInDueToDate), String.valueOf(amountAfterDate), transAuthId, "");

					AdditionalInfo additionalInfo = new AdditionalInfo(request.getAdditionalInfo().getReserveField1(),
							request.getAdditionalInfo().getReserveField2(),
							request.getAdditionalInfo().getReserveField3(),
							request.getAdditionalInfo().getReserveField4(),
							request.getAdditionalInfo().getReserveField5(),
							request.getAdditionalInfo().getReserveField6(),
							request.getAdditionalInfo().getReserveField7(),
							request.getAdditionalInfo().getReserveField8(),
							request.getAdditionalInfo().getReserveField9(),
							request.getAdditionalInfo().getReserveField10());

					response = new BillInquiryResponse(info, txnInfo, additionalInfo);

					transactionStatus = Constants.Status.Success;

					return response;
				}

				billerNameRes = pithamgetVoucherResponse.getPithmGetVoucher().getGetInquiryResult().getStudentName();
				billingMonthRes = pithamgetVoucherResponse.getPithmGetVoucher().getGetInquiryResult().getBillingMonth();
				billStatusCodeRes = pithamgetVoucherResponse.getPithmGetVoucher().getGetInquiryResult().getStatus();
				billStatusDescRes = pithamgetVoucherResponse.getPithmGetVoucher().getGetInquiryResult().getStatusDesc();
				dueDateRes = pithamgetVoucherResponse.getPithmGetVoucher().getGetInquiryResult().getDueDate();
				amountWithInDueDateRes = pithamgetVoucherResponse.getPithmGetVoucher().getGetInquiryResult()
						.getAmountWidDate();
				amountAfterDueDateRes = pithamgetVoucherResponse.getPithmGetVoucher().getGetInquiryResult()
						.getAmountAdDate();

				if (amountWithInDueDateRes == null) {

					amountDueDateRes = null;
				}

				else {

					amountDueDateRes = new BigDecimal(amountWithInDueDateRes);
				}

				if (amountAfterDueDateRes == null) {

					amounAfterDateRes = null;
				}

				else {

					amounAfterDateRes = new BigDecimal(amountAfterDueDateRes);
				}

				if (billStatusDescRes.equalsIgnoreCase(Constants.BILL_STATUS.BILL_UNPAID)) {

					billstatus = "U";
					billStatus = "Unpaid";

					transactionStatus = Constants.Status.Pending;

				}

				else if (billStatusDescRes.equalsIgnoreCase(Constants.BILL_STATUS.BILL_EXPIRED)) {

					billstatus = "E";
					billStatus = "Expired";

					transactionStatus = Constants.Status.Expired;

				}

				else if (billStatusDescRes.equalsIgnoreCase(Constants.BILL_STATUS.BILL_BLOCK)) {

					billstatus = "B";
					billStatus = "Block";

					transactionStatus = Constants.Status.Block;

				}

				if (pithamgetVoucherResponse.getResponseCode().equalsIgnoreCase(ResponseCodes.OK)) {

					info = new Info(pithamgetVoucherResponse.getResponseCode(),
							Constants.ResponseDescription.OPERATION_SUCCESSFULL, rrn, stan);

					TxnInfo txnInfo = new TxnInfo(request.getTxnInfo().getBillerId(),
							request.getTxnInfo().getBillNumber(),
							pithamgetVoucherResponse.getPithmGetVoucher().getGetInquiryResult().getStudentName(),
							billstatus,
							pithamgetVoucherResponse.getPithmGetVoucher().getGetInquiryResult().getDueDate(),
							pithamgetVoucherResponse.getPithmGetVoucher().getGetInquiryResult().getAmountWidDate(),
							pithamgetVoucherResponse.getPithmGetVoucher().getGetInquiryResult().getAmountAdDate(),
							transAuthId, "");

					AdditionalInfo additionalInfo = new AdditionalInfo(request.getAdditionalInfo().getReserveField1(),
							request.getAdditionalInfo().getReserveField2(),
							request.getAdditionalInfo().getReserveField3(),
							request.getAdditionalInfo().getReserveField4(),
							request.getAdditionalInfo().getReserveField5(),
							request.getAdditionalInfo().getReserveField6(),
							request.getAdditionalInfo().getReserveField7(),
							request.getAdditionalInfo().getReserveField8(),
							request.getAdditionalInfo().getReserveField9(),
							request.getAdditionalInfo().getReserveField10());

					response = new BillInquiryResponse(info, txnInfo, additionalInfo);

				}

				else {

					info = new Info(pithamgetVoucherResponse.getResponseCode(),
							pithamgetVoucherResponse.getResponseDesc(), rrn, stan);

					TxnInfo txnInfo = new TxnInfo(request.getTxnInfo().getBillerId(),
							request.getTxnInfo().getBillNumber(), billerName, billstatus, dueDate,
							String.valueOf(amountInDueToDate), String.valueOf(amountAfterDate), transAuthId, "");

					AdditionalInfo additionalInfo = new AdditionalInfo(request.getAdditionalInfo().getReserveField1(),
							request.getAdditionalInfo().getReserveField2(),
							request.getAdditionalInfo().getReserveField3(),
							request.getAdditionalInfo().getReserveField4(),
							request.getAdditionalInfo().getReserveField5(),
							request.getAdditionalInfo().getReserveField6(),
							request.getAdditionalInfo().getReserveField7(),
							request.getAdditionalInfo().getReserveField8(),
							request.getAdditionalInfo().getReserveField9(),
							request.getAdditionalInfo().getReserveField10());

					response = new BillInquiryResponse(info, txnInfo, additionalInfo);

					return response;
				}
			}

			else {

				info = new Info(Constants.ResponseCodes.SERVICE_FAIL, Constants.ResponseDescription.SERVICE_FAIL, rrn,
						stan);

				TxnInfo txnInfo = new TxnInfo(request.getTxnInfo().getBillerId(), request.getTxnInfo().getBillNumber(),
						billerName, billstatus, dueDate, String.valueOf(amountInDueToDate),
						String.valueOf(amountAfterDate), transAuthId, "");

				AdditionalInfo additionalInfo = new AdditionalInfo(request.getAdditionalInfo().getReserveField1(),
						request.getAdditionalInfo().getReserveField2(), request.getAdditionalInfo().getReserveField3(),
						request.getAdditionalInfo().getReserveField4(), request.getAdditionalInfo().getReserveField5(),
						request.getAdditionalInfo().getReserveField6(), request.getAdditionalInfo().getReserveField7(),
						request.getAdditionalInfo().getReserveField8(), request.getAdditionalInfo().getReserveField9(),
						request.getAdditionalInfo().getReserveField10());

				response = new BillInquiryResponse(info, txnInfo, additionalInfo);

				return response;
			}

		}

		catch (Exception ex) {

			LOG.error("Exception {}", ex);

		}

		finally {

			try {

				String requestAsString = objectMapper.writeValueAsString(request);
				String responseAsString = objectMapper.writeValueAsString(response);

				auditLoggingService.auditLog(Constants.ACTIVITY.BillInquiry, response.getInfo().getResponseCode(),
						response.getInfo().getResponseDesc(), requestAsString, responseAsString, requestedDate,
						new Date(), rrn, request.getTxnInfo().getBillerId(), request.getTxnInfo().getBillNumber(),
						channel, username);

			} catch (Exception ex) {
				LOG.error("{}", ex);
			}

			try {

				paymentLoggingService.paymentLog(requestedDate, new Date(), rrn, stan,
						response.getInfo().getResponseCode(), response.getInfo().getResponseDesc(),
						billerNameRes == null ? billerName : billerNameRes, request.getTxnInfo().getBillNumber(),
						request.getTxnInfo().getBillerId(),
						amountDueDateRes == null ? amountInDueToDate : amountDueDateRes,
						amounAfterDateRes == null ? amountAfterDate : amounAfterDateRes, Constants.ACTIVITY.BillInquiry,
						transactionStatus, channel, billStatus, request.getTxnInfo().getTranDate(),
						request.getTxnInfo().getTranTime(), transAuthId, null,
						dueDateRes == null ? dueDate : dueDateRes,
						billingMonthRes == null ? billingMonth : billingMonthRes, "", bankName, bankCode, branchName,
						branchCode);

			} catch (Exception ex) {
				LOG.error("{}", ex);
			}

			LOG.info("----- Bill Inquiry Method End -----");

		}

		return response;

	}

	/////////////////////////////////////

	public BillInquiryResponse billInquiryTHARDEEP(BillInquiryRequest request, HttpServletRequest httpRequestData) {

		LOG.info("PITHAM Bill Inquiry Request {} ", request.toString());

		BillInquiryResponse response = null;
		ThardeepGetVoucherResponse thardeepgetVoucherResponse = null;
		Info info = null;
		String rrn = request.getInfo().getRrn(); // utilMethods.getRRN();
		String stan = request.getInfo().getStan(); // utilMethods.getStan();
		String transactionStatus = "";
		String billStatus = "";
		String username = "";
		String channel = "";
		BigDecimal amountInDueToDate = null;
		BigDecimal amountAfterDate = null;
		BigDecimal amountPaid;
		String billerName = "";
		String dueDate = "";
		String billingMonth = "";
		String transAuthId = "";

		String amountWithInDueDateRes = "";
		BigDecimal amountDueDateRes = null;
		String amountAfterDueDateRes;
		BigDecimal amounAfterDateRes = null;
		BigDecimal requestAmount = null;

		String billerNameRes = "", billInquiryCode = "", billInquiryDesc = "", dueDateRes = "", billingMonthRes = "",
				billStatusRes = "", tranAuthIdRes = "", amountRes = "", cnicRes = "";

		String bankName = "", bankCode = "", branchName = "", branchCode = "";

		Date requestedDate = new Date();
		String formattedDueDate = "";

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
			inquiryParams.add(Constants.MPAY_REQUEST_METHODS.THARDEEP_BILL_INQUIRY);
			inquiryParams.add(request.getTxnInfo().getBillNumber().trim());
			inquiryParams.add(rrn);
			inquiryParams.add(stan);

			thardeepgetVoucherResponse = serviceCaller.get(inquiryParams, ThardeepGetVoucherResponse.class, rrn,
					Constants.ACTIVITY.BillInquiry);

			if (thardeepgetVoucherResponse != null) {

			
				billerNameRes = thardeepgetVoucherResponse.getResponse().getThardeepGetVoucher().getConsumerName();
				billingMonthRes = thardeepgetVoucherResponse.getResponse().getThardeepGetVoucher().getBillingMonth();
				billInquiryCode = thardeepgetVoucherResponse.getResponse().getThardeepGetVoucher().getResponseCode();
				billInquiryDesc = thardeepgetVoucherResponse.getResponse().getThardeepGetVoucher().getStatusResponse();
				billStatusRes = thardeepgetVoucherResponse.getResponse().getThardeepGetVoucher().getBillStatus();
				dueDateRes = thardeepgetVoucherResponse.getResponse().getThardeepGetVoucher().getDueDate();

				formattedDueDate = utilMethods.formatDueDate(dueDateRes);

				tranAuthIdRes = thardeepgetVoucherResponse.getResponse().getThardeepGetVoucher().getAuthId();
				amountRes = thardeepgetVoucherResponse.getResponse().getThardeepGetVoucher().getAmount();
				
				requestAmount = new BigDecimal(amountRes.replaceFirst("^\\+?0+", ""));
				amountInDueToDate = requestAmount.setScale(2, RoundingMode.UP);

				cnicRes = thardeepgetVoucherResponse.getResponse().getThardeepGetVoucher().getCnicNo();


				
				if (thardeepgetVoucherResponse.getResponse().getResponseCode()
						.equalsIgnoreCase(Constants.ResponseCodes.CONSUMER_NUMBER_NOT_EXISTS)) {

					info = new Info(Constants.ResponseCodes.CONSUMER_NUMBER_NOT_EXISTS,
							Constants.ResponseDescription.CONSUMER_NUMBER_NOT_EXISTS, rrn, stan);

					TxnInfo txnInfo = new TxnInfo(request.getTxnInfo().getBillerId(),
							request.getTxnInfo().getBillNumber(), billerNameRes, billStatusRes, formattedDueDate, "",
							"", tranAuthIdRes, "");

					AdditionalInfo additionalInfo = new AdditionalInfo(request.getAdditionalInfo().getReserveField1(),
							request.getAdditionalInfo().getReserveField2(),
							request.getAdditionalInfo().getReserveField3(),
							request.getAdditionalInfo().getReserveField4(),
							request.getAdditionalInfo().getReserveField5(),
							request.getAdditionalInfo().getReserveField6(),
							request.getAdditionalInfo().getReserveField7(),
							request.getAdditionalInfo().getReserveField8(),
							request.getAdditionalInfo().getReserveField9(),
							request.getAdditionalInfo().getReserveField10());

					response = new BillInquiryResponse(info, txnInfo, additionalInfo);

					return response;
				}

				
				if (thardeepgetVoucherResponse.getResponse().getResponseCode()
						.equalsIgnoreCase(Constants.ResponseCodes.CONSUMER_NUMBER_BLOCK)) {

					info = new Info(Constants.ResponseCodes.CONSUMER_NUMBER_BLOCK,
							Constants.ResponseDescription.CONSUMER_NUMBER_BLOCK, rrn, stan);

					TxnInfo txnInfo = new TxnInfo(request.getTxnInfo().getBillerId(),
							request.getTxnInfo().getBillNumber(), billerNameRes, billStatusRes, formattedDueDate, "",
							"", tranAuthIdRes, "");

					AdditionalInfo additionalInfo = new AdditionalInfo(request.getAdditionalInfo().getReserveField1(),
							request.getAdditionalInfo().getReserveField2(),
							request.getAdditionalInfo().getReserveField3(),
							request.getAdditionalInfo().getReserveField4(),
							request.getAdditionalInfo().getReserveField5(),
							request.getAdditionalInfo().getReserveField6(),
							request.getAdditionalInfo().getReserveField7(),
							request.getAdditionalInfo().getReserveField8(),
							request.getAdditionalInfo().getReserveField9(),
							request.getAdditionalInfo().getReserveField10());

					response = new BillInquiryResponse(info, txnInfo, additionalInfo);

					return response;
				}
				
				
				else if (thardeepgetVoucherResponse.getResponse().getResponseCode()
						.equalsIgnoreCase(Constants.ResponseCodes.UNKNOWN_ERROR)) {

					info = new Info(thardeepgetVoucherResponse.getResponse().getResponseCode(),
							thardeepgetVoucherResponse.getResponse().getResponseDesc(), rrn, stan);

					TxnInfo txnInfo = new TxnInfo(request.getTxnInfo().getBillerId(),
							request.getTxnInfo().getBillNumber(), billerNameRes, billStatusRes, formattedDueDate, "",
							"", tranAuthIdRes, "");

					AdditionalInfo additionalInfo = new AdditionalInfo(request.getAdditionalInfo().getReserveField1(),
							request.getAdditionalInfo().getReserveField2(),
							request.getAdditionalInfo().getReserveField3(),
							request.getAdditionalInfo().getReserveField4(),
							request.getAdditionalInfo().getReserveField5(),
							request.getAdditionalInfo().getReserveField6(),
							request.getAdditionalInfo().getReserveField7(),
							request.getAdditionalInfo().getReserveField8(),
							request.getAdditionalInfo().getReserveField9(),
							request.getAdditionalInfo().getReserveField10());

					response = new BillInquiryResponse(info, txnInfo, additionalInfo);

					return response;
				}
				
				

				else if (billStatusRes.equalsIgnoreCase(Constants.BILL_STATUS_SINGLE_ALPHABET.BILL_PAID)) {
					
					
					PaymentLog paymentLog = paymentLogRepository
							.findFirstByBillerIdAndBillerNumberAndBillStatusIgnoreCaseAndActivityAndResponseCodeOrderByIDDesc(
									request.getTxnInfo().getBillerId().trim(),
									request.getTxnInfo().getBillNumber().trim(), Constants.BILL_STATUS.BILL_PAID,
									Constants.ACTIVITY.BillPayment, Constants.ResponseCodes.OK);

					if (paymentLog != null) {

						billStatus = "Paid";

						transAuthId = paymentLog.getTranAuthId();
						amountInDueToDate = paymentLog.getAmountwithinduedate();
						amountAfterDate = paymentLog.getAmountafterduedate();
						billerName = paymentLog.getName();
						amountPaid = paymentLog.getAmountPaid();
						dueDate = paymentLog.getDuedate();
						billingMonth = paymentLog.getBillingMonth();

					} else {

						info = new Info(Constants.ResponseCodes.PAYMENT_NOT_FOUND,
								Constants.ResponseDescription.PAYMENT_NOT_FOUND, rrn, stan);

						TxnInfo txnInfo = new TxnInfo(request.getTxnInfo().getBillerId(),
								request.getTxnInfo().getBillNumber(), billerName, billStatus, dueDate,
								String.valueOf(amountInDueToDate), String.valueOf(amountAfterDate), transAuthId, "");

						AdditionalInfo additionalInfo = new AdditionalInfo(
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

						response = new BillInquiryResponse(info, txnInfo, additionalInfo);

						return response;
					}

					info = new Info(Constants.ResponseCodes.OK, Constants.ResponseDescription.OPERATION_SUCCESSFULL,
							rrn, stan);

					TxnInfo txnInfo = new TxnInfo(request.getTxnInfo().getBillerId(),
							request.getTxnInfo().getBillNumber(), billerName, billStatus, dueDate,
							String.valueOf(amountInDueToDate), String.valueOf(amountAfterDate), transAuthId, "");

					AdditionalInfo additionalInfo = new AdditionalInfo(request.getAdditionalInfo().getReserveField1(),
							request.getAdditionalInfo().getReserveField2(),
							request.getAdditionalInfo().getReserveField3(),
							request.getAdditionalInfo().getReserveField4(),
							request.getAdditionalInfo().getReserveField5(),
							request.getAdditionalInfo().getReserveField6(),
							request.getAdditionalInfo().getReserveField7(),
							request.getAdditionalInfo().getReserveField8(),
							request.getAdditionalInfo().getReserveField9(),
							request.getAdditionalInfo().getReserveField10());

					response = new BillInquiryResponse(info, txnInfo, additionalInfo);

					transactionStatus = Constants.Status.Success;

					return response;
				}

						
				
				if (billStatusRes.equalsIgnoreCase(Constants.BILL_STATUS_SINGLE_ALPHABET.BILL_UNPAID)) {

					billStatus = "Unpaid";

					transactionStatus = Constants.Status.Pending;
				}

				else if (billStatusRes.equalsIgnoreCase(Constants.BILL_STATUS_SINGLE_ALPHABET.BILL_EXPIRED)) {

					billStatus = "Expired";

					transactionStatus = Constants.Status.Expired;
				}

				else if (billStatusRes.equalsIgnoreCase(Constants.BILL_STATUS_SINGLE_ALPHABET.BILL_BLOCK)) {

					billStatus = "Block";

					transactionStatus = Constants.Status.Block;
				}

				if (thardeepgetVoucherResponse.getResponse().getResponseCode().equalsIgnoreCase(ResponseCodes.OK)) {

					info = new Info(thardeepgetVoucherResponse.getResponse().getResponseCode(),
							Constants.ResponseDescription.OPERATION_SUCCESSFULL, rrn, stan);

					TxnInfo txnInfo = new TxnInfo(request.getTxnInfo().getBillerId(),
							request.getTxnInfo().getBillNumber(), billerNameRes, billStatusRes, formattedDueDate,
							String.valueOf(amountInDueToDate), "", tranAuthIdRes, "");

					AdditionalInfo additionalInfo = new AdditionalInfo(request.getAdditionalInfo().getReserveField1(),
							request.getAdditionalInfo().getReserveField2(),
							request.getAdditionalInfo().getReserveField3(),
							request.getAdditionalInfo().getReserveField4(),
							request.getAdditionalInfo().getReserveField5(),
							request.getAdditionalInfo().getReserveField6(),
							request.getAdditionalInfo().getReserveField7(),
							request.getAdditionalInfo().getReserveField8(),
							request.getAdditionalInfo().getReserveField9(),
							request.getAdditionalInfo().getReserveField10());

					response = new BillInquiryResponse(info, txnInfo, additionalInfo);

				}

				else {

					info = new Info(thardeepgetVoucherResponse.getResponse().getResponseCode(),
							thardeepgetVoucherResponse.getResponse().getResponseDesc(), rrn, stan);

					TxnInfo txnInfo = new TxnInfo(request.getTxnInfo().getBillerId(),
							request.getTxnInfo().getBillNumber(), billerNameRes, billStatusRes, formattedDueDate, "",
							"", tranAuthIdRes, "");

					AdditionalInfo additionalInfo = new AdditionalInfo(request.getAdditionalInfo().getReserveField1(),
							request.getAdditionalInfo().getReserveField2(),
							request.getAdditionalInfo().getReserveField3(),
							request.getAdditionalInfo().getReserveField4(),
							request.getAdditionalInfo().getReserveField5(),
							request.getAdditionalInfo().getReserveField6(),
							request.getAdditionalInfo().getReserveField7(),
							request.getAdditionalInfo().getReserveField8(),
							request.getAdditionalInfo().getReserveField9(),
							request.getAdditionalInfo().getReserveField10());

					response = new BillInquiryResponse(info, txnInfo, additionalInfo);

					return response;
				}
			}

			else {

				info = new Info(Constants.ResponseCodes.SERVICE_FAIL, Constants.ResponseDescription.SERVICE_FAIL, rrn,
						stan);

				TxnInfo txnInfo = new TxnInfo(request.getTxnInfo().getBillerId(), request.getTxnInfo().getBillNumber(),
						billerNameRes, billStatusRes, formattedDueDate, "", "", tranAuthIdRes, "");

				AdditionalInfo additionalInfo = new AdditionalInfo(request.getAdditionalInfo().getReserveField1(),
						request.getAdditionalInfo().getReserveField2(), request.getAdditionalInfo().getReserveField3(),
						request.getAdditionalInfo().getReserveField4(), request.getAdditionalInfo().getReserveField5(),
						request.getAdditionalInfo().getReserveField6(), request.getAdditionalInfo().getReserveField7(),
						request.getAdditionalInfo().getReserveField8(), request.getAdditionalInfo().getReserveField9(),
						request.getAdditionalInfo().getReserveField10());

				response = new BillInquiryResponse(info, txnInfo, additionalInfo);

				return response;
			}

		}

		catch (Exception ex) {

			LOG.error("Exception {}", ex);

		}

		finally {

			try {

				String requestAsString = objectMapper.writeValueAsString(request);
				String responseAsString = objectMapper.writeValueAsString(response);

				auditLoggingService.auditLog(Constants.ACTIVITY.BillInquiry, response.getInfo().getResponseCode(),
						response.getInfo().getResponseDesc(), requestAsString, responseAsString, requestedDate,
						new Date(), rrn, request.getTxnInfo().getBillerId(), request.getTxnInfo().getBillNumber(),
						channel, username);

			} catch (Exception ex) {
				LOG.error("{}", ex);
			}

			try {

				paymentLoggingService.paymentLog(requestedDate, new Date(), rrn, stan,
						response.getInfo().getResponseCode(), response.getInfo().getResponseDesc(),
						billerNameRes == null ? billerName : billerNameRes, request.getTxnInfo().getBillNumber(),
						request.getTxnInfo().getBillerId(), amountInDueToDate, amountAfterDate,
						Constants.ACTIVITY.BillInquiry, transactionStatus, channel, billStatus,
						request.getTxnInfo().getTranDate(), request.getTxnInfo().getTranTime(), tranAuthIdRes, null,
						dueDateRes == null ? dueDate : dueDateRes,
						billingMonthRes == null ? billingMonth : billingMonthRes, "", bankName, bankCode, branchName,
						branchCode);

			} catch (Exception ex) {
				LOG.error("{}", ex);
			}

			LOG.info("----- Bill Inquiry Method End -----");

		}

		return response;

	}

}
