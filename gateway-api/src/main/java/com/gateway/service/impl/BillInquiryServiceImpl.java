package com.gateway.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
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
import com.gateway.model.mpay.response.billinquiry.fbr.FbrGetVoucherResponse;
import com.gateway.model.mpay.response.billinquiry.offline.OfflineGetVoucherResponse;
import com.gateway.model.mpay.response.billinquiry.pta.DataWrapper;
import com.gateway.model.mpay.response.billinquiry.pta.PtaGetVoucherResponse;
import com.gateway.repository.BillerConfigurationRepo;
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
	// private BillerListRepository billerListRepository;
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

	@Override
	public BillInquiryResponse billInquiry(HttpServletRequest httpRequestData, BillInquiryRequest request) {

		LOG.info("================ REQUEST billInquiry ================");
		LOG.info("===>> REQUEST ::" + request.toString());
		BillInquiryResponse billInquiryResponse = null;
		BillInquiryValidationResponse billInquiryValidationResponse = null;
		// BillerList billerDetail = null;
		// SubBillersList subBiller;
		Info info = null;
		String parentBillerId = null;
		String subBillerId = null;
		String rrn = request.getInfo().getRrn();
		String stan = request.getInfo().getStan();

		try {

			// TODO: here we have changed BillerId()
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
									if (billerDetail.getBillerName().equalsIgnoreCase("BEOE")
											&& type.equalsIgnoreCase(Constants.BillerType.ONLINE_BILLER)) { // BEOE
										switch (subBillerDetail.getSubBillerName()) {
										case BillerConstant.BEOE.BEOE:
											billInquiryResponse = billInquiryBEOE(request,
													billInquiryValidationResponse);
											break;

										default:
											LOG.info("subBiller does not exists.");
											info = new Info(Constants.ResponseCodes.INVALID_BILLER_ID,
													Constants.ResponseDescription.INVALID_BILLER_ID, rrn, stan);
											billInquiryResponse = new BillInquiryResponse(info, null, null);
											break;

										}
									} else if (billerDetail.getBillerName().equalsIgnoreCase("PRAL")
											&& type.equalsIgnoreCase(Constants.BillerType.ONLINE_BILLER)) { // PRAL

										switch (subBillerDetail.getSubBillerName()) {

										case BillerConstant.PRAL.KPPSC:
											billInquiryResponse = billInquiryKppsc(request,
													billInquiryValidationResponse, httpRequestData);
											break;
										case BillerConstant.PRAL.FBR:
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

									// PTA
									else if (billerDetail.getBillerName().equalsIgnoreCase("PTA")
											&& type.equalsIgnoreCase(Constants.BillerType.ONLINE_BILLER)) { // PRAL

										switch (subBillerDetail.getSubBillerName()) {

										case BillerConstant.PTA.PTA:
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
											billInquiryValidationResponse.getResponseDesc(), rrn, stan);
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
		try {
			ObjectMapper reqMapper = new ObjectMapper();
			String requestAsString = reqMapper.writeValueAsString(request);
			ProvinceTransaction provinceTransaction = null;
			// BillerList billersList = null;

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

	public BillInquiryResponse billInquiryBEOE(BillInquiryRequest request,
			BillInquiryValidationResponse billInquiryValidationResponse) {

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
		BigDecimal requestTotalAmountbdUp = null;

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

					Double amountInDueToDate = null;
					String amountAfterDueDate = "";
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
							PaymentLog paymentLog = paymentLogRepository.findFirstByBillerNumberAndBillStatus(
									request.getTxnInfo().getBillNumber().trim(), Constants.BILL_STATUS.BILL_PAID);
							if (paymentLog != null) {
								// datePaid = paymentLog.getTranDate();
								// billingMonth = utilMethods.formatDateString(datePaid);
								billstatus = "P";
								transAuthId = paymentLog.getTranAuthId();

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
						request.getTxnInfo().getBillerId(), request.getTxnInfo().getBillNumber(), channel, username);

			} catch (Exception ex) {
				LOG.error("{}", ex);
			}
			try {

				paymentLoggingService.paymentLog(responseDate, responseDate, rrn, stan,
						response.getInfo().getResponseCode(), response.getInfo().getResponseDesc(), cnic,
						request.getTerminalInfo().getMobile(), name, request.getTxnInfo().getBillNumber(),
						request.getTxnInfo().getBillerId(), requestTotalAmountbdUp, dbTransactionFees,
						Constants.ACTIVITY.BillInquiry, "", request.getTxnInfo().getBillNumber(), transactionStatus,
						address, transactionFees, dbTax, dbTotal, channel, billStatus,
						request.getTxnInfo().getTranDate(), request.getTxnInfo().getTranTime(), province, transAuthId);

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

		BigDecimal requestTotalAmountbdUp = null;
		try {

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

				paymentLoggingService.paymentLog(responseDate, responseDate, rrn, stan,
						response.getInfo().getResponseCode(), response.getInfo().getResponseDesc(), cnic,
						request.getTerminalInfo().getMobile(), name, request.getTxnInfo().getBillNumber(),
						request.getTxnInfo().getBillerId(), requestTotalAmountbdUp, dbTransactionFees,
						Constants.ACTIVITY.BillInquiry, "", request.getTxnInfo().getBillNumber(), transactionStatus,
						address, transactionFees, dbTax, dbTotal, channel, billStatus,
						request.getTxnInfo().getTranDate(), request.getTxnInfo().getTranTime(), province, transAuthId);

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
		String amountPaid = "";
		String amountPaidInDueDate = "";
		String amountPaidAfterDueDate = "";
		String datePaid = "";
		String billingMonth = "";
		String dueDate = "";
		String billingDate = "";
		String expiryDate = "";
		String oneBillNumber = "";
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
				info = new Info(getVoucherResponse.getResponse().getResponseCode(),
						getVoucherResponse.getResponse().getResponseDesc(), rrn, stan);
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

							PaymentLog paymentLog = paymentLogRepository.findFirstByBillerNumberAndBillStatus(
									request.getTxnInfo().getBillNumber().trim(), Constants.BILL_STATUS.BILL_PAID);
							if (paymentLog != null) {
								datePaid = paymentLog.getTranDate();
								// billingMonth = utilMethods.formatDateString(datePaid);
								billstatus = "P";
								// amountPaid = String.valueOf(paymentLog.getAmount());
								transAuthId = paymentLog.getTranAuthId();
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
						request.getTxnInfo().getBillerId(), requestAmount, dbTransactionFees,
						Constants.ACTIVITY.BillInquiry, "", request.getTxnInfo().getBillNumber(), transactionStatus,
						address, transactionFees, dbTax, dbTotal, channel, billStatus,
						request.getTxnInfo().getTranDate(), request.getTxnInfo().getTranTime(), province, transAuthId);

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
		String amountPaid = "";
		String billingMonth = "";
		String dueDAte = "";
		String oneBillNumber = "";
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

					Double amountInDueToDate = null;
					String amountAfterDueDate = "";
					String status = "";

					if (getVoucherResponse.getResponse().getPtaGetVoucher() != null) {

						DataWrapper dataWrapper = getVoucherResponse.getResponse().getPtaGetVoucher().getDataWrapper()
								.get(0);

						requestTotalAmountbdUp = BigDecimal.valueOf(Double.parseDouble(dataWrapper.getTotalAmount()))
								.setScale(2, RoundingMode.UP);
						amountInDueToDate = utilMethods.bigDecimalToDouble(requestTotalAmountbdUp);
						// amountPaidInDueDate = utilMethods.formatAmount(requestTotalAmountbdUp, 12);
						amountAfterDueDate = String.valueOf(requestTotalAmountbdUp);
						// amountPaid = amountPaidInDueDate;

						depostiroName = dataWrapper.getDepositorName();
						mobile = dataWrapper.getDepositorContactNo();
						billStatus = dataWrapper.getStatus();
						dbAmount = amountInDueToDate;
						oneBillNumber = dataWrapper.getOneBillNumber();
						oneBillNumber = dataWrapper.getOneBillNumber();
						if (oneBillNumber == null) {
							oneBillNumber = "";
						}

						dbTotal = requestTotalAmountbdUp.doubleValue();

						billStatus = dataWrapper.getStatus().trim().equals("0") ? Constants.BILL_STATUS.BILL_UNPAID
								: Constants.BILL_STATUS.BILL_PAID;
						if (billStatus.equalsIgnoreCase(Constants.BILL_STATUS.BILL_PAID)) {
							PaymentLog paymentLog = paymentLogRepository.findFirstByBillerNumberAndBillStatus(
									request.getTxnInfo().getBillNumber().trim(), Constants.BILL_STATUS.BILL_PAID);
							if (paymentLog != null) {
								datePaid = paymentLog.getTranDate();
								// billingMonth = utilMethods.formatDateString(datePaid);
								status = "P";
								transAuthId = paymentLog.getTranAuthId();

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
								request.getAdditionalInfo().getReserveField5());

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
						request.getTxnInfo().getBillerId(), requestTotalAmountbdUp, dbTransactionFees,
						Constants.ACTIVITY.BillInquiry, "", request.getTxnInfo().getBillNumber(), transactionStatus,
						address, transactionFees, dbTax, dbTotal, channel, billStatus,
						request.getTxnInfo().getTranDate(), request.getTxnInfo().getTranTime(), province, transAuthId);

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
		double amountInDueToDate = 0;// double dbAmount = 0;
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
		BigDecimal requestAmount = null;
		String reserved = "";

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
				info = new Info(fbrGetVoucherResponse.getResponse().getResponseCode(),
						fbrGetVoucherResponse.getResponse().getResponseDesc(), rrn, stan);
				if (fbrGetVoucherResponse.getResponse().getResponseCode().equals(ResponseCodes.OK)) {

					double amountAfterDueDate = 0;

					BigDecimal requestAmountafterduedate = null;
					if (fbrGetVoucherResponse.getResponse().getPralFbrGetVoucher() != null) {

						String amountStr = fbrGetVoucherResponse.getResponse().getPralFbrGetVoucher()
								.getAmountWithinDueDate();
						String amountAfterDueDateStr = fbrGetVoucherResponse.getResponse().getPralFbrGetVoucher()
								.getAmountAfterDueDate();

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

						name = fbrGetVoucherResponse.getResponse().getPralFbrGetVoucher().getConsumerDetail();
						dueDate = fbrGetVoucherResponse.getResponse().getPralFbrGetVoucher().getDueDate();
						reserved = fbrGetVoucherResponse.getResponse().getPralFbrGetVoucher().getReserved();
						if (reserved == null || reserved.isBlank() || reserved.isEmpty()) {
							reserved = request.getAdditionalInfo().getReserveField1();
						}
						billStatus = fbrGetVoucherResponse.getResponse().getPralFbrGetVoucher().getBillStatus().trim()
								.equalsIgnoreCase("U") ? Constants.BILL_STATUS.BILL_UNPAID
										: Constants.BILL_STATUS.BILL_PAID;
						dbBillStatus = billStatus;
						// dbAmount = amountInDueToDate;
						oneBillNumber = fbrGetVoucherResponse.getResponse().getPralFbrGetVoucher().getOneBillNumber();
						if (oneBillNumber == null) {
							oneBillNumber = "";
						}
//						amountPaid = String.format("%012d",
//								Integer.parseInt(getVoucherResponse.getResponse().getGetvoucher().getTotal()));

						// dbTotal = requestTotalAmountbdUp.doubleValue();

						if (billStatus.equalsIgnoreCase(Constants.BILL_STATUS.BILL_PAID)) {
							PaymentLog paymentLog = paymentLogRepository.findFirstByBillerNumberAndBillStatus(
									request.getTxnInfo().getBillNumber().trim(), Constants.BILL_STATUS.BILL_PAID);
							if (paymentLog != null) {
								// datePaid = paymentLog.getTranDate();
								// billingMonth = utilMethods.formatDateString(datePaid);
								billStatus = "P";
								transAuthId = paymentLog.getTranAuthId();

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

					}

					TxnInfo txnInfo = new TxnInfo(request.getTxnInfo().getBillerId(),
							request.getTxnInfo().getBillNumber(), name, billStatus, dueDate,
							String.valueOf(requestAmount), String.valueOf(requestAmountafterduedate), transAuthId,
							oneBillNumber);

					AdditionalInfo additionalInfo = new AdditionalInfo(reserved,

							request.getAdditionalInfo().getReserveField2(),
							request.getAdditionalInfo().getReserveField3(),
							request.getAdditionalInfo().getReserveField4(),
							request.getAdditionalInfo().getReserveField5());

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
						request.getTxnInfo().getBillerId(), requestAmount, dbTransactionFees,
						Constants.ACTIVITY.BillInquiry, "", request.getTxnInfo().getBillNumber(), transactionStatus,
						address, transactionFees, dbTax, dbTotal, channel, dbBillStatus,
						request.getTxnInfo().getTranDate(), request.getTxnInfo().getTranTime(), province, transAuthId);

			} catch (Exception ex) {
				LOG.error("{}", ex);
			}
		}
		return response;
	}
}