package com.gateway.service.impl;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gateway.entity.BillerConfiguration;
import com.gateway.entity.CombinedPaymentLogView;
import com.gateway.entity.FeeType;
import com.gateway.entity.PaymentLog;
import com.gateway.entity.PendingPayment;
import com.gateway.entity.PgPaymentLog;
import com.gateway.entity.ProvinceTransaction;
import com.gateway.entity.SubBillersList;
import com.gateway.model.mpay.response.billinquiry.BillerDetails;
import com.gateway.model.mpay.response.billinquiry.GetVoucherResponse;
import com.gateway.model.mpay.response.billinquiry.aiou.AiouGetVoucherResponse;
import com.gateway.model.mpay.response.billinquiry.bisekohat.BiseKohatBillInquiryResponse;
import com.gateway.model.mpay.response.billinquiry.bppra.BppraSupplierVoucherResponse;
import com.gateway.model.mpay.response.billinquiry.bppra.BppraTenderVoucherResponse;
import com.gateway.model.mpay.response.billinquiry.bppra.ChallanFee;
import com.gateway.model.mpay.response.billinquiry.bzu.BzuGetVoucherResponse;
import com.gateway.model.mpay.response.billinquiry.dls.DlsGetVoucherResponse;
import com.gateway.model.mpay.response.billinquiry.fbr.FbrGetVoucherResponse;
import com.gateway.model.mpay.response.billinquiry.lesco.LescoBillInquiryResponse;
import com.gateway.model.mpay.response.billinquiry.offline.OfflineGetVoucherResponse;
import com.gateway.model.mpay.response.billinquiry.pitham.PithamGetVoucherResponse;
import com.gateway.model.mpay.response.billinquiry.pta.DataWrapper;
import com.gateway.model.mpay.response.billinquiry.pta.PtaGetVoucherResponse;
import com.gateway.model.mpay.response.billinquiry.pu.PuBillInquiryResponse;
import com.gateway.model.mpay.response.billinquiry.slic.SlicPolicyInquiryResponse;
import com.gateway.model.mpay.response.billinquiry.thardeep.ThardeepGetVoucherResponse;
import com.gateway.model.mpay.response.billinquiry.uom.UomGetVoucherResponse;
import com.gateway.model.mpay.response.billinquiry.wasa.WasaBillnquiryResponse;
import com.gateway.model.mpay.response.billpayment.UpdateVoucherResponse;
import com.gateway.model.mpay.response.billpayment.aiou.AiouUpdateVoucherResponse;
import com.gateway.model.mpay.response.billpayment.bisekohat.UpdateBiseKohatResponse;
import com.gateway.model.mpay.response.billpayment.bzu.BzuUpdateVoucherResponse;
import com.gateway.model.mpay.response.billpayment.dls.DlsUpdateVoucherResponse;
import com.gateway.model.mpay.response.billpayment.fbr.FbrUpdateVoucherResponse;
import com.gateway.model.mpay.response.billpayment.lesco.LescoBillPaymentResponse;
import com.gateway.model.mpay.response.billpayment.offline.OfflineUpdateVoucherResponse;
import com.gateway.model.mpay.response.billpayment.pitham.PithamUpdateVoucherResponse;
import com.gateway.model.mpay.response.billpayment.pta.PtaUpdateVoucherResponse;
import com.gateway.model.mpay.response.billpayment.pu.PuBillPaymentResponse;
import com.gateway.model.mpay.response.billpayment.slic.UpdateSlicPolicyTranslationResponse;
import com.gateway.model.mpay.response.billpayment.thardeep.ThardeepUpdateVoucherResponse;
import com.gateway.model.mpay.response.billpayment.uom.UomUpdateVoucherResponse;
import com.gateway.model.mpay.response.billpayment.wasa.WasaBillPaymentResponse;
import com.gateway.repository.BillerConfigurationRepo;
import com.gateway.repository.CombinedPaymentLogViewRepository;
import com.gateway.repository.FeeTypeRepository;
import com.gateway.repository.PaymentLogRepository;
import com.gateway.repository.PendingPaymentRepository;
import com.gateway.repository.PgPaymentLogRepository;
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
import com.gateway.utils.FeeTypeMapper;
import com.gateway.utils.JwtTokenUtil;
import com.gateway.utils.MemcachedService;
import com.gateway.utils.RSAEncryption;
import com.gateway.utils.UtilMethods;

import jakarta.servlet.http.HttpServletRequest;
import kong.unirest.HttpResponse;

@Service
public class BillPaymentServiceImpl implements BillPaymentService {

	private static final Logger LOG = LoggerFactory.getLogger(BillPaymentServiceImpl.class);

	@Value("${payment.pendingThresholdMinutes}")
	private int pendingThresholdMinutes;

	@Value("${payment.pendingVoucherUpdateMessage}")
	private String pendingVoucherUpdateMessage;

	@Value("${payment.pendingPaymentMessage}")
	private String pendingPaymentMessage;

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
	private CombinedPaymentLogViewRepository combinedPaymentLogViewRepository;

	@Autowired
	private ObjectMapper objectMapper;

	@Value("${fbr.identification.type}")
	private String identificationType;

	@Value("${uom.bank.mnemonic}")
	private String bankMnemonic;

	@Value("${uom.reserved}")
	private String reserved;

	@Autowired
	private FeeTypeMapper feeTypeMapper;

	@Autowired
	private FeeTypeRepository feeTypeRepository;

	@Value("${payment.log.table}")
	private String paymentLogTable;

	@Autowired
	private ObjectMapper mapper;

	@Autowired
	private PgPaymentLogRepository pgPaymentLogRepository;

	@Autowired
	private PendingPaymentRepository pendingPaymentRepository;

	@Autowired
	private RSAEncryption rsaUtility;

	@Value("${bzu.username}")
	private String bzu_username;

	@Value("${bzu.password}")
	private String bzu_password;

	@Value("${slic.billerIdLoan}")
	private String billerIdLoan;

	@Value("${slic.billerIdPremium}")
	private String billerIdPremium;

	@Value("${bppra.authticateCall}")
	private String bppraAuthticateCall;

	@Value("${bppra.tokenInquiryCall}")
	private String bppraTokenInquiryCall;

	@Value("${bppra.tenderChallanInquiryCall}")
	private String bppraChallanInquiryCall;

	@Value("${bppra.completeInquiryCall}")
	private String bppraCompleteInquiryCall;

	@Value("${bppra.tenderChallanPaidCall}")
	private String bppraTenderChallanPaidCall;

	@Value("${bppra.supplierChallanInquiryCall}")
	private String bppraSupplierChallanInquiryCall;

	@Value("${bppra.supplierChallanPaidCall}")
	private String bppraSupplierChallanPaidCall;

	@Value("${bppra.clientSecret}")
	private String bppraClientSecret;

	@Value("${bppra.requestFormAuth}")
	private String requestFormAuth;

	@Value("${bppra.requestFormChallanEnquire}")
	private String requestFormChallanEnquire;

	@Value("${bppra.branchCode}")
	private String branchcode;

	@Value("${bppra.personName}")
	private String personName;

	@Value("${bppra.tenderPrefix}")
	private String tenderPrefix;

	@Value("${bppra.supplierPrefix}")
	private String supplierPrefix;

	@Value("${bise.kohat.username}")
	private String kusername;

	@Value("${bise.kohat.password}")
	private String kpassword;

	@Value("${lesco.coll_Mechanism_Code}")
	private String collMechanismCode;

	@Value("${lesco.bank_Branch_Code}")
	private String bankBranchCode;

	@Value("${pithm.paymentMode}")
	private String paymentMode;

	@Value("${pu.channel}")
	private String puChannel;

	@Value("${wasa.consumerCell}")
	private String consumerCell;

	@Value("${wasa.payMode}")
	private String payMode;

	@Value("${wasa.tranStatus}")
	private String tranStatus;

	@Autowired
	private MemcachedService memcachedService;

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

									///////// Pitham ///////

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

									////////// Pitham ///////

									////////// Thardeep ///////

									else if (billerDetail.getBillerName()
											.equalsIgnoreCase(BillerConstant.Thardeep.THARDEEP)
											&& type.equalsIgnoreCase(Constants.BillerType.ONLINE_BILLER)) {

										switch (subBillerDetail.getSubBillerName()) {

										case BillerConstant.Thardeep.THARDEEP:
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

									////////// Thardeep //////

									////////// Uom ///////////

									else if (billerDetail.getBillerName().equalsIgnoreCase(BillerConstant.Uom.UOM)
											&& type.equalsIgnoreCase(Constants.BillerType.ONLINE_BILLER)) {

										switch (subBillerDetail.getSubBillerName()) {

										case BillerConstant.Uom.UOM:
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

									////////// Uom //////////

									////////// Dls ///////

									else if (billerDetail.getBillerName().equalsIgnoreCase(BillerConstant.Dls.DLS)
											&& type.equalsIgnoreCase(Constants.BillerType.ONLINE_BILLER)) {

										switch (subBillerDetail.getSubBillerName()) {

										case BillerConstant.Dls.DLS:
											billPaymentResponse = billPaymentDls(request, httpRequestData);
											break;

										default:
											LOG.info("subBiller does not exists.");
											infoPay = new InfoPay(Constants.ResponseCodes.INVALID_BILLER_ID,
													Constants.ResponseDescription.INVALID_BILLER_ID, rrn, stan);
											billPaymentResponse = new BillPaymentResponse(infoPay, null, null);

											break;
										}
									}

									////////// Dls //////////

									////////// Bzu ///////

									else if (billerDetail.getBillerName().equalsIgnoreCase(BillerConstant.BZU.BZU)
											&& type.equalsIgnoreCase(Constants.BillerType.ONLINE_BILLER)) {

										switch (subBillerDetail.getSubBillerName()) {

										case BillerConstant.BZU.BZU:
											billPaymentResponse = billPaymentBzu(request, httpRequestData);
											break;

										default:
											LOG.info("subBiller does not exists.");
											infoPay = new InfoPay(Constants.ResponseCodes.INVALID_BILLER_ID,
													Constants.ResponseDescription.INVALID_BILLER_ID, rrn, stan);
											billPaymentResponse = new BillPaymentResponse(infoPay, null, null);

											break;
										}
									}

									////////// Bzu //////////

									////////// State Life ///////

									else if (billerDetail.getBillerName().equalsIgnoreCase(BillerConstant.SLIC.SLIC)
											&& type.equalsIgnoreCase(Constants.BillerType.ONLINE_BILLER)) {

										switch (subBillerDetail.getSubBillerName()) {

										case BillerConstant.SLIC.SLIC:
											billPaymentResponse = billPaymentSlic(request, httpRequestData);
											break;

										default:
											LOG.info("subBiller does not exists.");
											infoPay = new InfoPay(Constants.ResponseCodes.INVALID_BILLER_ID,
													Constants.ResponseDescription.INVALID_BILLER_ID, rrn, stan);
											billPaymentResponse = new BillPaymentResponse(infoPay, null, null);

											break;
										}
									}

									////////// State Life ///////

									////////// BPPRA ///////

									else if (billerDetail.getBillerName().equalsIgnoreCase(BillerConstant.BPPRA.BPPRA)
											&& type.equalsIgnoreCase(Constants.BillerType.ONLINE_BILLER)) {

										switch (subBillerDetail.getSubBillerName()) {

										case BillerConstant.BPPRA.BPPRA:
											billPaymentResponse = billPaymentBppra(request, httpRequestData);
											break;

										default:
											LOG.info("subBiller does not exists.");
											infoPay = new InfoPay(Constants.ResponseCodes.INVALID_BILLER_ID,
													Constants.ResponseDescription.INVALID_BILLER_ID, rrn, stan);
											billPaymentResponse = new BillPaymentResponse(infoPay, null, null);

											break;
										}
									}

									////////// BPPRA ///////

									////////// BISE-KOHAT ///////

									else if (billerDetail.getBillerName()
											.equalsIgnoreCase(BillerConstant.BISEKOHAT.BISEKOHAT)
											&& type.equalsIgnoreCase(Constants.BillerType.ONLINE_BILLER)) {

										switch (subBillerDetail.getSubBillerName()) {

										case BillerConstant.BISEKOHAT.BISEKOHAT:
											billPaymentResponse = billPaymentBiseKohat(request, httpRequestData);
											break;

										default:
											LOG.info("subBiller does not exists.");
											infoPay = new InfoPay(Constants.ResponseCodes.INVALID_BILLER_ID,
													Constants.ResponseDescription.INVALID_BILLER_ID, rrn, stan);
											billPaymentResponse = new BillPaymentResponse(infoPay, null, null);

											break;
										}
									}

									////////// BISE-KOHAT ///////

									////////// LESCO ///////

									else if (billerDetail.getBillerName().equalsIgnoreCase(BillerConstant.LESCO.LESCO)
											&& type.equalsIgnoreCase(Constants.BillerType.ONLINE_BILLER)) {

										switch (subBillerDetail.getSubBillerName()) {

										case BillerConstant.LESCO.LESCO:
											billPaymentResponse = billPaymentLesco(request, httpRequestData);
											break;

										default:
											LOG.info("subBiller does not exists.");
											infoPay = new InfoPay(Constants.ResponseCodes.INVALID_BILLER_ID,
													Constants.ResponseDescription.INVALID_BILLER_ID, rrn, stan);
											billPaymentResponse = new BillPaymentResponse(infoPay, null, null);

											break;
										}
									}

									////////// LESCO ///////

									////////// WASA ///////

									else if (billerDetail.getBillerName().equalsIgnoreCase(BillerConstant.WASA.WASA)
											&& type.equalsIgnoreCase(Constants.BillerType.ONLINE_BILLER)) {

										switch (subBillerDetail.getSubBillerName()) {

										case BillerConstant.WASA.WASA:
											billPaymentResponse = billPaymentWasa(request, httpRequestData);
											break;

										default:
											LOG.info("subBiller does not exists.");
											infoPay = new InfoPay(Constants.ResponseCodes.INVALID_BILLER_ID,
													Constants.ResponseDescription.INVALID_BILLER_ID, rrn, stan);
											billPaymentResponse = new BillPaymentResponse(infoPay, null, null);

											break;
										}
									}

									////////// WASA ///////

									////////// PU ///////

									else if (billerDetail.getBillerName().equalsIgnoreCase(BillerConstant.PU.PU)
											&& type.equalsIgnoreCase(Constants.BillerType.ONLINE_BILLER)) {

										switch (subBillerDetail.getSubBillerName()) {

										case BillerConstant.PU.PU:
											billPaymentResponse = billPaymentPu(request, httpRequestData);
											break;

										default:
											LOG.info("subBiller does not exists.");
											infoPay = new InfoPay(Constants.ResponseCodes.INVALID_BILLER_ID,
													Constants.ResponseDescription.INVALID_BILLER_ID, rrn, stan);
											billPaymentResponse = new BillPaymentResponse(infoPay, null, null);

											break;
										}
									}

									////////// PU ///////

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

			Pair<Boolean, String> validationResponse = paramsValidatorService.validateRequestParams(requestAsString);
			if (!validationResponse.getLeft()) {
				response = new BillPaymentValidationResponse(Constants.ResponseCodes.INVALID_DATA,
						validationResponse.getRight(), rrn, stan);
				return response;
			}

			try {
				String[] result = jwtTokenUtil.getTokenInformation(httpRequestData);
				username = result[0];
				channel = result[1];

			} catch (Exception ex) {
				LOG.info("BillPaymentServiceImpl - billPaymentValidations - Getting token error : " + ex);

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

			response = new BillPaymentValidationResponse("00", "SUCCESS", username, channel, provinceTransaction, rrn,
					stan);

		} catch (Exception ex) {
			LOG.info("BillDetailsServiceImpl - billPaymentValidations : " + ex);

		}

		return response;

	}

	@Override
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
		BigDecimal inquiryTotalAmountbdUp = null, amountInDueToDate = null, amountAfterDueDate = null;
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
					amountInDueToDate = inquiryTotalAmountbdUp;
					// amountPaidInDueDate = utilMethods.formatAmount(requestTotalAmountbdUp, 12);
					amountAfterDueDate = amountInDueToDate;
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

							//////////////////////////////////////////////////////////

							PendingPayment pendingPayment = pendingPaymentRepository
									.findFirstByVoucherIdAndBillerIdOrderByPaymentIdDesc(
											request.getTxnInfo().getBillNumber().trim(),
											request.getTxnInfo().getBillerId().trim());

							if (pendingPayment != null) {

								if (pendingPayment.getIgnoreTimer()) {

									infoPay = new InfoPay(Constants.ResponseCodes.UNKNOWN_ERROR, pendingPaymentMessage,
											rrn, stan);
									response = new BillPaymentResponse(infoPay, null, null);
									transactionStatus = Constants.Status.Pending;
									billStatus = Constants.BILL_STATUS.BILL_PENDING;
									return response;

								} else {
									LocalDateTime transactionDateTime = pendingPayment.getTransactionDate();
									LocalDateTime now = LocalDateTime.now(); // Current date and time

									// Calculate the difference in minutes
									long minutesDifference = Duration.between(transactionDateTime, now).toMinutes();

									if (minutesDifference <= pendingThresholdMinutes) {

										infoPay = new InfoPay(Constants.ResponseCodes.UNKNOWN_ERROR,
												pendingPaymentMessage, rrn, stan);
										response = new BillPaymentResponse(infoPay, null, null);

										transactionStatus = Constants.Status.Pending;
										billStatus = Constants.BILL_STATUS.BILL_PENDING;
										return response;

									}
								}
							}

							LOG.info("Calling Payment Inquiry from pg_payment_log table");
							PgPaymentLog pgPaymentLog = pgPaymentLogRepository
									.findFirstByVoucherIdAndBillerIdAndBillStatus(request.getTxnInfo().getBillNumber(),
											request.getTxnInfo().getBillerId(), Constants.BILL_STATUS.BILL_PAID);

							if (pgPaymentLog != null
									&& pgPaymentLog.getTransactionStatus().equalsIgnoreCase(Constants.Status.Success)) {

								infoPay = new InfoPay(Constants.ResponseCodes.UNKNOWN_ERROR,
										pendingVoucherUpdateMessage, rrn, stan); // success

								transactionStatus = Constants.Status.Success;
								billStatus = Constants.BILL_STATUS.BILL_PAID;

								response = new BillPaymentResponse(infoPay, null, null);

								return response;
							}

							/////////////////////////////////////////////////////////////////////////////

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
						request.getTxnInfo().getBillerId(), inquiryTotalAmountbdUp, amountInDueToDate,
						amountAfterDueDate, dbTransactionFees, Constants.ACTIVITY.BillPayment, paymentRefrence,
						request.getTxnInfo().getBillNumber(), transactionStatus, address, dbTotal, channel, billStatus,
						request.getTxnInfo().getTranDate(), request.getTxnInfo().getTranTime(), province, transAuthId,
						bankName, bankCode, branchName, branchCode, username, "");

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
		BigDecimal inquiryTotalAmountbdUp = null, amountInDueToDate = null, amountAfterDueDate = null;
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
					amountInDueToDate = inquiryTotalAmountbdUp;
					amountAfterDueDate = amountInDueToDate;
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
						request.getTxnInfo().getBillerId(), inquiryTotalAmountbdUp, amountInDueToDate,
						amountAfterDueDate, dbTransactionFees, Constants.ACTIVITY.BillPayment, paymentRefrence,
						request.getTxnInfo().getBillNumber(), transactionStatus, address, dbTotal, channel, billStatus,
						request.getTxnInfo().getTranDate(), request.getTxnInfo().getTranTime(), province, transAuthId,
						bankName, bankCode, branchName, branchCode, username, "");

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
		double amountAfterDueDate = 0;

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
						request.getTxnInfo().getBillerId(), requestAmount, new BigDecimal(amountInDueToDate),
						new BigDecimal(amountAfterDueDate), dbTransactionFees, Constants.ACTIVITY.BillPayment,
						paymentRefrence, request.getTxnInfo().getBillNumber(), transactionStatus, address, dbTotal,
						channel, billStatus, request.getTxnInfo().getTranDate(), request.getTxnInfo().getTranTime(),
						province, transAuthId, bankName, bankCode, branchName, branchCode, username, "");

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
		double amountInDueToDate = 0;
		String amountAfterDueDate = "";

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
								ubpsBillParams.add(request.getBranchInfo().getBranchCode());
								ubpsBillParams.add(request.getBranchInfo().getBranchName());
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
						request.getTxnInfo().getBillerId(), requestTotalAmountbdUp,
						(Double.isNaN(amountInDueToDate) ? BigDecimal.ZERO : new BigDecimal(amountInDueToDate)),
						(amountAfterDueDate == null || amountAfterDueDate.equals("") ? BigDecimal.ZERO
								: new BigDecimal(amountAfterDueDate)),
						dbTransactionFees, Constants.ACTIVITY.BillPayment, paymentRefrence,
						request.getTxnInfo().getBillNumber(), transactionStatus, address, dbTotal, channel, billStatus,
						request.getTxnInfo().getTranDate(), request.getTxnInfo().getTranTime(), province, transAuthId,
						bankName, bankCode, branchName, branchCode, username, "");

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
		BigDecimal requestAmountDb = null, amountAfterDueDate = null;
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
						request.getTxnInfo().getBillerId(), requestAmountDb, amountInDueToDate, amountAfterDueDate,
						dbTransactionFees, Constants.ACTIVITY.BillPayment, paymentRefrence,
						request.getTxnInfo().getBillNumber(), transactionStatus, address, dbTotal, channel, billStatus,
						request.getTxnInfo().getTranDate(), request.getTxnInfo().getTranTime(), province, transAuthId,
						bankName, bankCode, branchName, branchCode, username, "");

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

								// ubpsBillParams.add("123456");// Stan

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
						request.getTxnInfo().getBillNumber(), transactionStatus, address, dbTotal, channel, billStatus,
						request.getTxnInfo().getTranDate(), request.getTxnInfo().getTranTime(), province, transAuthId,
						bankName, bankCode, branchName, branchCode, username, "");

			} catch (Exception ex) {
				LOG.error("{}", ex);
			}
		}

		UtilMethods.generalLog("OUT -  Bill Payment Response {}" + response, LOG);

		return response;

	}

	@Override
	public BillPaymentResponse billPaymentPitham(BillPaymentRequest request, HttpServletRequest httpRequestData) {

		LOG.info("PITHM Bill Payment Request {} ", request.toString());

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
		String amountPaidInDueDate = "", amountPaid = "";
		String channel = "";
		String username = "";

		ArrayList<String> inquiryParams = new ArrayList<String>();
		ArrayList<String> paymentParams = new ArrayList<String>();

		String amountWithInDueDateRes = "", amountAfterDueDateRes = "";
		String billerNameRes = "", dueDateRes = "", billingMonthRes = "", billStatusCodeRes = "", billStatus = "",
				billStatusDescRes = "";
		String billerId = "", billerNumber = "";
		String paymentRefrence = utilMethods.getRRN();

		String bankName = "", bankCode = "", branchName = "", branchCode = "";
		BigDecimal nullValue = null;

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
					billStatus = Constants.BILL_STATUS.BILL_PAID;
					return response;

				}

				else if (pithamgetVoucherResponse.getResponseCode().equalsIgnoreCase(Constants.ResponseCodes.OK)) {

					////////////////////////

					PendingPayment pendingPayment = pendingPaymentRepository
							.findFirstByVoucherIdAndBillerIdOrderByPaymentIdDesc(
									request.getTxnInfo().getBillNumber().trim(),
									request.getTxnInfo().getBillerId().trim());

					if (pendingPayment != null) {

						if (pendingPayment.getIgnoreTimer()) {

							infoPay = new InfoPay(Constants.ResponseCodes.UNKNOWN_ERROR, pendingPaymentMessage, rrn,
									stan);
							response = new BillPaymentResponse(infoPay, null, null);
							transactionStatus = Constants.Status.Pending;
							return response;

						} else {
							LocalDateTime transactionDateTime = pendingPayment.getTransactionDate();
							LocalDateTime now = LocalDateTime.now(); // Current date and time

							// Calculate the difference in minutes
							long minutesDifference = Duration.between(transactionDateTime, now).toMinutes();

							if (minutesDifference <= pendingThresholdMinutes) {

								infoPay = new InfoPay(Constants.ResponseCodes.UNKNOWN_ERROR, pendingPaymentMessage, rrn,
										stan);
								response = new BillPaymentResponse(infoPay, null, null);

								transactionStatus = Constants.Status.Pending;
								return response;

							}
						}
					}

					LOG.info("Calling Payment Inquiry from pg_payment_log table");
					PgPaymentLog pgPaymentLog = pgPaymentLogRepository.findFirstByVoucherIdAndBillerIdAndBillStatus(
							request.getTxnInfo().getBillNumber(), request.getTxnInfo().getBillerId(),
							Constants.BILL_STATUS.BILL_PAID);

					if (pgPaymentLog != null
							&& pgPaymentLog.getTransactionStatus().equalsIgnoreCase(Constants.Status.Success)) {

						infoPay = new InfoPay(Constants.ResponseCodes.UNKNOWN_ERROR, pendingVoucherUpdateMessage, rrn,
								stan); // success

						transactionStatus = Constants.Status.Success;

						response = new BillPaymentResponse(infoPay, null, null);

						return response;

					}

					///////////////////////

					billerNameRes = pithamgetVoucherResponse.getPithmGetVoucher().getGetInquiryResult()
							.getStudentName();
					billingMonthRes = pithamgetVoucherResponse.getPithmGetVoucher().getGetInquiryResult()
							.getBillingMonth();
					billStatusCodeRes = pithamgetVoucherResponse.getPithmGetVoucher().getGetInquiryResult().getStatus();
					billStatusDescRes = pithamgetVoucherResponse.getPithmGetVoucher().getGetInquiryResult()
							.getStatusDesc();
					dueDateRes = pithamgetVoucherResponse.getPithmGetVoucher().getGetInquiryResult().getDueDate();
					amountWithInDueDateRes = pithamgetVoucherResponse.getPithmGetVoucher().getGetInquiryResult()
							.getAmountWidDate();
					amountAfterDueDateRes = pithamgetVoucherResponse.getPithmGetVoucher().getGetInquiryResult()
							.getAmountAdDate();

					if (billStatusDescRes.equalsIgnoreCase(Constants.BILL_STATUS.BILL_EXPIRED)) {

						infoPay = new InfoPay(Constants.ResponseCodes.INVALID_DATA,
								Constants.ResponseDescription.EXPIRED, rrn, stan);

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
					paymentParams.add(paymentMode);
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

					pithanUpdateVoucherResponse = serviceCaller.get(paymentParams, PithamUpdateVoucherResponse.class,
							rrn, Constants.ACTIVITY.BillPayment, BillerConstant.Pithm.PITHM);

					infoPay = new InfoPay(Constants.ResponseCodes.OK,
							Constants.ResponseDescription.OPERATION_SUCCESSFULL, rrn, stan);

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

					billStatus = Constants.BILL_STATUS.BILL_PAID;
					transactionStatus = Constants.Status.Success;

					return response;
				}

				else {

					infoPay = new InfoPay(Constants.ResponseCodes.SERVICE_FAIL,
							Constants.ResponseDescription.SERVICE_FAIL, rrn, stan);

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
						response.getInfo().getResponseCode(), response.getInfo().getResponseDesc(), billerNameRes,
						request.getTxnInfo().getBillNumber(), request.getTxnInfo().getBillerId(),
						(!amountWithInDueDateRes.isEmpty()) ? new BigDecimal(amountWithInDueDateRes) : nullValue,
						(!amountAfterDueDateRes.isEmpty()) ? new BigDecimal(amountAfterDueDateRes) : nullValue,
						Constants.ACTIVITY.BillPayment, transactionStatus, channel, billStatus,
						request.getTxnInfo().getTranDate(), request.getTxnInfo().getTranTime(), transAuthId,
						new BigDecimal(request.getTxnInfo().getTranAmount()), dueDateRes, billingMonthRes,
						paymentRefrence, bankName, bankCode, branchName, branchCode, "", username, "");

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

		BigDecimal amountInDueToDate = null, amountAfterDate = null;
		String paymentRefrence = utilMethods.getRRN();

		String bankName = "", bankCode = "", branchName = "", branchCode = "";

		String billerNameRes = "", billInquiryCode = "", billInquiryDesc = "", dueDateRes = "", billingMonthRes = "",
				billStatusRes = "", tranAuthIdRes = "", amountRes = "", cnicRes = "", formattedDueDate = "";

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
					Constants.ACTIVITY.BillInquiry, BillerConstant.Thardeep.THARDEEP);

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

					///////////////////////////////

					PendingPayment pendingPayment = pendingPaymentRepository
							.findFirstByVoucherIdAndBillerIdOrderByPaymentIdDesc(
									request.getTxnInfo().getBillNumber().trim(),
									request.getTxnInfo().getBillerId().trim());

					if (pendingPayment != null) {

						if (pendingPayment.getIgnoreTimer()) {

							infoPay = new InfoPay(Constants.ResponseCodes.UNKNOWN_ERROR, pendingPaymentMessage, rrn,
									stan);
							response = new BillPaymentResponse(infoPay, null, null);
							transactionStatus = Constants.Status.Pending;
							return response;

						} else {
							LocalDateTime transactionDateTime = pendingPayment.getTransactionDate();
							LocalDateTime now = LocalDateTime.now(); // Current date and time

							// Calculate the difference in minutes
							long minutesDifference = Duration.between(transactionDateTime, now).toMinutes();

							if (minutesDifference <= pendingThresholdMinutes) {

								infoPay = new InfoPay(Constants.ResponseCodes.UNKNOWN_ERROR, pendingPaymentMessage, rrn,
										stan);
								response = new BillPaymentResponse(infoPay, null, null);

								transactionStatus = Constants.Status.Pending;
								return response;

							}
						}
					}

					LOG.info("Calling Payment Inquiry from pg_payment_log table");
					PgPaymentLog pgPaymentLog = pgPaymentLogRepository.findFirstByVoucherIdAndBillerIdAndBillStatus(
							request.getTxnInfo().getBillNumber(), request.getTxnInfo().getBillerId(),
							Constants.BILL_STATUS.BILL_PAID);

					if (pgPaymentLog != null
							&& pgPaymentLog.getTransactionStatus().equalsIgnoreCase(Constants.Status.Success)) {

						infoPay = new InfoPay(Constants.ResponseCodes.UNKNOWN_ERROR, pendingVoucherUpdateMessage, rrn,
								stan); // success

						transactionStatus = Constants.Status.Success;

						response = new BillPaymentResponse(infoPay, null, null);

						return response;

					}

					///////////////////////

					billerNameRes = thardeepgetVoucherResponse.getResponse().getThardeepGetVoucher().getConsumerName();
					billingMonthRes = thardeepgetVoucherResponse.getResponse().getThardeepGetVoucher()
							.getBillingMonth();
					billInquiryCode = thardeepgetVoucherResponse.getResponse().getThardeepGetVoucher()
							.getResponseCode();
					billInquiryDesc = thardeepgetVoucherResponse.getResponse().getThardeepGetVoucher()
							.getStatusResponse();
					billStatusRes = thardeepgetVoucherResponse.getResponse().getThardeepGetVoucher().getBillStatus();
					dueDateRes = thardeepgetVoucherResponse.getResponse().getThardeepGetVoucher().getDueDate();

					formattedDueDate = utilMethods.formatDueDate(dueDateRes);

					tranAuthIdRes = thardeepgetVoucherResponse.getResponse().getThardeepGetVoucher().getAuthId();
					amountRes = thardeepgetVoucherResponse.getResponse().getThardeepGetVoucher().getAmount();
					cnicRes = thardeepgetVoucherResponse.getResponse().getThardeepGetVoucher().getCnicNo();

					if (billStatusRes.equalsIgnoreCase(Constants.BILL_STATUS_SINGLE_ALPHABET.BILL_EXPIRED)) {

						infoPay = new InfoPay(Constants.ResponseCodes.INVALID_DATA,
								Constants.ResponseDescription.EXPIRED, rrn, stan);

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

					if (Double.valueOf(request.getTxnInfo().getTranAmount())
							.compareTo(Double.valueOf(amountRes)) != 0) {
						infoPay = new InfoPay(Constants.ResponseCodes.AMMOUNT_MISMATCH,
								Constants.ResponseDescription.AMMOUNT_MISMATCH, rrn, stan);
						response = new BillPaymentResponse(infoPay, null, null);
						return response;
					}

					thardeepUpdateVoucherResponse = serviceCaller.get(paymentParams,
							ThardeepUpdateVoucherResponse.class, rrn, Constants.ACTIVITY.BillPayment,
							BillerConstant.Thardeep.THARDEEP);

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

					infoPay = new InfoPay(Constants.ResponseCodes.SERVICE_FAIL,
							Constants.ResponseDescription.SERVICE_FAIL, rrn, stan);

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
						response.getInfo().getResponseCode(), response.getInfo().getResponseDesc(), billerNameRes,
						request.getTxnInfo().getBillNumber(), request.getTxnInfo().getBillerId(), amountInDueToDate,
						amountAfterDate, Constants.ACTIVITY.BillPayment, transactionStatus, channel,
						Constants.BILL_STATUS.BILL_PAID, request.getTxnInfo().getTranDate(),
						request.getTxnInfo().getTranTime(), transAuthId,
						new BigDecimal(request.getTxnInfo().getTranAmount()), formattedDueDate, billingMonthRes,
						paymentRefrence, bankName, bankCode, branchName, branchCode, tranAuthIdRes, username, "");

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

		BigDecimal amountInDueToDate = null, amountAfterDate = null, requestAmountWithinDueDate = null,
				requestAmountafterduedate = null, txnAmount = null;
		String amountWithInDueDateRes = "", amountAfterDueDateRes, billerNameRes = "", dueDateRes = "",
				billingMonthRes = "", billStatusRes = "";
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
					Constants.ACTIVITY.BillInquiry, BillerConstant.Uom.UOM);

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

				else if (uomgetVoucherResponse.getResponse().getResponseCode()
						.equalsIgnoreCase(Constants.ResponseCodes.BILL_EXPIRED)) {

					infoPay = new InfoPay(Constants.ResponseCodes.BILL_EXPIRED, Constants.ResponseDescription.EXPIRED,
							rrn, stan);

					response = new BillPaymentResponse(infoPay, null, null);

					return response;

				}

				else if (uomgetVoucherResponse.getResponse().getResponseCode()
						.equalsIgnoreCase(ResponseCodes.BILL_ALREADY_PAID)) {

					billerId = request.getTxnInfo().getBillerId();
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

				else if (uomgetVoucherResponse.getResponse().getResponseCode()
						.equalsIgnoreCase(Constants.ResponseCodes.OK)) {

					billerNameRes = uomgetVoucherResponse.getResponse().getUomgetvoucher().getConsumerDetail();
					billingMonthRes = uomgetVoucherResponse.getResponse().getUomgetvoucher().getBilling_Month();
					billStatusRes = uomgetVoucherResponse.getResponse().getUomgetvoucher().getBillStatus();
					dueDateRes = uomgetVoucherResponse.getResponse().getUomgetvoucher().getDueDate();
					amountWithInDueDateRes = uomgetVoucherResponse.getResponse().getUomgetvoucher()
							.getAmount_Within_DueDate();
					amountAfterDueDateRes = uomgetVoucherResponse.getResponse().getUomgetvoucher()
							.getAmount_After_DueDate();

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

						infoPay = new InfoPay(Constants.ResponseCodes.INVALID_DATA,
								Constants.ResponseDescription.EXPIRED, rrn, stan);

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
					paymentParams
							.add(utilMethods.formatAmountIso(Double.valueOf(request.getTxnInfo().getTranAmount()), 12));
					paymentParams.add(request.getTxnInfo().getTranDate().trim());
					paymentParams.add(request.getTxnInfo().getTranTime().trim());
					paymentParams.add(transAuthId);
					paymentParams.add(bankMnemonic);
					paymentParams.add(reserved);
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
							Constants.ACTIVITY.BillPayment, BillerConstant.Uom.UOM);

					if (uomUpdateVoucherResponse != null) {

						if (uomUpdateVoucherResponse.getResponse() == null) {

							infoPay = new InfoPay(Constants.ResponseCodes.SERVICE_FAIL,
									Constants.ResponseDescription.SERVICE_FAIL, rrn, stan);

							response = new BillPaymentResponse(infoPay, null, null);

							transactionStatus = Constants.Status.Fail;

							return response;

						}

						else if (uomUpdateVoucherResponse.getResponse().getResponse_code()
								.equalsIgnoreCase(Constants.ResponseCodes.CONSUMER_NUMBER_NOT_EXISTS)) {

							infoPay = new InfoPay(Constants.ResponseCodes.CONSUMER_NUMBER_NOT_EXISTS,
									Constants.ResponseDescription.CONSUMER_NUMBER_NOT_EXISTS, rrn, stan);

							response = new BillPaymentResponse(infoPay, null, null);

							return response;
						}

						else if (uomUpdateVoucherResponse.getResponse().getResponse_code()
								.equalsIgnoreCase(Constants.ResponseCodes.UNKNOWN_ERROR)) {

							infoPay = new InfoPay(uomUpdateVoucherResponse.getResponse().getResponse_code(),
									uomUpdateVoucherResponse.getResponse().getResponse_desc(), rrn, stan);

							response = new BillPaymentResponse(infoPay, null, null);

							return response;
						}

						else if (uomUpdateVoucherResponse.getResponse().getResponse_code()
								.equalsIgnoreCase(ResponseCodes.BILL_ALREADY_PAID)) {

							billerId = request.getTxnInfo().getBillerId();
							billerNumber = request.getTxnInfo().getBillNumber();

							infoPay = new InfoPay(uomUpdateVoucherResponse.getResponse().getResponse_code(),
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

						else if (uomUpdateVoucherResponse.getResponse().getResponse_code()
								.equalsIgnoreCase(Constants.ResponseCodes.OK)) {

							infoPay = new InfoPay(uomUpdateVoucherResponse.getResponse().getResponse_code(),
									Constants.ResponseDescription.OPERATION_SUCCESSFULL, rrn, stan);

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

					} else {

						infoPay = new InfoPay(Constants.ResponseCodes.SERVICE_FAIL,
								Constants.ResponseDescription.SERVICE_FAIL, rrn, stan);

						response = new BillPaymentResponse(infoPay, null, null);

						return response;

					}

				} else {

					infoPay = new InfoPay(Constants.ResponseCodes.SERVICE_FAIL,
							Constants.ResponseDescription.SERVICE_FAIL, rrn, stan);

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
						response.getInfo().getResponseCode(), response.getInfo().getResponseDesc(), billerNameRes,
						request.getTxnInfo().getBillNumber(), request.getTxnInfo().getBillerId(), amountInDueToDate,
						amountAfterDate, Constants.ACTIVITY.BillPayment, transactionStatus, channel,
						Constants.BILL_STATUS.BILL_PAID, request.getTxnInfo().getTranDate(),
						request.getTxnInfo().getTranTime(), transAuthId,
						new BigDecimal(request.getTxnInfo().getTranAmount()), String.valueOf(dueDate), billingMonthRes,
						paymentRefrence, bankName, bankCode, branchName, branchCode, "", username, "");

				LOG.info(" --- Bill Payment Method End --- ");

			} catch (Exception ex) {
				LOG.error("{Exception payment Logs}", ex);
			}

		}
		return response;
	}

	@Override
	public BillPaymentResponse billPaymentDls(BillPaymentRequest request, HttpServletRequest httpRequestData) {

		LOG.info("Inside billPayment Dls method ");

		BillPaymentResponse response = null;
		DlsGetVoucherResponse dlsgetVoucherResponse = null;
		Date requestedDate = new Date();
		DlsUpdateVoucherResponse dlsUpdateVoucherResponse = null;
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
		String requestname = "";
		String channel = "";
		String username = "";
		BigDecimal requestAmount = null, amountInDueDate = null;
		String bankName = "", bankCode = "", branchName = "", branchCode = "", status = "", feeDetail = "";
		double amountInDueToDate = 0;
		String amountAfterDueDate = "";
		BigDecimal amount = BigDecimal.ZERO;

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
			inquiryParams.add(Constants.MPAY_REQUEST_METHODS.DLS_BILL_INQUIRY);
			inquiryParams.add(request.getTxnInfo().getBillNumber().trim());
			inquiryParams.add(rrn);
			inquiryParams.add(stan);

			dlsgetVoucherResponse = serviceCaller.get(inquiryParams, DlsGetVoucherResponse.class, rrn,
					Constants.ACTIVITY.BillInquiry, BillerConstant.Dls.DLS);

			if (dlsgetVoucherResponse != null) {
				info = new Info(dlsgetVoucherResponse.getResponse().getResponseCode(),
						dlsgetVoucherResponse.getResponse().getResponseDesc(), rrn, stan);
				if (dlsgetVoucherResponse.getResponse().getResponseCode().equals(ResponseCodes.OK)) {

					feeDetail = utilMethods.formatFeeTypeList(dlsgetVoucherResponse);

					if (dlsgetVoucherResponse.getResponse().getDlsgetvoucher() != null) {

						requestAmount = new BigDecimal(dlsgetVoucherResponse.getResponse().getDlsgetvoucher()
								.getAmount().replaceFirst("^\\+?0+", ""));
						amountInDueDate = requestAmount.setScale(2, RoundingMode.UP);

						amountAfterDueDate = String.valueOf(amountInDueToDate);

						requestname = dlsgetVoucherResponse.getResponse().getDlsgetvoucher().getName();
						mobile = dlsgetVoucherResponse.getResponse().getDlsgetvoucher().getMobile_number();
						billStatus = dlsgetVoucherResponse.getResponse().getDlsgetvoucher().getStatus();

						billStatus = billStatus.trim().equals("1") ? Constants.BILL_STATUS.BILL_UNPAID
								: Constants.BILL_STATUS.BILL_PAID;

						dbTotal = amountInDueDate.doubleValue();
						dbAmount = amountInDueDate.doubleValue();

						if (Double.valueOf(request.getTxnInfo().getTranAmount()).compareTo(dbAmount) != 0) {
							infoPay = new InfoPay(Constants.ResponseCodes.AMMOUNT_MISMATCH,
									Constants.ResponseDescription.AMMOUNT_MISMATCH, rrn, stan);
							response = new BillPaymentResponse(infoPay, null, null);
							return response;
						}

						if (billStatus.equalsIgnoreCase(Constants.BILL_STATUS.BILL_UNPAID)) {

							////////////////////////////////////////////////////////

							PendingPayment pendingPayment = pendingPaymentRepository
									.findFirstByVoucherIdAndBillerIdOrderByPaymentIdDesc(
											request.getTxnInfo().getBillNumber().trim(),
											request.getTxnInfo().getBillerId().trim());

							if (pendingPayment != null) {

								if (pendingPayment.getIgnoreTimer()) {

									infoPay = new InfoPay(Constants.ResponseCodes.UNKNOWN_ERROR, pendingPaymentMessage,
											rrn, stan);
									response = new BillPaymentResponse(infoPay, null, null);
									transactionStatus = Constants.Status.Pending;
									billStatus = Constants.BILL_STATUS.BILL_PENDING;
									return response;

								} else {
									LocalDateTime transactionDateTime = pendingPayment.getTransactionDate();
									LocalDateTime now = LocalDateTime.now(); // Current date and time

									// Calculate the difference in minutes
									long minutesDifference = Duration.between(transactionDateTime, now).toMinutes();

									if (minutesDifference <= pendingThresholdMinutes) {

										infoPay = new InfoPay(Constants.ResponseCodes.UNKNOWN_ERROR,
												pendingPaymentMessage, rrn, stan);
										response = new BillPaymentResponse(infoPay, null, null);

										transactionStatus = Constants.Status.Pending;
										billStatus = Constants.BILL_STATUS.BILL_PENDING;
										return response;

									}
								}
							}

							LOG.info("Calling Payment Inquiry from pg_payment_log table");
							PgPaymentLog pgPaymentLog = pgPaymentLogRepository
									.findFirstByVoucherIdAndBillerIdAndBillStatus(request.getTxnInfo().getBillNumber(),
											request.getTxnInfo().getBillerId(), Constants.BILL_STATUS.BILL_PAID);

							if (pgPaymentLog != null
									&& pgPaymentLog.getTransactionStatus().equalsIgnoreCase(Constants.Status.Success)) {

								infoPay = new InfoPay(Constants.ResponseCodes.UNKNOWN_ERROR,
										pendingVoucherUpdateMessage, rrn, stan); // success

								transactionStatus = Constants.Status.Success;
								billStatus = Constants.BILL_STATUS.BILL_PAID;

								response = new BillPaymentResponse(infoPay, null, null);

								return response;
							}

							////////////////////////////////////////////////////////

							LOG.info("Calling UpdateVoucher ");
							ArrayList<String> ubpsBillParams = new ArrayList<>();

							ubpsBillParams.add(Constants.MPAY_REQUEST_METHODS.DLS_BILL_PAYMENT);
							ubpsBillParams.add(request.getTxnInfo().getBillNumber());
							ubpsBillParams.add(rrn);
							ubpsBillParams.add(stan);

							dlsUpdateVoucherResponse = serviceCaller.get(ubpsBillParams, DlsUpdateVoucherResponse.class,
									rrn, Constants.ACTIVITY.BillPayment, BillerConstant.Dls.DLS);

							if (dlsUpdateVoucherResponse != null) {
								infoPay = new InfoPay(dlsUpdateVoucherResponse.getResponse().getResponseCode(),
										dlsUpdateVoucherResponse.getResponse().getResponseDesc(), rrn, stan);
								if (dlsUpdateVoucherResponse.getResponse().getResponseCode().equals(ResponseCodes.OK)) {
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

								} else {
									infoPay = new InfoPay(Constants.ResponseCodes.UNKNOWN_ERROR,
											Constants.ResponseDescription.UNKNOWN_ERROR, rrn, stan);

									response = new BillPaymentResponse(infoPay, null, null);
								}

							}

							else {
								infoPay = new InfoPay(Constants.ResponseCodes.SERVICE_FAIL,
										Constants.ResponseDescription.SERVICE_FAIL, rrn, stan);

								response = new BillPaymentResponse(infoPay, null, null);
							}

						}

					}

					else {
						infoPay = new InfoPay(Constants.ResponseCodes.UNKNOWN_ERROR,
								Constants.ResponseDescription.UNKNOWN_ERROR, rrn, stan);

						response = new BillPaymentResponse(infoPay, null, null);

						transactionStatus = Constants.Status.Fail;

						LOG.info("Calling Bill payment End");
					}
				}

				else if (dlsgetVoucherResponse.getResponse().getResponseCode()
						.equals(Constants.ResponseCodes.BILL_ALREADY_PAID)) {
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
				}

				else if (dlsgetVoucherResponse.getResponse().getResponseCode()
						.equals(Constants.ResponseCodes.CONSUMER_NUMBER_NOT_EXISTS)) {
					infoPay = new InfoPay(Constants.ResponseCodes.CONSUMER_NUMBER_NOT_EXISTS,
							Constants.ResponseDescription.CONSUMER_NUMBER_NOT_EXISTS, rrn, stan);
					response = new BillPaymentResponse(infoPay, null, null);
					transactionStatus = Constants.Status.Fail;

				}

				else if (dlsgetVoucherResponse.getResponse().getResponseCode()
						.equals(Constants.ResponseCodes.CONSUMER_NUMBER_BLOCK)) {
					infoPay = new InfoPay(Constants.ResponseCodes.CONSUMER_NUMBER_BLOCK,
							Constants.ResponseDescription.CONSUMER_NUMBER_BLOCK, rrn, stan);
					response = new BillPaymentResponse(infoPay, null, null);
					status = "B";
					transactionStatus = Constants.Status.Fail;
				}

				else if (dlsgetVoucherResponse.getResponse().getResponseCode()
						.equals(Constants.ResponseCodes.UNKNOWN_ERROR)) {
					infoPay = new InfoPay(Constants.ResponseCodes.UNKNOWN_ERROR,
							Constants.ResponseDescription.UNKNOWN_ERROR, rrn, stan);
					response = new BillPaymentResponse(infoPay, null, null);
					transactionStatus = Constants.Status.Fail;
				}

				else if (dlsgetVoucherResponse.getResponse().getResponseCode()
						.equals(Constants.ResponseCodes.INVALID_DATA)) {
					infoPay = new InfoPay(Constants.ResponseCodes.INVALID_DATA,
							Constants.ResponseDescription.INVALID_DATA, rrn, stan);
					response = new BillPaymentResponse(infoPay, null, null);
					transactionStatus = Constants.Status.Fail;
				}

				else if (dlsgetVoucherResponse.getResponse().getResponseCode()
						.equals(Constants.ResponseCodes.SERVICE_FAIL)) {
					infoPay = new InfoPay(Constants.ResponseCodes.SERVICE_FAIL,
							Constants.ResponseDescription.SERVICE_FAIL, rrn, stan);
					response = new BillPaymentResponse(infoPay, null, null);
					transactionStatus = Constants.Status.Fail;
				}

				else {
					infoPay = new InfoPay(Constants.ResponseCodes.UNKNOWN_ERROR,
							Constants.ResponseDescription.UNKNOWN_ERROR, rrn, stan);
					response = new BillPaymentResponse(infoPay, null, null);

				}
			}

			else {
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

				PaymentLog savedPaymentLog = paymentLoggingService.paymentLog(requestedDate, new Date(), rrn, stan,
						response.getInfo().getResponseCode(), response.getInfo().getResponseDesc(), cnic,
						request.getTerminalInfo().getMobile(), requestname, request.getTxnInfo().getBillNumber(),
						request.getTxnInfo().getBillerId(), amountInDueDate == null ? amount : amountInDueDate,
						amountInDueDate == null ? amount : amountInDueDate,
						amountAfterDueDate == null || amountAfterDueDate.trim().equalsIgnoreCase("") ? amount
								: new BigDecimal(amountAfterDueDate),
						dbTransactionFees, Constants.ACTIVITY.BillPayment, paymentRefrence,
						request.getTxnInfo().getBillNumber(), transactionStatus, address, dbTotal, channel, billStatus,
						request.getTxnInfo().getTranDate(), request.getTxnInfo().getTranTime(), province, transAuthId,
						bankName, bankCode, branchName, branchCode, username, feeDetail);

				if (dlsgetVoucherResponse != null && dlsgetVoucherResponse.getResponse() != null
						&& dlsgetVoucherResponse.getResponse().getDlsgetvoucher() != null
						&& dlsgetVoucherResponse.getResponse().getDlsgetvoucher().getFeeTypesList_wrapper() != null) {

					if (infoPay.getResponseCode().equals(ResponseCodes.OK)
							&& billStatus.equalsIgnoreCase(Constants.BILL_STATUS.BILL_PAID)) {
						List<? extends BillerDetails> billerDetailsList = dlsgetVoucherResponse.getResponse()
								.getDlsgetvoucher().getFeeTypesList_wrapper();

						FeeType[] feeDetails = feeTypeMapper.mapFeeTypeListToArray(billerDetailsList);

						if (savedPaymentLog != null) {
							for (FeeType feeType : feeDetails) {
								feeType.setPaymentLogId(savedPaymentLog.getID());
								feeType.setSource(paymentLogTable);
								feeTypeRepository.save(feeType);

								LOG.info("Saved FeeType {} associated with PaymentLog ID {}", feeType.getId(),
										savedPaymentLog.getID());

							}
						} else {
							LOG.error(
									"Failed to saved FeeDetails because paymentLog is null. Cannot perform operation.");
						}
					}

				}

			} catch (Exception ex) {
				LOG.error("{}", ex);
			}
		}

		UtilMethods.generalLog("OUT -  Bill Payment Response {}" + response, LOG);

		LOG.info("----- Bill Payment Method End -----");

		return response;

	}

	@Override
	public BillPaymentResponse billPaymentBzu(BillPaymentRequest request, HttpServletRequest httpRequestData) {

		LOG.info("Bzu Bill Payment Request {} ", request.toString());

		BillPaymentResponse response = null;
		BzuGetVoucherResponse bzugetVoucherResponse = null;
		BzuUpdateVoucherResponse bzuUpdateVoucherResponse = null;
		Date requestedDate = new Date();
		InfoPay infoPay = null;
		TxnInfoPay txnInfoPay = null;
		AdditionalInfoPay additionalInfoPay = null;
		String transactionStatus = "";
		String rrn = request.getInfo().getRrn(); // utilMethods.getRRN();

		LOG.info("RRN :{ }", rrn);
		String stan = request.getInfo().getStan();
		String transAuthId = request.getTxnInfo().getTranAuthId();
		String channel = "", username = "";

		ArrayList<String> inquiryParams = new ArrayList<String>();
		ArrayList<String> paymentParams = new ArrayList<String>();

		BigDecimal amountInDueToDate = null, amountAfterDate = null, requestAmountafterduedate = null, txnAmount = null;
		String amountWithInDueDate = "", studentName = "", dueDate = "", billstatus = "", fatherName = "",
				billStatus = "";
		String billerId = "", billerNumber = "";
		String paymentRefrence = utilMethods.getRRN();
		String bankName = "", bankCode = "", branchName = "", branchCode = "";
		LocalDate localDate;

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

			inquiryParams.add(Constants.MPAY_REQUEST_METHODS.BZU_BILL_INQUIRY);
			inquiryParams.add(request.getTxnInfo().getBillNumber().trim());
			inquiryParams.add(bzu_username);
			inquiryParams.add(bzu_password);
			inquiryParams.add(rrn);
			inquiryParams.add(stan);

			bzugetVoucherResponse = serviceCaller.get(inquiryParams, BzuGetVoucherResponse.class, rrn,
					Constants.ACTIVITY.BillInquiry, BillerConstant.BZU.BZU);

			if (bzugetVoucherResponse != null) {

				if (bzugetVoucherResponse.getResponse() == null) {

					infoPay = new InfoPay(Constants.ResponseCodes.SERVICE_FAIL,
							Constants.ResponseDescription.SERVICE_FAIL, rrn, stan);

					response = new BillPaymentResponse(infoPay, null, null);

					transactionStatus = Constants.Status.Fail;

					return response;

				}

				else if (bzugetVoucherResponse.getResponse().getResponseCode()
						.equalsIgnoreCase(Constants.ResponseCodes.CONSUMER_NUMBER_NOT_EXISTS)) {

					infoPay = new InfoPay(Constants.ResponseCodes.CONSUMER_NUMBER_NOT_EXISTS,
							Constants.ResponseDescription.CONSUMER_NUMBER_NOT_EXISTS, rrn, stan);

					response = new BillPaymentResponse(infoPay, null, null);

					return response;
				}

				else if (bzugetVoucherResponse.getResponse().getResponseCode()
						.equalsIgnoreCase(ResponseCodes.BILL_ALREADY_PAID)) {

					billerId = request.getTxnInfo().getBillerId();
					billerNumber = request.getTxnInfo().getBillNumber();

					infoPay = new InfoPay(bzugetVoucherResponse.getResponse().getResponseCode(),
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

					billStatus = "Paid";

					response = new BillPaymentResponse(infoPay, txnInfoPay, additionalInfoPay);
					return response;

				}

				else if (bzugetVoucherResponse.getResponse().getResponseCode()
						.equalsIgnoreCase(Constants.ResponseCodes.OK)) {

					studentName = bzugetVoucherResponse.getResponse().getBzugetVoucher().getVoucher().getStudentName();
					fatherName = bzugetVoucherResponse.getResponse().getBzugetVoucher().getVoucher().getStudentFname();
					dueDate = utilMethods.transactionDateFormater(
							bzugetVoucherResponse.getResponse().getBzugetVoucher().getVoucher().getDueDate());
					billstatus = "U";
					amountInDueToDate = new BigDecimal(
							bzugetVoucherResponse.getResponse().getBzugetVoucher().getVoucher().getTotalFees());
					amountInDueToDate = amountInDueToDate.setScale(2, RoundingMode.UP);
					billStatus = "Unpaid";

					paymentParams.add(Constants.MPAY_REQUEST_METHODS.BZU_BILL_PAYMENT);
					paymentParams.add(request.getTxnInfo().getBillNumber().trim());
					paymentParams.add(bzu_username);
					paymentParams.add(bzu_password);
					paymentParams.add(transAuthId);
					paymentParams.add(rrn);
					paymentParams.add(stan);

					txnAmount = new BigDecimal(request.getTxnInfo().getTranAmount());

					if (utilMethods.isValidInput(dueDate)) {

						try {

							if (txnAmount.compareTo(amountInDueToDate) != 0) {
								infoPay = new InfoPay(Constants.ResponseCodes.AMMOUNT_MISMATCH,
										Constants.ResponseDescription.AMMOUNT_MISMATCH, rrn, stan);
								response = new BillPaymentResponse(infoPay, null, null);
								return response;
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

					bzuUpdateVoucherResponse = serviceCaller.get(paymentParams, BzuUpdateVoucherResponse.class, rrn,
							Constants.ACTIVITY.BillPayment, BillerConstant.BZU.BZU);

					if (bzuUpdateVoucherResponse != null) {

						if (bzuUpdateVoucherResponse.getResponse() == null) {

							infoPay = new InfoPay(Constants.ResponseCodes.SERVICE_FAIL,
									Constants.ResponseDescription.SERVICE_FAIL, rrn, stan);

							response = new BillPaymentResponse(infoPay, null, null);

							transactionStatus = Constants.Status.Fail;

							return response;

						}

						else if (bzuUpdateVoucherResponse.getResponse().getResponseCode()
								.equalsIgnoreCase(Constants.ResponseCodes.OK)) {

							infoPay = new InfoPay(Constants.ResponseCodes.OK,
									Constants.ResponseDescription.OPERATION_SUCCESSFULL, rrn, stan);

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

							billStatus = "Paid";

							return response;

						}

					} else {

						infoPay = new InfoPay(Constants.ResponseCodes.UNKNOWN_ERROR,
								Constants.ResponseDescription.UNKNOWN_ERROR, rrn, stan);

						response = new BillPaymentResponse(infoPay, null, null);

						return response;

					}

				}

				else {

					infoPay = new InfoPay(Constants.ResponseCodes.UNKNOWN_ERROR,
							Constants.ResponseDescription.UNKNOWN_ERROR, rrn, stan);

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
						response.getInfo().getResponseCode(), response.getInfo().getResponseDesc(), studentName,
						request.getTxnInfo().getBillNumber(), request.getTxnInfo().getBillerId(), amountInDueToDate,
						amountAfterDate, Constants.ACTIVITY.BillPayment, transactionStatus, channel, billStatus,
						request.getTxnInfo().getTranDate(), request.getTxnInfo().getTranTime(), transAuthId,
						new BigDecimal(request.getTxnInfo().getTranAmount()), String.valueOf(dueDate), "",
						paymentRefrence, bankName, bankCode, branchName, branchCode, "", username, "");

				LOG.info(" --- Bill Payment Method End --- ");

			} catch (Exception ex) {
				LOG.error("{Exception payment Logs}", ex);
			}

		}
		return response;
	}

	@Override
	public BillPaymentResponse billPaymentSlic(BillPaymentRequest request, HttpServletRequest httpRequestData) {

		LOG.info("Slic Bill Payment Request {} ", request.toString());

		BillPaymentResponse response = null;
		UpdateSlicPolicyTranslationResponse updateSlicPolicyTranslationResponse = null;
		SlicPolicyInquiryResponse slicPolicyInquiryResponse = null;
		Date requestedDate = new Date();
		InfoPay infoPay = null;
		TxnInfoPay txnInfoPay = null;
		AdditionalInfoPay additionalInfoPay = null;
		String rrn = request.getInfo().getRrn(); // utilMethods.getRRN();
		String paymentRefrence = utilMethods.getRRN();
		String stan = request.getInfo().getStan();
		LOG.info("RRN :{ }", rrn);
		String transAuthId = request.getTxnInfo().getTranAuthId();
		String channel = "", username = "";

		ArrayList<String> inquiryParams = new ArrayList<String>();
		ArrayList<String> paymentParams = new ArrayList<String>();

		BigDecimal amountInDueToDate = null, txnAmount = null;
		String billStatus = "", transactionStatus = "", billerId = "", billerNumber = "", billerName = "",
				due_Date = "";
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

			inquiryParams.add(Constants.MPAY_REQUEST_METHODS.SLIC_BILL_INQUIRY);
			inquiryParams.add(request.getTxnInfo().getBillNumber().trim());
			inquiryParams.add(rrn); /// BatchTransID
			inquiryParams.add(rrn);
			inquiryParams.add(stan);

			//// Inquiry Call to M-Pay

			slicPolicyInquiryResponse = serviceCaller.get(inquiryParams, SlicPolicyInquiryResponse.class, rrn,
					Constants.ACTIVITY.BillInquiry, BillerConstant.SLIC.SLIC);

			if (slicPolicyInquiryResponse != null) {

				if (slicPolicyInquiryResponse.getSlicResponse() == null) {

					infoPay = new InfoPay(Constants.ResponseCodes.SERVICE_FAIL,
							Constants.ResponseDescription.SERVICE_FAIL, rrn, stan);

					response = new BillPaymentResponse(infoPay, null, null);

					transactionStatus = Constants.Status.Fail;

					return response;

				}

				//// consumer number not exsist

				else if (slicPolicyInquiryResponse.getSlicResponse().getResponseCode()
						.equalsIgnoreCase(Constants.ResponseCodes.CONSUMER_NUMBER_NOT_EXISTS)) {

					infoPay = new InfoPay(Constants.ResponseCodes.CONSUMER_NUMBER_NOT_EXISTS,
							Constants.ResponseDescription.CONSUMER_NUMBER_NOT_EXISTS, rrn, stan);

					response = new BillPaymentResponse(infoPay, null, null);

					return response;
				}

				//// Already Paid

				else if (slicPolicyInquiryResponse.getSlicResponse().getResponseCode()
						.equalsIgnoreCase(Constants.ResponseCodes.BILL_ALREADY_PAID)) {

					billerId = request.getTxnInfo().getBillerId();
					billerNumber = request.getTxnInfo().getBillNumber();

					infoPay = new InfoPay(Constants.ResponseCodes.BILL_ALREADY_PAID,
							Constants.ResponseDescription.BILL_ALREADY_PAID, rrn, stan);

					txnInfoPay = new TxnInfoPay(billerId, billerNumber, paymentRefrence);

					additionalInfoPay = new AdditionalInfoPay("", request.getAdditionalInfo().getReserveField2(),
							request.getAdditionalInfo().getReserveField3(),
							request.getAdditionalInfo().getReserveField4(),
							request.getAdditionalInfo().getReserveField5(),
							request.getAdditionalInfo().getReserveField6(),
							request.getAdditionalInfo().getReserveField7(),
							request.getAdditionalInfo().getReserveField8(),
							request.getAdditionalInfo().getReserveField9(),
							request.getAdditionalInfo().getReserveField10());

					transactionStatus = Constants.Status.Success;

					billStatus = Constants.BILL_STATUS.BILL_PAID;

					response = new BillPaymentResponse(infoPay, txnInfoPay, additionalInfoPay);
					return response;

				}

				/////// Inquiry success response

				else if (slicPolicyInquiryResponse.getSlicResponse().getResponseCode()
						.equalsIgnoreCase(Constants.ResponseCodes.OK)) {

					PendingPayment pendingPayment = pendingPaymentRepository
							.findFirstByVoucherIdAndBillerIdOrderByPaymentIdDesc(
									request.getTxnInfo().getBillNumber().trim(),
									request.getTxnInfo().getBillerId().trim());

					if (pendingPayment != null) {

						if (pendingPayment.getIgnoreTimer()) {

							infoPay = new InfoPay(Constants.ResponseCodes.UNKNOWN_ERROR, pendingPaymentMessage, rrn,
									stan);
							response = new BillPaymentResponse(infoPay, null, null);
							transactionStatus = Constants.Status.Pending;
							billStatus = Constants.BILL_STATUS.BILL_PENDING;
							return response;

						} else {
							LocalDateTime transactionDateTime = pendingPayment.getTransactionDate();
							LocalDateTime now = LocalDateTime.now(); // Current date and time

							// Calculate the difference in minutes
							long minutesDifference = Duration.between(transactionDateTime, now).toMinutes();

							if (minutesDifference <= pendingThresholdMinutes) {

								infoPay = new InfoPay(Constants.ResponseCodes.UNKNOWN_ERROR, pendingPaymentMessage, rrn,
										stan);
								response = new BillPaymentResponse(infoPay, null, null);

								transactionStatus = Constants.Status.Pending;
								billStatus = Constants.BILL_STATUS.BILL_PENDING;
								return response;

							}
						}
					}

					LOG.info("Calling Payment Inquiry from pg_payment_log table");
					PgPaymentLog pgPaymentLog = pgPaymentLogRepository.findFirstByVoucherIdAndBillerIdAndBillStatus(
							request.getTxnInfo().getBillNumber(), request.getTxnInfo().getBillerId(),
							Constants.BILL_STATUS.BILL_PAID);

					if (pgPaymentLog != null
							&& pgPaymentLog.getTransactionStatus().equalsIgnoreCase(Constants.Status.Success)) {

						infoPay = new InfoPay(Constants.ResponseCodes.UNKNOWN_ERROR, pendingVoucherUpdateMessage, rrn,
								stan); // success

						transactionStatus = Constants.Status.Success;
						billStatus = Constants.BILL_STATUS.BILL_PAID;

						response = new BillPaymentResponse(infoPay, null, null);

						return response;
					}

					///// Loan

					if (request.getTxnInfo().getBillerId().equals(billerIdLoan)) {

						amountInDueToDate = new BigDecimal(slicPolicyInquiryResponse.getSlicResponse()
								.getSlicPolicyInquiry().getResultWrapper().get(0).getDueAmt());
						amountInDueToDate = amountInDueToDate.setScale(2, RoundingMode.UP);

						if (amountInDueToDate.compareTo(BigDecimal.ZERO) == 0) {

							Optional<CombinedPaymentLogView> combinedPaymentLogViewCheck = Optional
									.ofNullable(combinedPaymentLogViewRepository
											.findFirstByBillerNumberAndBillStatusAndActivitiesBillerIdOrderByRequestDateTimeDesc(
													request.getTxnInfo().getBillNumber().trim(),
													Constants.BILL_STATUS.BILL_PAID, Constants.ACTIVITY.BillPayment,
													Constants.ACTIVITY.RBTS_FUND_TRANSFER,
													Constants.ACTIVITY.CREDIT_DEBIT_CARD,
													request.getTxnInfo().getBillerId()));

							if (combinedPaymentLogViewCheck.isPresent()) {

								billerId = request.getTxnInfo().getBillerId();
								billerNumber = request.getTxnInfo().getBillNumber();

								infoPay = new InfoPay(Constants.ResponseCodes.BILL_ALREADY_PAID,
										Constants.ResponseDescription.BILL_ALREADY_PAID, rrn, stan);

								txnInfoPay = new TxnInfoPay(billerId, billerNumber, paymentRefrence);

								additionalInfoPay = new AdditionalInfoPay("",
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

								billStatus = Constants.BILL_STATUS.BILL_PAID;

								response = new BillPaymentResponse(infoPay, txnInfoPay, additionalInfoPay);
								return response;
							}

						}

					}

					///// Premium

					else if (request.getTxnInfo().getBillerId().equals(billerIdPremium)) {

						amountInDueToDate = new BigDecimal(slicPolicyInquiryResponse.getSlicResponse()
								.getSlicPolicyInquiry().getResultWrapper().get(1).getDueAmt());
						amountInDueToDate = amountInDueToDate.setScale(2, RoundingMode.UP);

						if (amountInDueToDate.compareTo(BigDecimal.ZERO) == 0) {

							Optional<CombinedPaymentLogView> combinedPaymentLogViewCheck = Optional
									.ofNullable(combinedPaymentLogViewRepository
											.findFirstByBillerNumberAndBillStatusAndActivitiesBillerIdOrderByRequestDateTimeDesc(
													request.getTxnInfo().getBillNumber().trim(),
													Constants.BILL_STATUS.BILL_PAID, Constants.ACTIVITY.BillPayment,
													Constants.ACTIVITY.RBTS_FUND_TRANSFER,
													Constants.ACTIVITY.CREDIT_DEBIT_CARD,
													request.getTxnInfo().getBillerId()));

							if (combinedPaymentLogViewCheck.isPresent()) {

								billerId = request.getTxnInfo().getBillerId();
								billerNumber = request.getTxnInfo().getBillNumber();

								infoPay = new InfoPay(Constants.ResponseCodes.BILL_ALREADY_PAID,
										Constants.ResponseDescription.BILL_ALREADY_PAID, rrn, stan);

								txnInfoPay = new TxnInfoPay(billerId, billerNumber, paymentRefrence);

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

								transactionStatus = Constants.Status.Success;

								billStatus = Constants.BILL_STATUS.BILL_PAID;

								response = new BillPaymentResponse(infoPay, txnInfoPay, additionalInfoPay);
								return response;
							}

						}

					}

					paymentParams.add(Constants.MPAY_REQUEST_METHODS.SLIC_BILL_PAYMENT);
					paymentParams.add(request.getTxnInfo().getBillNumber().trim());
					paymentParams.add(request.getInfo().getRrn());
					paymentParams.add(request.getTxnInfo().getTranAmount());
					paymentParams.add(request.getAdditionalInfo().getReserveField1());
					paymentParams.add(rrn);
					paymentParams.add(stan);

					billerName = slicPolicyInquiryResponse.getSlicResponse().getSlicPolicyInquiry().getPolicy_holder();
					due_Date = utilMethods
							.DueDate(slicPolicyInquiryResponse.getSlicResponse().getSlicPolicyInquiry().getDue_date());

					txnAmount = new BigDecimal(request.getTxnInfo().getTranAmount());

					try {

						if (txnAmount.compareTo(amountInDueToDate) != 0) {
							infoPay = new InfoPay(Constants.ResponseCodes.AMMOUNT_MISMATCH,
									Constants.ResponseDescription.AMMOUNT_MISMATCH, rrn, stan);
							response = new BillPaymentResponse(infoPay, null, null);
							return response;
						}

					} catch (DateTimeParseException e) {
						LOG.error("Error parsing due date: " + e.getMessage());
					}

					//// M-Pay call to Payment

					updateSlicPolicyTranslationResponse = serviceCaller.get(paymentParams,
							UpdateSlicPolicyTranslationResponse.class, rrn, Constants.ACTIVITY.BillPayment,
							BillerConstant.SLIC.SLIC);

					if (updateSlicPolicyTranslationResponse != null) {

						if (updateSlicPolicyTranslationResponse.getSlicResponse() == null) {

							infoPay = new InfoPay(Constants.ResponseCodes.SERVICE_FAIL,
									Constants.ResponseDescription.SERVICE_FAIL, rrn, stan);

							response = new BillPaymentResponse(infoPay, null, null);

							transactionStatus = Constants.Status.Fail;

							return response;

						}

						else if (updateSlicPolicyTranslationResponse.getSlicResponse().getResponseCode()
								.equalsIgnoreCase(Constants.ResponseCodes.OK)) {

							infoPay = new InfoPay(Constants.ResponseCodes.OK,
									Constants.ResponseDescription.OPERATION_SUCCESSFULL, rrn, stan);

							txnInfoPay = new TxnInfoPay(request.getTxnInfo().getBillerId(),
									request.getTxnInfo().getBillNumber(), paymentRefrence);

							additionalInfoPay = new AdditionalInfoPay("",
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

							return response;

						}

						else {

							infoPay = new InfoPay(Constants.ResponseCodes.UNKNOWN_ERROR,
									Constants.ResponseDescription.UNKNOWN_ERROR, rrn, stan);
							response = new BillPaymentResponse(infoPay, null, null);
							transactionStatus = Constants.Status.Fail;
						}

					} else {

						infoPay = new InfoPay(Constants.ResponseCodes.SERVICE_FAIL,
								Constants.ResponseDescription.SERVICE_FAIL, rrn, stan);

						response = new BillPaymentResponse(infoPay, null, null);

						transactionStatus = Constants.Status.Fail;

						return response;

					}

				}

				else {

					infoPay = new InfoPay(Constants.ResponseCodes.UNKNOWN_ERROR,
							Constants.ResponseDescription.UNKNOWN_ERROR, rrn, stan);

					response = new BillPaymentResponse(infoPay, null, null);

					transactionStatus = Constants.Status.Fail;

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

		catch (

		Exception e) {

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
						response.getInfo().getResponseCode(), response.getInfo().getResponseDesc(), billerName,
						request.getTxnInfo().getBillNumber(), request.getTxnInfo().getBillerId(), amountInDueToDate,
						null, Constants.ACTIVITY.BillPayment, transactionStatus, channel, billStatus,
						request.getTxnInfo().getTranDate(), request.getTxnInfo().getTranTime(), transAuthId,
						new BigDecimal(request.getTxnInfo().getTranAmount()), due_Date, "", paymentRefrence, bankName,
						bankCode, branchName, branchCode, "", username, "");

				LOG.info(" --- Bill Payment Method End --- ");

			} catch (Exception ex) {
				LOG.error("{Exception payment Logs}", ex);
			}

		}
		return response;
	}

	@Override
	public BillPaymentResponse billPaymentBppra(BillPaymentRequest request, HttpServletRequest httpRequestData) {

		LOG.info("Bppra Bill Payment Request {} ", request.toString());

		BillPaymentResponse response = null;
		BppraTenderVoucherResponse bppraTenderVoucherResponse = null;
		BppraSupplierVoucherResponse bppraSupplierVoucherResponse = null;
		Date requestedDate = new Date();
		InfoPay infoPay = null;
		TxnInfoPay txnInfoPay = null;
		AdditionalInfoPay additionalInfoPay = null;
		String transactionStatus = "";
		String rrn = request.getInfo().getRrn(); // utilMethods.getRRN();

		LOG.info("RRN :{ }", rrn);
		String stan = request.getInfo().getStan();
		String transAuthId = request.getTxnInfo().getTranAuthId();
		String channel = "", username = "";

		BigDecimal totalTenderFeeAmount = null, txnAmount = null;
		String billerName = "", billStatus = "", encryptedChallandata = "", decryptData = "";
		String billerId = "", billerNumber = "";
		String paymentRefrence = utilMethods.getRRN();
		String bankName = "", bankCode = "", branchName = "", branchCode = "", challanFeeData = "", publicKey,
				decodeJwt = "", keyAndIv = "", jwt = "";
		List<ChallanFee> challanFees = null;
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

			// Setup keys
			try {
				rsaUtility.SetRSAEncryption();
			} catch (NoSuchAlgorithmException e) {
				LOG.info("BillPayment - keys - NoSuchAlgorithmException:" + e.getMessage());
			} catch (InvalidKeySpecException e) {
				LOG.info("BillPayment - keys - InvalidKeySpecException:" + e.getMessage());
			} catch (IOException e) {
				LOG.info("BillPayment - keys - IOException:" + e.getMessage());
			}

			billerNumber = request.getTxnInfo().getBillNumber();
			jwt = memcachedService.get(request.getAdditionalInfo().getReserveField2());
			if (jwt == null) {
				LOG.info("JWT is null — possibly due to an incorrect key");

				infoPay = new InfoPay(Constants.ResponseCodes.UNKNOWN_ERROR,
						Constants.ResponseDescription.UNKNOWN_ERROR, rrn, stan);

				response = new BillPaymentResponse(infoPay, null, null);
				transactionStatus = Constants.Status.Fail;
				return response;
			}
			LOG.info("jwt :" + jwt);

			//////// Tender //////

			if (billerNumber.startsWith(tenderPrefix)) {

				///// TenderAuth Call
				HttpResponse<String> tenderAuthResponse = utilMethods.authRequest(bppraAuthticateCall,
						bppraClientSecret);

				//// TenderAuth Failure
				if (tenderAuthResponse == null) {

					infoPay = new InfoPay(Constants.ResponseCodes.SERVICE_FAIL,
							Constants.ResponseDescription.SERVICE_FAIL, rrn, stan);

					response = new BillPaymentResponse(infoPay, null, null);
					transactionStatus = Constants.Status.Fail;
					return response;

				}

				///// TenderAuth Success
				if (tenderAuthResponse.getStatus() == 200) {

					String authToken = tenderAuthResponse.getBody();
					LOG.info("authToken" + authToken);

					publicKey = rsaUtility.getPublicKey();

					decodeJwt = utilMethods.DecodeJwt(jwt);
					keyAndIv = rsaUtility.RSADecrypt(decodeJwt);

					////// TenderChallanInquiry
					HttpResponse<String> tenderChallanInquiry = utilMethods.tenderChallanInquiryRequest(
							bppraChallanInquiryCall, request.getTxnInfo().getBillNumber(), requestFormChallanEnquire,
							jwt);

					//// TenderChallanInquiry Failure
					if (tenderChallanInquiry == null) {

						infoPay = new InfoPay(Constants.ResponseCodes.SERVICE_FAIL,
								Constants.ResponseDescription.SERVICE_FAIL, rrn, stan);

						response = new BillPaymentResponse(infoPay, null, null);
						transactionStatus = Constants.Status.Fail;
						return response;

					}

					/////// TenderChallanInquiry Success
					if (tenderChallanInquiry.getStatus() == 200) {

						LOG.info("TenderChallanInquiry : Success ");

						encryptedChallandata = tenderChallanInquiry.getBody();
						decryptData = utilMethods.aesPackedAlgorithm(keyAndIv, encryptedChallandata);

						bppraTenderVoucherResponse = mapper.readValue(decryptData, BppraTenderVoucherResponse.class);
						LOG.info("BppraTenderVoucherResponse : " + bppraTenderVoucherResponse);

						billerName = bppraTenderVoucherResponse.getTenderChallanData().getPaName();

						//////// status is Paid (True) ////

						if (bppraTenderVoucherResponse.getTenderChallanData().isPaidStatus()) {

							billerId = request.getTxnInfo().getBillerId();
							billerNumber = request.getTxnInfo().getBillNumber();

							infoPay = new InfoPay(Constants.ResponseCodes.BILL_ALREADY_PAID,
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

							billStatus = Constants.BILL_STATUS.BILL_PAID;

							response = new BillPaymentResponse(infoPay, txnInfoPay, additionalInfoPay);
							return response;

						}

						////// Status is Unpaid(False) /////

						else {

							challanFees = bppraTenderVoucherResponse.getChallanFee();
							challanFeeData = objectMapper.writeValueAsString(challanFees);

							PendingPayment pendingPayment = pendingPaymentRepository
									.findFirstByVoucherIdAndBillerIdOrderByPaymentIdDesc(
											request.getTxnInfo().getBillNumber().trim(),
											request.getTxnInfo().getBillerId().trim());

							if (pendingPayment != null) {

								if (pendingPayment.getIgnoreTimer()) {

									infoPay = new InfoPay(Constants.ResponseCodes.UNKNOWN_ERROR, pendingPaymentMessage,
											rrn, stan);
									response = new BillPaymentResponse(infoPay, null, null);
									transactionStatus = Constants.Status.Pending;
									billStatus = Constants.BILL_STATUS.BILL_PENDING;
									return response;

								} else {
									LocalDateTime transactionDateTime = pendingPayment.getTransactionDate();
									LocalDateTime now = LocalDateTime.now(); // Current date and time

									// Calculate the difference in minutes
									long minutesDifference = Duration.between(transactionDateTime, now).toMinutes();

									if (minutesDifference <= pendingThresholdMinutes) {

										infoPay = new InfoPay(Constants.ResponseCodes.UNKNOWN_ERROR,
												pendingPaymentMessage, rrn, stan);
										response = new BillPaymentResponse(infoPay, null, null);

										transactionStatus = Constants.Status.Pending;
										billStatus = Constants.BILL_STATUS.BILL_PENDING;
										return response;

									}
								}
							}

							LOG.info("Calling Payment Inquiry from pg_payment_log table");
							PgPaymentLog pgPaymentLog = pgPaymentLogRepository
									.findFirstByVoucherIdAndBillerIdAndBillStatus(request.getTxnInfo().getBillNumber(),
											request.getTxnInfo().getBillerId(), Constants.BILL_STATUS.BILL_PAID);

							if (pgPaymentLog != null
									&& pgPaymentLog.getTransactionStatus().equalsIgnoreCase(Constants.Status.Success)) {

								infoPay = new InfoPay(Constants.ResponseCodes.UNKNOWN_ERROR,
										pendingVoucherUpdateMessage, rrn, stan); // success

								transactionStatus = Constants.Status.Success;
								billStatus = Constants.BILL_STATUS.BILL_PAID;

								response = new BillPaymentResponse(infoPay, null, null);

								return response;
							}

							///// Amount work --- Start ////

							totalTenderFeeAmount = utilMethods
									.getTotalTenderFeeAmount(bppraTenderVoucherResponse.getChallanFee());
							totalTenderFeeAmount = totalTenderFeeAmount.setScale(2, RoundingMode.UP);

							LOG.info("Total Tender Fee Amount: " + totalTenderFeeAmount);

							txnAmount = new BigDecimal(request.getTxnInfo().getTranAmount());

							try {

								if (txnAmount.compareTo(totalTenderFeeAmount) != 0) {
									infoPay = new InfoPay(Constants.ResponseCodes.AMMOUNT_MISMATCH,
											Constants.ResponseDescription.AMMOUNT_MISMATCH, rrn, stan);
									response = new BillPaymentResponse(infoPay, null, null);
									return response;
								}

							} catch (DateTimeParseException e) {
								LOG.error("Error parsing due date: " + e.getMessage());
							}

							///// Amount work --- End ////

							/// TenderMarkChallanPaid Call
							HttpResponse<String> tenderMarkChallanPaid = utilMethods.tenderMarkChallanPaidRequest(
									bppraTenderChallanPaidCall, request.getTxnInfo().getBillNumber(),
									requestFormChallanEnquire, jwt);

							/// TenderMarkChallanPaid Failure
							if (tenderMarkChallanPaid == null) {

								infoPay = new InfoPay(Constants.ResponseCodes.SERVICE_FAIL,
										Constants.ResponseDescription.SERVICE_FAIL, rrn, stan);

								response = new BillPaymentResponse(infoPay, null, null);
								transactionStatus = Constants.Status.Fail;
								return response;

							}

							/// TenderMarkChallanPaid Success
							if (tenderMarkChallanPaid.getStatus() == 200) {

								/////// TenderCompleteInquiry Call /////

								HttpResponse<String> tenderCompleteInquiry = utilMethods.completeInquiryRequest(
										bppraCompleteInquiryCall, authToken, requestFormAuth, jwt, branchcode,
										personName);

								/// TenderCompleteInquiry Failure
								if (tenderCompleteInquiry == null) {

									HttpResponse<String> tenderCompleteInquiryRetry = utilMethods
											.completeInquiryRequest(bppraCompleteInquiryCall, authToken,
													requestFormAuth, jwt, branchcode, personName);

									if (tenderCompleteInquiryRetry == null
											|| tenderCompleteInquiryRetry.getStatus() == 400
											|| tenderCompleteInquiryRetry.getStatus() == 401
											|| tenderCompleteInquiryRetry.getStatus() == 404) {

										LOG.info(
												"TenderCompleteInquiryRetry : BadRequest/UnAuthorized/Enquery Instance with Provided GUID Not Found : rrn: {} - stan: {}",
												rrn, stan);

										infoPay = new InfoPay(Constants.ResponseCodes.OK,
												Constants.ResponseDescription.OPERATION_SUCCESSFULL, rrn, stan);

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

										return response;

									}

									else if (tenderCompleteInquiryRetry.getStatus() == 200) {

										LOG.info(
												"TenderCompleteInquiryRetry : Enquery Completed Successfully :  rrn: {} - stan: {}",
												rrn, stan);

										infoPay = new InfoPay(Constants.ResponseCodes.OK,
												Constants.ResponseDescription.OPERATION_SUCCESSFULL, rrn, stan);

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

										return response;

									}

								}

								//// TenderCompleteInquiry Success
								if (tenderCompleteInquiry.getStatus() == 200) {

									LOG.info(
											"TenderCompleteInquiry : Enquery Completed Successfully :  rrn: {} - stan: {}",
											rrn, stan);

									infoPay = new InfoPay(Constants.ResponseCodes.OK,
											Constants.ResponseDescription.OPERATION_SUCCESSFULL, rrn, stan);

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

									return response;

								}

								else if (tenderCompleteInquiry == null || tenderCompleteInquiry.getStatus() == 400
										|| tenderCompleteInquiry.getStatus() == 401
										|| tenderCompleteInquiry.getStatus() == 404) {

									LOG.info(
											"TenderCompleteInquiry : BadRequest/UnAuthorized/Enquery Instance with Provided GUID Not Found : rrn: {} - stan: {}",
											rrn, stan);

									infoPay = new InfoPay(Constants.ResponseCodes.OK,
											Constants.ResponseDescription.OPERATION_SUCCESSFULL, rrn, stan);

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

									return response;

								}

							}

							else if (tenderMarkChallanPaid.getStatus() == 400) {

								LOG.info("TenderMarkChallanPaid : BadRequest");

								infoPay = new InfoPay(Constants.ResponseCodes.UNKNOWN_ERROR,
										Constants.ResponseDescription.UNKNOWN_ERROR, rrn, stan);
								response = new BillPaymentResponse(infoPay, null, null);
								transactionStatus = Constants.Status.Fail;
								return response;

							}

							else if (tenderMarkChallanPaid.getStatus() == 401) {

								LOG.info("TenderMarkChallanPaid : UnAuthorized");

								infoPay = new InfoPay(Constants.ResponseCodes.CONSUMER_NUMBER_NOT_EXISTS,
										Constants.ResponseDescription.CONSUMER_NUMBER_NOT_EXISTS, rrn, stan);
								response = new BillPaymentResponse(infoPay, null, null);
								transactionStatus = Constants.Status.Fail;
								return response;

							}

							else if (tenderMarkChallanPaid.getStatus() == 404) {

								LOG.info("TenderMarkChallanPaid : Challan Not Found");

								infoPay = new InfoPay(Constants.ResponseCodes.CONSUMER_NUMBER_NOT_EXISTS,
										Constants.ResponseDescription.CONSUMER_NUMBER_NOT_EXISTS, rrn, stan);
								response = new BillPaymentResponse(infoPay, null, null);
								transactionStatus = Constants.Status.Fail;
								return response;

							}

						}

					}

					else if (tenderChallanInquiry.getStatus() == 400) {

						LOG.info("TenderChallanInquiry : Bad Request");

						infoPay = new InfoPay(Constants.ResponseCodes.UNKNOWN_ERROR,
								Constants.ResponseDescription.UNKNOWN_ERROR, rrn, stan);
						response = new BillPaymentResponse(infoPay, null, null);
						transactionStatus = Constants.Status.Fail;
						return response;

					}

					else if (tenderChallanInquiry.getStatus() == 401) {

						LOG.info("TenderChallanInquiry : UnAuthorized");

						infoPay = new InfoPay(Constants.ResponseCodes.UNKNOWN_ERROR,
								Constants.ResponseDescription.UNKNOWN_ERROR, rrn, stan);
						response = new BillPaymentResponse(infoPay, null, null);
						transactionStatus = Constants.Status.Fail;
						return response;

					}

					else if (tenderChallanInquiry.getStatus() == 404) {

						LOG.info("TenderChallanInquiry : Challan Not Found");

						infoPay = new InfoPay(Constants.ResponseCodes.CONSUMER_NUMBER_NOT_EXISTS,
								Constants.ResponseDescription.CONSUMER_NUMBER_NOT_EXISTS, rrn, stan);
						response = new BillPaymentResponse(infoPay, null, null);
						transactionStatus = Constants.Status.Fail;
						return response;

					}

				} else if (tenderAuthResponse.getStatus() == 400) {

					LOG.info("TenderAuthentication : Invalid Secret key");

					infoPay = new InfoPay(Constants.ResponseCodes.UNKNOWN_ERROR,
							Constants.ResponseDescription.UNKNOWN_ERROR, rrn, stan);
					response = new BillPaymentResponse(infoPay, null, null);
					transactionStatus = Constants.Status.Fail;
					return response;

				}

				else if (tenderAuthResponse.getStatus() == 401) {

					LOG.info("TenderAuthentication : UnAuthorized");

					infoPay = new InfoPay(Constants.ResponseCodes.UNKNOWN_ERROR,
							Constants.ResponseDescription.UNKNOWN_ERROR, rrn, stan);
					response = new BillPaymentResponse(infoPay, null, null);
					transactionStatus = Constants.Status.Fail;
					return response;

				}

				else if (tenderAuthResponse.getStatus() == 404) {

					LOG.info("TenderAuthentication : Client with the provided Secret Not Found");

					infoPay = new InfoPay(Constants.ResponseCodes.UNKNOWN_ERROR,
							Constants.ResponseDescription.UNKNOWN_ERROR, rrn, stan);
					response = new BillPaymentResponse(infoPay, null, null);
					transactionStatus = Constants.Status.Fail;
					return response;

				}

			}

			/////// Supplier ////////

			else if (billerNumber.startsWith(supplierPrefix)) {

				///// SupplierAuth Call
				HttpResponse<String> supplierAuthResponse = utilMethods.authRequest(bppraAuthticateCall,
						bppraClientSecret);

				//// SupplierAuth Failure
				if (supplierAuthResponse == null) {

					infoPay = new InfoPay(Constants.ResponseCodes.SERVICE_FAIL,
							Constants.ResponseDescription.SERVICE_FAIL, rrn, stan);

					response = new BillPaymentResponse(infoPay, null, null);
					transactionStatus = Constants.Status.Fail;
					return response;

				}

				///// supplierAuth Success
				if (supplierAuthResponse.getStatus() == 200) {

					String authToken = supplierAuthResponse.getBody();
					LOG.info("authToken" + authToken);

					//// publicKey
					publicKey = rsaUtility.getPublicKey();

					LOG.info("jwt :" + jwt);

					decodeJwt = utilMethods.DecodeJwt(jwt);
					keyAndIv = rsaUtility.RSADecrypt(decodeJwt);

					////// SupplierChallanInquiry
					HttpResponse<String> supplierChallanInquiry = utilMethods.supplierChallanInquiryRequest(
							bppraSupplierChallanInquiryCall, request.getTxnInfo().getBillNumber(),
							requestFormChallanEnquire, jwt);

					//// SupplierChallanInquiry Failure
					if (supplierChallanInquiry == null) {

						infoPay = new InfoPay(Constants.ResponseCodes.SERVICE_FAIL,
								Constants.ResponseDescription.SERVICE_FAIL, rrn, stan);

						response = new BillPaymentResponse(infoPay, null, null);
						transactionStatus = Constants.Status.Fail;
						return response;

					}

					/////// SupplierChallanInquiry Success
					if (supplierChallanInquiry.getStatus() == 200) {

						LOG.info("TenderChallanInquiry : Success ");

						encryptedChallandata = supplierChallanInquiry.getBody();
						decryptData = utilMethods.aesPackedAlgorithm(keyAndIv, encryptedChallandata);

						bppraSupplierVoucherResponse = mapper.readValue(decryptData,
								BppraSupplierVoucherResponse.class);
						LOG.info("BppraSupplierVoucherResponse : " + bppraSupplierVoucherResponse);

						billerName = bppraSupplierVoucherResponse.getSupplierChallanData().getSupplierName();

						//////// status is Paid (True) ////

						if (bppraSupplierVoucherResponse.getSupplierChallanData().isPaidStatus()) {

							billerId = request.getTxnInfo().getBillerId();
							billerNumber = request.getTxnInfo().getBillNumber();

							infoPay = new InfoPay(Constants.ResponseCodes.BILL_ALREADY_PAID,
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

							billStatus = Constants.BILL_STATUS.BILL_PAID;

							response = new BillPaymentResponse(infoPay, txnInfoPay, additionalInfoPay);

							return response;

						}

						////// Status is Unpaid(False) /////

						else {

							challanFees = bppraSupplierVoucherResponse.getChallanFee();
							challanFeeData = objectMapper.writeValueAsString(challanFees);

							PendingPayment pendingPayment = pendingPaymentRepository
									.findFirstByVoucherIdAndBillerIdOrderByPaymentIdDesc(
											request.getTxnInfo().getBillNumber().trim(),
											request.getTxnInfo().getBillerId().trim());

							if (pendingPayment != null) {

								if (pendingPayment.getIgnoreTimer()) {

									infoPay = new InfoPay(Constants.ResponseCodes.UNKNOWN_ERROR, pendingPaymentMessage,
											rrn, stan);
									response = new BillPaymentResponse(infoPay, null, null);
									transactionStatus = Constants.Status.Pending;
									billStatus = Constants.BILL_STATUS.BILL_PENDING;
									return response;

								} else {
									LocalDateTime transactionDateTime = pendingPayment.getTransactionDate();
									LocalDateTime now = LocalDateTime.now(); // Current date and time

									// Calculate the difference in minutes
									long minutesDifference = Duration.between(transactionDateTime, now).toMinutes();

									if (minutesDifference <= pendingThresholdMinutes) {

										infoPay = new InfoPay(Constants.ResponseCodes.UNKNOWN_ERROR,
												pendingPaymentMessage, rrn, stan);
										response = new BillPaymentResponse(infoPay, null, null);

										transactionStatus = Constants.Status.Pending;
										billStatus = Constants.BILL_STATUS.BILL_PENDING;
										return response;

									}
								}
							}

							LOG.info("Calling Payment Inquiry from pg_payment_log table");
							PgPaymentLog pgPaymentLog = pgPaymentLogRepository
									.findFirstByVoucherIdAndBillerIdAndBillStatus(request.getTxnInfo().getBillNumber(),
											request.getTxnInfo().getBillerId(), Constants.BILL_STATUS.BILL_PAID);

							if (pgPaymentLog != null
									&& pgPaymentLog.getTransactionStatus().equalsIgnoreCase(Constants.Status.Success)) {

								infoPay = new InfoPay(Constants.ResponseCodes.UNKNOWN_ERROR,
										pendingVoucherUpdateMessage, rrn, stan); // success

								transactionStatus = Constants.Status.Success;
								billStatus = Constants.BILL_STATUS.BILL_PAID;

								response = new BillPaymentResponse(infoPay, null, null);

								return response;
							}

							///// Amount work --- Start ////

							totalTenderFeeAmount = utilMethods
									.getTotalTenderFeeAmount(bppraSupplierVoucherResponse.getChallanFee());
							totalTenderFeeAmount = totalTenderFeeAmount.setScale(2, RoundingMode.UP);

							LOG.info("Total Tender Fee Amount: " + totalTenderFeeAmount);

							txnAmount = new BigDecimal(request.getTxnInfo().getTranAmount());

							try {

								if (txnAmount.compareTo(totalTenderFeeAmount) != 0) {
									infoPay = new InfoPay(Constants.ResponseCodes.AMMOUNT_MISMATCH,
											Constants.ResponseDescription.AMMOUNT_MISMATCH, rrn, stan);
									response = new BillPaymentResponse(infoPay, null, null);
									return response;
								}

							} catch (DateTimeParseException e) {
								LOG.error("Error parsing due date: " + e.getMessage());
							}

							///// Amount work --- End ////

							/// SupplierMarkChallanPaid Call
							HttpResponse<String> supplierMarkChallanPaid = utilMethods.supplierMarkChallanPaidRequest(
									bppraSupplierChallanPaidCall, request.getTxnInfo().getBillNumber(),
									requestFormChallanEnquire, jwt);

							/// SupplierMarkChallanPaid Failure
							if (supplierMarkChallanPaid == null) {

								infoPay = new InfoPay(Constants.ResponseCodes.SERVICE_FAIL,
										Constants.ResponseDescription.SERVICE_FAIL, rrn, stan);

								response = new BillPaymentResponse(infoPay, null, null);
								transactionStatus = Constants.Status.Fail;
								return response;

							}

							/// SupplierMarkChallanPaid Success
							if (supplierMarkChallanPaid.getStatus() == 200) {

								/////// SupplierCompleteInquiry Call

								HttpResponse<String> supplierCompleteInquiry = utilMethods.completeInquiryRequest(
										bppraCompleteInquiryCall, authToken, requestFormAuth, jwt, branchcode,
										personName);

								////// SupplierCompleteInquiry Failure
								if (supplierCompleteInquiry == null) {

									HttpResponse<String> supplierCompleteInquiryRetry = utilMethods
											.completeInquiryRequest(bppraCompleteInquiryCall, authToken,
													requestFormAuth, jwt, branchcode, personName);

									if (supplierCompleteInquiryRetry == null
											|| supplierCompleteInquiryRetry.getStatus() == 400
											|| supplierCompleteInquiryRetry.getStatus() == 401
											|| supplierCompleteInquiryRetry.getStatus() == 404) {

										LOG.info(
												"SupplierCompleteInquiryRetry : BadRequest/UnAuthorized/Enquery Instance with Provided GUID Not Found : rrn: {} - stan: {}",
												rrn, stan);

										infoPay = new InfoPay(Constants.ResponseCodes.OK,
												Constants.ResponseDescription.OPERATION_SUCCESSFULL, rrn, stan);

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

										return response;

									}

									else if (supplierCompleteInquiryRetry.getStatus() == 200) {

										LOG.info(
												"SupplierCompleteInquiryRetry : Enquery Completed Successfully :  rrn: {} - stan: {}",
												rrn, stan);

										infoPay = new InfoPay(Constants.ResponseCodes.OK,
												Constants.ResponseDescription.OPERATION_SUCCESSFULL, rrn, stan);

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

										return response;

									}

								}

								//// SupplierCompleteInquiry Success
								if (supplierCompleteInquiry.getStatus() == 200) {

									LOG.info(
											"SupplierCompleteInquiry : Enquery Completed Successfully :  rrn: {} - stan: {}",
											rrn, stan);

									infoPay = new InfoPay(Constants.ResponseCodes.OK,
											Constants.ResponseDescription.OPERATION_SUCCESSFULL, rrn, stan);

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

									return response;

								}

								else if (supplierCompleteInquiry.getStatus() == 400
										|| supplierCompleteInquiry.getStatus() == 401
										|| supplierCompleteInquiry.getStatus() == 404) {

									LOG.info(
											"SupplierCompleteInquiry : BadRequest/UnAuthorized/Enquery Instance with Provided GUID Not Found : rrn: {} - stan: {}",
											rrn, stan);

									infoPay = new InfoPay(Constants.ResponseCodes.OK,
											Constants.ResponseDescription.OPERATION_SUCCESSFULL, rrn, stan);

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

									return response;

								}

							}

							else if (supplierMarkChallanPaid.getStatus() == 400) {

								LOG.info("SupplierMarkChallanPaid : BadRequest");

								infoPay = new InfoPay(Constants.ResponseCodes.UNKNOWN_ERROR,
										Constants.ResponseDescription.UNKNOWN_ERROR, rrn, stan);
								response = new BillPaymentResponse(infoPay, null, null);
								transactionStatus = Constants.Status.Fail;
								return response;

							}

							else if (supplierMarkChallanPaid.getStatus() == 401) {

								LOG.info("SupplierMarkChallanPaid : UnAuthorized");

								infoPay = new InfoPay(Constants.ResponseCodes.UNKNOWN_ERROR,
										Constants.ResponseDescription.UNKNOWN_ERROR, rrn, stan);
								response = new BillPaymentResponse(infoPay, null, null);
								transactionStatus = Constants.Status.Fail;
								return response;

							}

							else if (supplierMarkChallanPaid.getStatus() == 404) {

								LOG.info("SupplierMarkChallanPaid : Challan Not Found");

								infoPay = new InfoPay(Constants.ResponseCodes.CONSUMER_NUMBER_NOT_EXISTS,
										Constants.ResponseDescription.CONSUMER_NUMBER_NOT_EXISTS, rrn, stan);
								response = new BillPaymentResponse(infoPay, null, null);
								transactionStatus = Constants.Status.Fail;
								return response;

							}

						}

					}

					else if (supplierChallanInquiry.getStatus() == 400) {

						LOG.info("SupplierChallanInquiry : Bad Request");

						infoPay = new InfoPay(Constants.ResponseCodes.UNKNOWN_ERROR,
								Constants.ResponseDescription.UNKNOWN_ERROR, rrn, stan);
						response = new BillPaymentResponse(infoPay, null, null);
						transactionStatus = Constants.Status.Fail;
						return response;

					}

					else if (supplierChallanInquiry.getStatus() == 401) {

						LOG.info("SupplierChallanInquiry : UnAuthorized");

						infoPay = new InfoPay(Constants.ResponseCodes.UNKNOWN_ERROR,
								Constants.ResponseDescription.UNKNOWN_ERROR, rrn, stan);
						response = new BillPaymentResponse(infoPay, null, null);
						transactionStatus = Constants.Status.Fail;
						return response;

					}

					else if (supplierChallanInquiry.getStatus() == 404) {

						LOG.info("SupplierChallanInquiry : Challan Not Found");

						infoPay = new InfoPay(Constants.ResponseCodes.CONSUMER_NUMBER_NOT_EXISTS,
								Constants.ResponseDescription.CONSUMER_NUMBER_NOT_EXISTS, rrn, stan);
						response = new BillPaymentResponse(infoPay, null, null);
						transactionStatus = Constants.Status.Fail;
						return response;

					}

				} else if (supplierAuthResponse.getStatus() == 400) {

					LOG.info("SupplierAuthentication : Invalid Secret key");

					infoPay = new InfoPay(Constants.ResponseCodes.UNKNOWN_ERROR,
							Constants.ResponseDescription.UNKNOWN_ERROR, rrn, stan);
					response = new BillPaymentResponse(infoPay, null, null);
					transactionStatus = Constants.Status.Fail;
					return response;

				}

				else if (supplierAuthResponse.getStatus() == 401) {

					LOG.info("SupplierAuthentication : Unathorized");

					infoPay = new InfoPay(Constants.ResponseCodes.UNKNOWN_ERROR,
							Constants.ResponseDescription.UNKNOWN_ERROR, rrn, stan);
					response = new BillPaymentResponse(infoPay, null, null);
					transactionStatus = Constants.Status.Fail;
					return response;

				}

				else if (supplierAuthResponse.getStatus() == 404) {

					LOG.info("SupplierAuthentication : Client with the provided Secret Not Found");

					infoPay = new InfoPay(Constants.ResponseCodes.UNKNOWN_ERROR,
							Constants.ResponseDescription.UNKNOWN_ERROR, rrn, stan);
					response = new BillPaymentResponse(infoPay, null, null);
					transactionStatus = Constants.Status.Fail;
					return response;

				}

			}

			else {
				LOG.info("Bill Payment - Tender or Supplier prefix not found in bill number");
				infoPay = new InfoPay(Constants.ResponseCodes.CONSUMER_NUMBER_NOT_EXISTS,
						Constants.ResponseDescription.CONSUMER_NUMBER_NOT_EXISTS, rrn, stan);
				response = new BillPaymentResponse(infoPay, null, null);
				transactionStatus = Constants.Status.Fail;
				return response;
			}

		}

		catch (Exception e) {

			LOG.info("Exception in bill payment ");
		}

		finally

		{

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

				PaymentLog paymentLog = paymentLoggingService.paymentLogBppra(requestedDate, new Date(), rrn, stan,
						response.getInfo().getResponseCode(), response.getInfo().getResponseDesc(), "",
						request.getTxnInfo().getBillNumber(), request.getTxnInfo().getBillerId(), totalTenderFeeAmount,
						null, Constants.ACTIVITY.BillPayment, transactionStatus, channel, billStatus,
						request.getTxnInfo().getTranDate(), request.getTxnInfo().getTranTime(), transAuthId,
						new BigDecimal(request.getTxnInfo().getTranAmount()), "", "", paymentRefrence, bankName,
						bankCode, branchName, branchCode, "", username, challanFeeData);

				if ((billerNumber.startsWith(tenderPrefix))
						&& (billStatus.equalsIgnoreCase(Constants.BILL_STATUS.BILL_PAID))) {
					List<ChallanFee> cList = bppraTenderVoucherResponse.getChallanFee();

					if (paymentLog != null) {
						for (ChallanFee challanFee : cList) {
							FeeType feeType = new FeeType();
							feeType.setPaymentLogId(paymentLog.getID());
							feeType.setFees(challanFee.getAmount());
							feeType.setTypeDetail(challanFee.getTariffTitle());
							feeType.setSource(paymentLogTable);
							feeTypeRepository.save(feeType);

							LOG.info("Saved FeeType {} associated with PaymentLog ID {}", feeType.getId(),
									paymentLog.getID());

						}
					} else {
						LOG.error("Failed to saved FeeDetails because paymentLog is null. Cannot perform operation.");
					}
				}

				else if ((billerNumber.startsWith(supplierPrefix))
						&& (billStatus.equalsIgnoreCase(Constants.BILL_STATUS.BILL_PAID))) {
					List<ChallanFee> cList = bppraSupplierVoucherResponse.getChallanFee();

					if (paymentLog != null) {
						for (ChallanFee challanFee : cList) {
							FeeType feeType = new FeeType();
							feeType.setPaymentLogId(paymentLog.getID());
							feeType.setFees(challanFee.getAmount());
							feeType.setTypeDetail(challanFee.getTariffTitle());
							feeType.setSource(paymentLogTable);
							feeTypeRepository.save(feeType);

							LOG.info("Saved FeeType {} associated with PaymentLog ID {}", feeType.getId(),
									paymentLog.getID());

						}
					} else {
						LOG.error("Failed to saved FeeDetails because paymentLog is null. Cannot perform operation.");
					}
				}

				LOG.info(" --- Bppra Bill Payment Method End --- ");

			} catch (Exception ex) {
				LOG.error("Bill Payment - Payment Log Exception ", ex);
			}

		}
		return response;

	}

	@Override
	public BillPaymentResponse billPaymentBiseKohat(BillPaymentRequest request, HttpServletRequest httpRequestData) {

		LOG.info("Bise Kohat Bill Payment Request {} ", request.toString());

		BillPaymentResponse response = null;
		UpdateBiseKohatResponse updateBiseKohatResponse = null;
		BiseKohatBillInquiryResponse biseKohatBillInquiryResponse = null;
		Date requestedDate = new Date();
		InfoPay infoPay = null;
		TxnInfoPay txnInfoPay = null;
		AdditionalInfoPay additionalInfoPay = null;
		String rrn = request.getInfo().getRrn(); // utilMethods.getRRN();
		String paymentRefrence = utilMethods.getRRN();
		String stan = request.getInfo().getStan();
		LOG.info("RRN :{ }", rrn);
		String transAuthId = request.getTxnInfo().getTranAuthId();
		String channel = "", username = "";

		ArrayList<String> inquiryParams = new ArrayList<String>();
		ArrayList<String> paymentParams = new ArrayList<String>();

		BigDecimal amountInDueToDate = null, txnAmount = null;
		String billStatus = "", transactionStatus = "", billerId = "", billerNumber = "", billerName = "",
				billingMonth = "", dueDate = "";
		String bankName = "", bankCode = "", branchName = "", branchCode = "";

		LocalDateTime nowDateTime = LocalDateTime.now();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		String localDateTime = nowDateTime.format(formatter);

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

			inquiryParams.add(Constants.MPAY_REQUEST_METHODS.BISE_KOHAT_INQUIRY);

			inquiryParams.add(kusername);
			inquiryParams.add(kpassword);
			inquiryParams.add(request.getTxnInfo().getBillNumber().trim());
			inquiryParams.add(rrn);
			inquiryParams.add(stan);

			//// Inquiry Call to M-Pay

			biseKohatBillInquiryResponse = serviceCaller.get(inquiryParams, BiseKohatBillInquiryResponse.class, rrn,
					Constants.ACTIVITY.BillInquiry, BillerConstant.BISEKOHAT.BISEKOHAT);

			if (biseKohatBillInquiryResponse != null) {

				if (biseKohatBillInquiryResponse.getBiseKohatResponse() == null) {

					infoPay = new InfoPay(Constants.ResponseCodes.SERVICE_FAIL,
							Constants.ResponseDescription.SERVICE_FAIL, rrn, stan);

					response = new BillPaymentResponse(infoPay, null, null);

					transactionStatus = Constants.Status.Fail;

					return response;

				}

				//// consumer number not exsist

				else if (biseKohatBillInquiryResponse.getBiseKohatResponse().getResponseCode()
						.equalsIgnoreCase(Constants.ResponseCodes.CONSUMER_NUMBER_NOT_EXISTS)) {

					infoPay = new InfoPay(Constants.ResponseCodes.CONSUMER_NUMBER_NOT_EXISTS,
							Constants.ResponseDescription.CONSUMER_NUMBER_NOT_EXISTS, rrn, stan);

					response = new BillPaymentResponse(infoPay, null, null);

					return response;
				}

				//// Already Paid

				else if (biseKohatBillInquiryResponse.getBiseKohatResponse().getResponseCode()
						.equalsIgnoreCase(Constants.ResponseCodes.BILL_ALREADY_PAID)) {

					billerId = request.getTxnInfo().getBillerId();
					billerNumber = request.getTxnInfo().getBillNumber();

					infoPay = new InfoPay(Constants.ResponseCodes.BILL_ALREADY_PAID,
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

					billStatus = Constants.BILL_STATUS.BILL_PAID;

					response = new BillPaymentResponse(infoPay, txnInfoPay, additionalInfoPay);
					return response;

				}

				/////// Inquiry success response

				else if (biseKohatBillInquiryResponse.getBiseKohatResponse().getResponseCode()
						.equalsIgnoreCase(Constants.ResponseCodes.OK)) {

					PendingPayment pendingPayment = pendingPaymentRepository
							.findFirstByVoucherIdAndBillerIdOrderByPaymentIdDesc(
									request.getTxnInfo().getBillNumber().trim(),
									request.getTxnInfo().getBillerId().trim());

					if (pendingPayment != null) {

						if (pendingPayment.getIgnoreTimer()) {

							infoPay = new InfoPay(Constants.ResponseCodes.UNKNOWN_ERROR, pendingPaymentMessage, rrn,
									stan);
							response = new BillPaymentResponse(infoPay, null, null);
							transactionStatus = Constants.Status.Pending;
							billStatus = Constants.BILL_STATUS.BILL_PENDING;
							return response;

						} else {
							LocalDateTime transactionDateTime = pendingPayment.getTransactionDate();
							LocalDateTime now = LocalDateTime.now(); // Current date and time

							// Calculate the difference in minutes
							long minutesDifference = Duration.between(transactionDateTime, now).toMinutes();

							if (minutesDifference <= pendingThresholdMinutes) {

								infoPay = new InfoPay(Constants.ResponseCodes.UNKNOWN_ERROR, pendingPaymentMessage, rrn,
										stan);
								response = new BillPaymentResponse(infoPay, null, null);

								transactionStatus = Constants.Status.Pending;
								billStatus = Constants.BILL_STATUS.BILL_PENDING;
								return response;

							}
						}
					}

					LOG.info("Calling Payment Inquiry from pg_payment_log table");
					PgPaymentLog pgPaymentLog = pgPaymentLogRepository.findFirstByVoucherIdAndBillerIdAndBillStatus(
							request.getTxnInfo().getBillNumber(), request.getTxnInfo().getBillerId(),
							Constants.BILL_STATUS.BILL_PAID);

					if (pgPaymentLog != null
							&& pgPaymentLog.getTransactionStatus().equalsIgnoreCase(Constants.Status.Success)) {

						infoPay = new InfoPay(Constants.ResponseCodes.UNKNOWN_ERROR, pendingVoucherUpdateMessage, rrn,
								stan); // success

						transactionStatus = Constants.Status.Success;
						billStatus = Constants.BILL_STATUS.BILL_PAID;

						response = new BillPaymentResponse(infoPay, null, null);

						return response;
					}

					billerName = biseKohatBillInquiryResponse.getBiseKohatResponse().getBisekohatbillinquiry()
							.getBiseKohatBillinquiryData().getCustomerName();
					billingMonth = utilMethods.formatDateFormat(biseKohatBillInquiryResponse.getBiseKohatResponse()
							.getBisekohatbillinquiry().getBiseKohatBillinquiryData().getBillingMonth());

					dueDate = utilMethods.transactionDateFormater(biseKohatBillInquiryResponse.getBiseKohatResponse()
							.getBisekohatbillinquiry().getBiseKohatBillinquiryData().getDueDate());

					paymentParams.add(Constants.MPAY_REQUEST_METHODS.BISE_KOHAT_PAYMENT);
					paymentParams.add(request.getTxnInfo().getBillNumber().trim());
					paymentParams.add(request.getInfo().getRrn());
					paymentParams.add(request.getTxnInfo().getTranAmount());
					paymentParams.add(biseKohatBillInquiryResponse.getBiseKohatResponse().getBisekohatbillinquiry()
							.getBiseKohatBillinquiryData().getPurpose());
					paymentParams.add(kusername);
					paymentParams.add(kpassword);
					paymentParams.add(channel);
					paymentParams.add(localDateTime);
					paymentParams.add(rrn);
					paymentParams.add(stan);

					txnAmount = new BigDecimal(request.getTxnInfo().getTranAmount());

					try {

						amountInDueToDate = new BigDecimal(biseKohatBillInquiryResponse.getBiseKohatResponse()
								.getBisekohatbillinquiry().getBiseKohatBillinquiryData().getAmount());
						amountInDueToDate = amountInDueToDate.setScale(2, RoundingMode.UP);

						if (txnAmount.compareTo(amountInDueToDate) != 0) {
							infoPay = new InfoPay(Constants.ResponseCodes.AMMOUNT_MISMATCH,
									Constants.ResponseDescription.AMMOUNT_MISMATCH, rrn, stan);
							response = new BillPaymentResponse(infoPay, null, null);
							return response;
						}

					} catch (DateTimeParseException e) {
						LOG.error("Error parsing due date: " + e.getMessage());
					}

					//// M-Pay call to Payment

					updateBiseKohatResponse = serviceCaller.get(paymentParams, UpdateBiseKohatResponse.class, rrn,
							Constants.ACTIVITY.BillPayment, BillerConstant.BISEKOHAT.BISEKOHAT);

					if (updateBiseKohatResponse != null) {

						if (updateBiseKohatResponse.getBiseKohatResponse() == null) {

							infoPay = new InfoPay(Constants.ResponseCodes.SERVICE_FAIL,
									Constants.ResponseDescription.SERVICE_FAIL, rrn, stan);

							response = new BillPaymentResponse(infoPay, null, null);

							transactionStatus = Constants.Status.Fail;

							return response;

						}

						else if (updateBiseKohatResponse.getBiseKohatResponse().getResponseCode()
								.equalsIgnoreCase(Constants.ResponseCodes.OK)) {

							infoPay = new InfoPay(Constants.ResponseCodes.OK,
									Constants.ResponseDescription.OPERATION_SUCCESSFULL, rrn, stan);

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

							billStatus = Constants.BILL_STATUS.BILL_PAID;

							return response;

						}

						else {

							infoPay = new InfoPay(Constants.ResponseCodes.UNKNOWN_ERROR,
									Constants.ResponseDescription.UNKNOWN_ERROR, rrn, stan);
							response = new BillPaymentResponse(infoPay, null, null);
							transactionStatus = Constants.Status.Fail;
						}

					} else {

						infoPay = new InfoPay(Constants.ResponseCodes.SERVICE_FAIL,
								Constants.ResponseDescription.SERVICE_FAIL, rrn, stan);

						response = new BillPaymentResponse(infoPay, null, null);

						transactionStatus = Constants.Status.Fail;

						return response;

					}

				}

				else {

					infoPay = new InfoPay(Constants.ResponseCodes.UNKNOWN_ERROR,
							Constants.ResponseDescription.UNKNOWN_ERROR, rrn, stan);

					response = new BillPaymentResponse(infoPay, null, null);

					transactionStatus = Constants.Status.Fail;

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

		catch (

		Exception e) {

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
						response.getInfo().getResponseCode(), response.getInfo().getResponseDesc(), billerName,
						request.getTxnInfo().getBillNumber(), request.getTxnInfo().getBillerId(), amountInDueToDate,
						null, Constants.ACTIVITY.BillPayment, transactionStatus, channel, billStatus,
						request.getTxnInfo().getTranDate(), request.getTxnInfo().getTranTime(), transAuthId,
						new BigDecimal(request.getTxnInfo().getTranAmount()), dueDate, billingMonth, paymentRefrence,
						bankName, bankCode, branchName, branchCode, "", username, "");

				LOG.info(" --- Bill Payment Method End --- ");

			} catch (Exception ex) {
				LOG.error("{Exception payment Logs}", ex);
			}

		}
		return response;
	}

	@Override
	public BillPaymentResponse billPaymentLesco(BillPaymentRequest request, HttpServletRequest httpRequestData) {

		LOG.info("Lesco Bill Payment Request {} ", request.toString());

		BillPaymentResponse response = null;
		LescoBillPaymentResponse lescoBillPaymentResponse = null;
		LescoBillInquiryResponse lescoBillInquiryResponse = null;
		Date requestedDate = new Date();
		InfoPay infoPay = null;
		TxnInfoPay txnInfoPay = null;
		AdditionalInfoPay additionalInfoPay = null;
		String rrn = request.getInfo().getRrn(); // utilMethods.getRRN();
		String paymentRefrence = utilMethods.getRRN();
		String stan = request.getInfo().getStan();
		LOG.info("RRN :{ }", rrn);
		String transAuthId = request.getTxnInfo().getTranAuthId();
		String channel = "", username = "";

		ArrayList<String> inquiryParams = new ArrayList<String>();
		ArrayList<String> paymentParams = new ArrayList<String>();

		BigDecimal amountInDueDate = null, amountAfterDueDate = null, txnAmount = null;
		String billStatus = "", transactionStatus = "", billerId = "", billerNumber = "", billerName = "",
				billingMonth = "", dueDate = "", cardType = "", ru_Code = "", customer_Id = "", amountInDueDateRes = "",
				amountAfterDueDateRes = "";
		String bankName = "", bankCode = "", branchName = "", branchCode = "";

		LocalDateTime nowDateTime = LocalDateTime.now();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd-yyyy");
		String PaymentDate = nowDateTime.format(formatter);

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

			inquiryParams.add(Constants.MPAY_REQUEST_METHODS.LESCO_BILL_INQUIRY);
			inquiryParams.add(request.getTxnInfo().getBillNumber().trim());
			inquiryParams.add(rrn);
			inquiryParams.add(stan);

			//// Inquiry Call to M-Pay

			lescoBillInquiryResponse = serviceCaller.get(inquiryParams, LescoBillInquiryResponse.class, rrn,
					Constants.ACTIVITY.BillInquiry, BillerConstant.LESCO.LESCO);

			if (lescoBillInquiryResponse != null) {

				if (lescoBillInquiryResponse.getLescoBillInquiry() == null) {

					infoPay = new InfoPay(Constants.ResponseCodes.SERVICE_FAIL,
							Constants.ResponseDescription.SERVICE_FAIL, rrn, stan);

					response = new BillPaymentResponse(infoPay, null, null);

					transactionStatus = Constants.Status.Fail;

					return response;

				}

				//// consumer number not exsist

				else if (lescoBillInquiryResponse.getLescoBillInquiry().getResponseCode()
						.equalsIgnoreCase(Constants.ResponseCodes.CONSUMER_NUMBER_NOT_EXISTS)) {

					infoPay = new InfoPay(Constants.ResponseCodes.CONSUMER_NUMBER_NOT_EXISTS,
							Constants.ResponseDescription.CONSUMER_NUMBER_NOT_EXISTS, rrn, stan);

					response = new BillPaymentResponse(infoPay, null, null);

					return response;
				}

				/////// Inquiry success response

				else if (lescoBillInquiryResponse.getLescoBillInquiry().getResponseCode()
						.equalsIgnoreCase(Constants.ResponseCodes.OK)) {

					PendingPayment pendingPayment = pendingPaymentRepository
							.findFirstByVoucherIdAndBillerIdOrderByPaymentIdDesc(
									request.getTxnInfo().getBillNumber().trim(),
									request.getTxnInfo().getBillerId().trim());

					if (pendingPayment != null) {

						if (pendingPayment.getIgnoreTimer()) {

							infoPay = new InfoPay(Constants.ResponseCodes.UNKNOWN_ERROR, pendingPaymentMessage, rrn,
									stan);
							response = new BillPaymentResponse(infoPay, null, null);
							transactionStatus = Constants.Status.Pending;
							billStatus = Constants.BILL_STATUS.BILL_PENDING;
							return response;

						} else {
							LocalDateTime transactionDateTime = pendingPayment.getTransactionDate();
							LocalDateTime now = LocalDateTime.now(); // Current date and time

							// Calculate the difference in minutes
							long minutesDifference = Duration.between(transactionDateTime, now).toMinutes();

							if (minutesDifference <= pendingThresholdMinutes) {

								infoPay = new InfoPay(Constants.ResponseCodes.UNKNOWN_ERROR, pendingPaymentMessage, rrn,
										stan);
								response = new BillPaymentResponse(infoPay, null, null);

								transactionStatus = Constants.Status.Pending;
								billStatus = Constants.BILL_STATUS.BILL_PENDING;
								return response;

							}
						}
					}

					LOG.info("Calling Payment Inquiry from pg_payment_log table");
					PgPaymentLog pgPaymentLog = pgPaymentLogRepository.findFirstByVoucherIdAndBillerIdAndBillStatus(
							request.getTxnInfo().getBillNumber(), request.getTxnInfo().getBillerId(),
							Constants.BILL_STATUS.BILL_PAID);

					if (pgPaymentLog != null
							&& pgPaymentLog.getTransactionStatus().equalsIgnoreCase(Constants.Status.Success)) {

						infoPay = new InfoPay(Constants.ResponseCodes.UNKNOWN_ERROR, pendingVoucherUpdateMessage, rrn,
								stan); // success

						transactionStatus = Constants.Status.Success;
						billStatus = Constants.BILL_STATUS.BILL_PAID;

						response = new BillPaymentResponse(infoPay, null, null);

						return response;
					}

					billerName = lescoBillInquiryResponse.getLescoBillInquiry().getLescobillinquirydata()
							.getDataWrapper().get(0).getName();

					billingMonth = utilMethods.getFormattedBillingMonth(lescoBillInquiryResponse.getLescoBillInquiry()
							.getLescobillinquirydata().getDataWrapper().get(0).getBillMonth());

					dueDate = utilMethods.getFormattedDueDate(lescoBillInquiryResponse.getLescoBillInquiry()
							.getLescobillinquirydata().getDataWrapper().get(0).getDueDate());

					cardType = lescoBillInquiryResponse.getLescoBillInquiry().getLescobillinquirydata().getDataWrapper()
							.get(0).getCardType();
					ru_Code = lescoBillInquiryResponse.getLescoBillInquiry().getLescobillinquirydata().getDataWrapper()
							.get(0).getRuCode();
					customer_Id = lescoBillInquiryResponse.getLescoBillInquiry().getLescobillinquirydata()
							.getDataWrapper().get(0).getCustId();

					paymentParams.add(Constants.MPAY_REQUEST_METHODS.LESCO_BILL_PAYMENT);
					paymentParams.add(cardType);
					paymentParams.add(request.getTxnInfo().getBillNumber().trim());
					paymentParams.add(ru_Code);
					paymentParams.add(PaymentDate);
					paymentParams.add(request.getTxnInfo().getTranAmount());
					paymentParams.add(collMechanismCode);
					paymentParams.add(customer_Id);
					paymentParams.add(bankBranchCode);
					paymentParams.add(rrn);
					paymentParams.add(stan);

					txnAmount = new BigDecimal(request.getTxnInfo().getTranAmount());

					amountInDueDateRes = lescoBillInquiryResponse.getLescoBillInquiry().getLescobillinquirydata()
							.getDataWrapper().get(0).getAmountWithInDueDate();

					amountAfterDueDateRes = lescoBillInquiryResponse.getLescoBillInquiry().getLescobillinquirydata()
							.getDataWrapper().get(0).getAmountAfterDueDate();

					amountInDueDate = new BigDecimal(lescoBillInquiryResponse.getLescoBillInquiry()
							.getLescobillinquirydata().getDataWrapper().get(0).getAmountWithInDueDate());
					amountInDueDate = amountInDueDate.setScale(2, RoundingMode.UP);

					amountAfterDueDate = new BigDecimal(lescoBillInquiryResponse.getLescoBillInquiry()
							.getLescobillinquirydata().getDataWrapper().get(0).getAmountAfterDueDate());
					amountAfterDueDate = amountAfterDueDate.setScale(2, RoundingMode.UP);

					if (utilMethods.isValidInput(dueDate)) {
						LocalDate currentDate = LocalDate.now();

						try {
							LocalDate localDueDate = utilMethods.parseDueDateWithoutDashes(dueDate);

							///// Check due date conditions///

							if (utilMethods.isPaymentWithinDueDate(currentDate, localDueDate)) {
								if (Double.valueOf(request.getTxnInfo().getTranAmount())
										.compareTo(Double.valueOf(amountInDueDateRes)) != 0) {
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
								.compareTo(Double.valueOf(amountInDueDateRes)) != 0) {
							infoPay = new InfoPay(Constants.ResponseCodes.AMMOUNT_MISMATCH,
									Constants.ResponseDescription.AMMOUNT_MISMATCH, rrn, stan);
							response = new BillPaymentResponse(infoPay, null, null);
							return response;
						}
					}

					//// M-Pay call to Payment

					lescoBillPaymentResponse = serviceCaller.get(paymentParams, LescoBillPaymentResponse.class, rrn,
							Constants.ACTIVITY.BillPayment, BillerConstant.LESCO.LESCO);

					if (lescoBillPaymentResponse != null) {

						if (lescoBillPaymentResponse.getLescoBillPayment() == null) {

							infoPay = new InfoPay(Constants.ResponseCodes.SERVICE_FAIL,
									Constants.ResponseDescription.SERVICE_FAIL, rrn, stan);

							response = new BillPaymentResponse(infoPay, null, null);

							transactionStatus = Constants.Status.Fail;

							return response;

						}

						//// Already Paid

						else if (lescoBillPaymentResponse.getLescoBillPayment().getResponseCode()
								.equalsIgnoreCase(Constants.ResponseCodes.BILL_ALREADY_PAID)) {

							billerId = request.getTxnInfo().getBillerId();
							billerNumber = request.getTxnInfo().getBillNumber();

							infoPay = new InfoPay(Constants.ResponseCodes.BILL_ALREADY_PAID,
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

							billStatus = Constants.BILL_STATUS.BILL_PAID;

							response = new BillPaymentResponse(infoPay, txnInfoPay, additionalInfoPay);
							return response;

						}

						else if (lescoBillPaymentResponse.getLescoBillPayment().getResponseCode()
								.equalsIgnoreCase(Constants.ResponseCodes.OK)) {

							infoPay = new InfoPay(Constants.ResponseCodes.OK,
									Constants.ResponseDescription.OPERATION_SUCCESSFULL, rrn, stan);

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

							billStatus = Constants.BILL_STATUS.BILL_PAID;

							return response;

						}

						else {

							infoPay = new InfoPay(Constants.ResponseCodes.UNKNOWN_ERROR,
									Constants.ResponseDescription.UNKNOWN_ERROR, rrn, stan);
							response = new BillPaymentResponse(infoPay, null, null);
							transactionStatus = Constants.Status.Fail;
						}

					} else {

						infoPay = new InfoPay(Constants.ResponseCodes.SERVICE_FAIL,
								Constants.ResponseDescription.SERVICE_FAIL, rrn, stan);

						response = new BillPaymentResponse(infoPay, null, null);

						transactionStatus = Constants.Status.Fail;

						return response;

					}

				}

				else {

					infoPay = new InfoPay(Constants.ResponseCodes.UNKNOWN_ERROR,
							Constants.ResponseDescription.UNKNOWN_ERROR, rrn, stan);

					response = new BillPaymentResponse(infoPay, null, null);

					transactionStatus = Constants.Status.Fail;

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

		catch (

		Exception e) {

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
						response.getInfo().getResponseCode(), response.getInfo().getResponseDesc(), billerName,
						request.getTxnInfo().getBillNumber(), request.getTxnInfo().getBillerId(), amountInDueDate,
						amountAfterDueDate, Constants.ACTIVITY.BillPayment, transactionStatus, channel, billStatus,
						request.getTxnInfo().getTranDate(), request.getTxnInfo().getTranTime(), transAuthId,
						new BigDecimal(request.getTxnInfo().getTranAmount()), dueDate, billingMonth, paymentRefrence,
						bankName, bankCode, branchName, branchCode, "", username, "");

				LOG.info(" --- Bill Payment Method End --- ");

			} catch (Exception ex) {
				LOG.error("{Exception payment Logs}", ex);
			}

		}
		return response;
	}

	@Override
	public BillPaymentResponse billPaymentWasa(BillPaymentRequest request, HttpServletRequest httpRequestData) {

		LOG.info("Wasa Bill Payment Request {} ", request.toString());

		BillPaymentResponse response = null;
		WasaBillPaymentResponse wasaBillPaymentResponse = null;
		WasaBillnquiryResponse wasaBillnquiryResponse = null;
		Date requestedDate = new Date();
		InfoPay infoPay = null;
		TxnInfoPay txnInfoPay = null;
		AdditionalInfoPay additionalInfoPay = null;
		String rrn = request.getInfo().getRrn(); // utilMethods.getRRN();
		String paymentRefrence = utilMethods.getRRN();
		String stan = request.getInfo().getStan();
		LOG.info("RRN :{ }", rrn);
		String transAuthId = request.getTxnInfo().getTranAuthId();
		String channel = "", username = "";

		ArrayList<String> inquiryParams = new ArrayList<String>();
		ArrayList<String> paymentParams = new ArrayList<String>();

		BigDecimal amountInDueDate = null, amountAfterDueDate = null, txnAmount = null;
		String billStatus = "", transactionStatus = "", billerId = "", billerNumber = "", billerName = "",
				billingMonth = "", dueDate = "", cardType = "", ru_Code = "", customer_Id = "", amountInDueDateRes = "",
				amountAfterDueDateRes = "";
		String bankName = "", bankCode = "", branchName = "", branchCode = "";

		try {

			String combinedTranDateTime = request.getTxnInfo().getTranDate() + request.getTxnInfo().getTranTime();

			String dateTime = utilMethods.gettransDateTime(combinedTranDateTime);

			if (request.getBranchInfo() != null) {
				bankName = request.getBranchInfo().getBankName();
				bankCode = request.getBranchInfo().getBankCode();
				branchName = request.getBranchInfo().getBranchName();
				branchCode = request.getBranchInfo().getBranchCode();
			}

			String[] result = jwtTokenUtil.getTokenInformation(httpRequestData);

			username = result[0];
			channel = result[1];

			inquiryParams.add(Constants.MPAY_REQUEST_METHODS.WASA_BILL_INQUIRY);
			inquiryParams.add(request.getTxnInfo().getBillNumber().trim());
			inquiryParams.add(rrn);
			inquiryParams.add(stan);

			//// Inquiry Call to M-Pay

			wasaBillnquiryResponse = serviceCaller.get(inquiryParams, WasaBillnquiryResponse.class, rrn,
					Constants.ACTIVITY.BillInquiry, BillerConstant.LESCO.LESCO);

			if (wasaBillnquiryResponse != null) {

				if (wasaBillnquiryResponse.getWasaResponse() == null) {

					infoPay = new InfoPay(Constants.ResponseCodes.SERVICE_FAIL,
							Constants.ResponseDescription.SERVICE_FAIL, rrn, stan);

					response = new BillPaymentResponse(infoPay, null, null);

					transactionStatus = Constants.Status.Fail;

					return response;

				}

				//// consumer number not exsist

				else if (wasaBillnquiryResponse.getWasaResponse().getResponseCode()
						.equalsIgnoreCase(Constants.ResponseCodes.CONSUMER_NUMBER_NOT_EXISTS)) {

					infoPay = new InfoPay(Constants.ResponseCodes.CONSUMER_NUMBER_NOT_EXISTS,
							Constants.ResponseDescription.CONSUMER_NUMBER_NOT_EXISTS, rrn, stan);

					response = new BillPaymentResponse(infoPay, null, null);

					return response;
				}

				//// Already Paid

				else if (wasaBillnquiryResponse.getWasaResponse().getResponseCode()
						.equalsIgnoreCase(Constants.ResponseCodes.BILL_ALREADY_PAID)) {

					billerId = request.getTxnInfo().getBillerId();
					billerNumber = request.getTxnInfo().getBillNumber();

					infoPay = new InfoPay(Constants.ResponseCodes.BILL_ALREADY_PAID,
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

					billStatus = Constants.BILL_STATUS.BILL_PAID;

					response = new BillPaymentResponse(infoPay, txnInfoPay, additionalInfoPay);
					return response;

				}

				/////// Inquiry success response

				else if (wasaBillnquiryResponse.getWasaResponse().getResponseCode()
						.equalsIgnoreCase(Constants.ResponseCodes.OK)) {

					PendingPayment pendingPayment = pendingPaymentRepository
							.findFirstByVoucherIdAndBillerIdOrderByPaymentIdDesc(
									request.getTxnInfo().getBillNumber().trim(),
									request.getTxnInfo().getBillerId().trim());

					if (pendingPayment != null) {

						if (pendingPayment.getIgnoreTimer()) {

							infoPay = new InfoPay(Constants.ResponseCodes.UNKNOWN_ERROR, pendingPaymentMessage, rrn,
									stan);
							response = new BillPaymentResponse(infoPay, null, null);
							transactionStatus = Constants.Status.Pending;
							billStatus = Constants.BILL_STATUS.BILL_PENDING;
							return response;

						} else {
							LocalDateTime transactionDateTime = pendingPayment.getTransactionDate();
							LocalDateTime now = LocalDateTime.now(); // Current date and time

							// Calculate the difference in minutes
							long minutesDifference = Duration.between(transactionDateTime, now).toMinutes();

							if (minutesDifference <= pendingThresholdMinutes) {

								infoPay = new InfoPay(Constants.ResponseCodes.UNKNOWN_ERROR, pendingPaymentMessage, rrn,
										stan);
								response = new BillPaymentResponse(infoPay, null, null);

								transactionStatus = Constants.Status.Pending;
								billStatus = Constants.BILL_STATUS.BILL_PENDING;
								return response;

							}
						}
					}

					LOG.info("Calling Payment Inquiry from pg_payment_log table");
					PgPaymentLog pgPaymentLog = pgPaymentLogRepository.findFirstByVoucherIdAndBillerIdAndBillStatus(
							request.getTxnInfo().getBillNumber(), request.getTxnInfo().getBillerId(),
							Constants.BILL_STATUS.BILL_PAID);

					if (pgPaymentLog != null
							&& pgPaymentLog.getTransactionStatus().equalsIgnoreCase(Constants.Status.Success)) {

						infoPay = new InfoPay(Constants.ResponseCodes.UNKNOWN_ERROR, pendingVoucherUpdateMessage, rrn,
								stan); // success

						transactionStatus = Constants.Status.Success;
						billStatus = Constants.BILL_STATUS.BILL_PAID;

						response = new BillPaymentResponse(infoPay, null, null);

						return response;
					}

					billerId = request.getTxnInfo().getBillerId();
					billerNumber = request.getTxnInfo().getBillNumber();
					billerName = wasaBillnquiryResponse.getWasaResponse().getWasaBillInquiry().getConsumername();

					amountInDueDateRes = wasaBillnquiryResponse.getWasaResponse().getWasaBillInquiry().getBillAmount();

					amountAfterDueDateRes = wasaBillnquiryResponse.getWasaResponse().getWasaBillInquiry()
							.getBillAmountAfterDueDate();

					amountInDueDate = new BigDecimal(
							wasaBillnquiryResponse.getWasaResponse().getWasaBillInquiry().getBillAmount());
					amountInDueDate = amountInDueDate.setScale(2, RoundingMode.UP);

					amountAfterDueDate = new BigDecimal(
							wasaBillnquiryResponse.getWasaResponse().getWasaBillInquiry().getBillAmountAfterDueDate());
					amountAfterDueDate = amountAfterDueDate.setScale(2, RoundingMode.UP);

					dueDate = utilMethods.getDueDateFormatted(
							wasaBillnquiryResponse.getWasaResponse().getWasaBillInquiry().getDueDate());

					billingMonth = wasaBillnquiryResponse.getWasaResponse().getWasaBillInquiry().getPeriod();

					txnAmount = new BigDecimal(request.getTxnInfo().getTranAmount());

					if (utilMethods.isValidInput(dueDate)) {
						LocalDate currentDate = LocalDate.now();

						try {
							LocalDate localDueDate = utilMethods.parseDueDateWithoutDashes(dueDate);

							///// Check due date conditions///

							if (utilMethods.isPaymentWithinDueDate(currentDate, localDueDate)) {
								if (Double.valueOf(request.getTxnInfo().getTranAmount())
										.compareTo(Double.valueOf(amountInDueDateRes)) != 0) {
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
								.compareTo(Double.valueOf(amountInDueDateRes)) != 0) {
							infoPay = new InfoPay(Constants.ResponseCodes.AMMOUNT_MISMATCH,
									Constants.ResponseDescription.AMMOUNT_MISMATCH, rrn, stan);
							response = new BillPaymentResponse(infoPay, null, null);
							return response;
						}
					}

					//// M-Pay call to Payment

					paymentParams.add(Constants.MPAY_REQUEST_METHODS.WASA_BILL_PAYMENT);
					paymentParams.add(request.getTxnInfo().getBillNumber().trim());
					paymentParams.add(request.getTxnInfo().getTranAmount());
					paymentParams.add(consumerCell);
					paymentParams.add(payMode);
					paymentParams.add(rrn);
					paymentParams.add(dateTime);
					paymentParams.add(tranStatus);
					paymentParams.add(rrn);
					paymentParams.add(stan);

					wasaBillPaymentResponse = serviceCaller.get(paymentParams, WasaBillPaymentResponse.class, rrn,
							Constants.ACTIVITY.BillPayment, BillerConstant.WASA.WASA);

					if (wasaBillPaymentResponse != null) {

						if (wasaBillPaymentResponse.getWasaBillPayment() == null) {

							infoPay = new InfoPay(Constants.ResponseCodes.SERVICE_FAIL,
									Constants.ResponseDescription.SERVICE_FAIL, rrn, stan);

							response = new BillPaymentResponse(infoPay, null, null);

							transactionStatus = Constants.Status.Fail;

							return response;

						}

						else if (wasaBillPaymentResponse.getWasaBillPayment().getResponseCode()
								.equalsIgnoreCase(Constants.ResponseCodes.OK)) {

							infoPay = new InfoPay(Constants.ResponseCodes.OK,
									Constants.ResponseDescription.OPERATION_SUCCESSFULL, rrn, stan);

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

							billStatus = Constants.BILL_STATUS.BILL_PAID;

							return response;

						}

						else {

							infoPay = new InfoPay(Constants.ResponseCodes.UNKNOWN_ERROR,
									Constants.ResponseDescription.UNKNOWN_ERROR, rrn, stan);
							response = new BillPaymentResponse(infoPay, null, null);
							transactionStatus = Constants.Status.Fail;
						}

					} else {

						infoPay = new InfoPay(Constants.ResponseCodes.SERVICE_FAIL,
								Constants.ResponseDescription.SERVICE_FAIL, rrn, stan);

						response = new BillPaymentResponse(infoPay, null, null);

						transactionStatus = Constants.Status.Fail;

						return response;

					}

				}

				else {

					infoPay = new InfoPay(Constants.ResponseCodes.UNKNOWN_ERROR,
							Constants.ResponseDescription.UNKNOWN_ERROR, rrn, stan);

					response = new BillPaymentResponse(infoPay, null, null);

					transactionStatus = Constants.Status.Fail;

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

		catch (

		Exception e) {

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
						response.getInfo().getResponseCode(), response.getInfo().getResponseDesc(), billerName,
						request.getTxnInfo().getBillNumber(), request.getTxnInfo().getBillerId(), amountInDueDate,
						amountAfterDueDate, Constants.ACTIVITY.BillPayment, transactionStatus, channel, billStatus,
						request.getTxnInfo().getTranDate(), request.getTxnInfo().getTranTime(), transAuthId,
						new BigDecimal(request.getTxnInfo().getTranAmount()), dueDate, billingMonth, paymentRefrence,
						bankName, bankCode, branchName, branchCode, "", username, "");

				LOG.info(" --- Bill Payment Method End --- ");

			} catch (Exception ex) {
				LOG.error("{Exception payment Logs}", ex);
			}

		}
		return response;
	}

	@Override
	public BillPaymentResponse billPaymentPu(BillPaymentRequest request, HttpServletRequest httpRequestData) {

		LOG.info("Pu Bill Payment Request {} ", request.toString());

		BillPaymentResponse response = null;
		PuBillPaymentResponse puBillPaymentResponse = null;
		PuBillInquiryResponse puBillInquiryResponse = null;
		Date requestedDate = new Date();
		InfoPay infoPay = null;
		TxnInfoPay txnInfoPay = null;
		AdditionalInfoPay additionalInfoPay = null;
		String rrn = request.getInfo().getRrn(); // utilMethods.getRRN();
		String paymentRefrence = utilMethods.getRRN();
		String stan = request.getInfo().getStan();
		LOG.info("RRN :{ }", rrn);
		String transAuthId = request.getTxnInfo().getTranAuthId();
		String channel = "", username = "";

		ArrayList<String> inquiryParams = new ArrayList<String>();
		ArrayList<String> paymentParams = new ArrayList<String>();

		BigDecimal amountInDueDate = null, amountAfterDueDate = null, txnAmount = null;
		String billStatus = "", transactionStatus = "", billerId = "", billerNumber = "", billerName = "",
				billingMonth = "", dueDate = "", cardType = "", ru_Code = "", customer_Id = "", amountInDueDateRes = "",
				amountAfterDueDateRes = "";
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

			inquiryParams.add(Constants.MPAY_REQUEST_METHODS.PU_BILL_INQUIRY);
			inquiryParams.add(request.getTxnInfo().getBillNumber().trim());
			inquiryParams.add(rrn);
			inquiryParams.add(stan);

			//// Inquiry Call to M-Pay

			puBillInquiryResponse = serviceCaller.get(inquiryParams, PuBillInquiryResponse.class, rrn,
					Constants.ACTIVITY.BillInquiry, BillerConstant.PU.PU);

			if (puBillInquiryResponse != null) {

				if (puBillInquiryResponse.getPuBillInquiry() == null) {

					infoPay = new InfoPay(Constants.ResponseCodes.SERVICE_FAIL,
							Constants.ResponseDescription.SERVICE_FAIL, rrn, stan);

					response = new BillPaymentResponse(infoPay, null, null);

					transactionStatus = Constants.Status.Fail;

					return response;

				}

				//// consumer number not exsist

				else if (puBillInquiryResponse.getPuBillInquiry().getResponseCode()
						.equalsIgnoreCase(Constants.ResponseCodes.CONSUMER_NUMBER_NOT_EXISTS)) {

					infoPay = new InfoPay(Constants.ResponseCodes.CONSUMER_NUMBER_NOT_EXISTS,
							Constants.ResponseDescription.CONSUMER_NUMBER_NOT_EXISTS, rrn, stan);

					response = new BillPaymentResponse(infoPay, null, null);

					return response;
				}

				//// Already Paid

				else if (puBillInquiryResponse.getPuBillInquiry().getResponseCode()
						.equalsIgnoreCase(Constants.ResponseCodes.BILL_ALREADY_PAID)) {

					billerId = request.getTxnInfo().getBillerId();
					billerNumber = request.getTxnInfo().getBillNumber();

					infoPay = new InfoPay(Constants.ResponseCodes.BILL_ALREADY_PAID,
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

					billStatus = Constants.BILL_STATUS.BILL_PAID;

					response = new BillPaymentResponse(infoPay, txnInfoPay, additionalInfoPay);
					return response;

				}

				/////// Inquiry success response

				else if (puBillInquiryResponse.getPuBillInquiry().getResponseCode()
						.equalsIgnoreCase(Constants.ResponseCodes.OK)) {

					PendingPayment pendingPayment = pendingPaymentRepository
							.findFirstByVoucherIdAndBillerIdOrderByPaymentIdDesc(
									request.getTxnInfo().getBillNumber().trim(),
									request.getTxnInfo().getBillerId().trim());

					if (pendingPayment != null) {

						if (pendingPayment.getIgnoreTimer()) {

							infoPay = new InfoPay(Constants.ResponseCodes.UNKNOWN_ERROR, pendingPaymentMessage, rrn,
									stan);
							response = new BillPaymentResponse(infoPay, null, null);
							transactionStatus = Constants.Status.Pending;
							billStatus = Constants.BILL_STATUS.BILL_PENDING;
							return response;

						} else {
							LocalDateTime transactionDateTime = pendingPayment.getTransactionDate();
							LocalDateTime now = LocalDateTime.now(); // Current date and time

							// Calculate the difference in minutes
							long minutesDifference = Duration.between(transactionDateTime, now).toMinutes();

							if (minutesDifference <= pendingThresholdMinutes) {

								infoPay = new InfoPay(Constants.ResponseCodes.UNKNOWN_ERROR, pendingPaymentMessage, rrn,
										stan);
								response = new BillPaymentResponse(infoPay, null, null);

								transactionStatus = Constants.Status.Pending;
								billStatus = Constants.BILL_STATUS.BILL_PENDING;
								return response;

							}
						}
					}

					LOG.info("Calling Payment Inquiry from pg_payment_log table");
					PgPaymentLog pgPaymentLog = pgPaymentLogRepository.findFirstByVoucherIdAndBillerIdAndBillStatus(
							request.getTxnInfo().getBillNumber(), request.getTxnInfo().getBillerId(),
							Constants.BILL_STATUS.BILL_PAID);

					if (pgPaymentLog != null
							&& pgPaymentLog.getTransactionStatus().equalsIgnoreCase(Constants.Status.Success)) {

						infoPay = new InfoPay(Constants.ResponseCodes.UNKNOWN_ERROR, pendingVoucherUpdateMessage, rrn,
								stan); // success

						transactionStatus = Constants.Status.Success;
						billStatus = Constants.BILL_STATUS.BILL_PAID;

						response = new BillPaymentResponse(infoPay, null, null);

						return response;
					}

					billerId = request.getTxnInfo().getBillerId();
					billerNumber = request.getTxnInfo().getBillNumber();

					billerName = puBillInquiryResponse.getPuBillInquiry().getPuBillInquiryData().getCandidateName();

					amountInDueDateRes = puBillInquiryResponse.getPuBillInquiry().getPuBillInquiryData()
							.getVoucherAmount();

					amountInDueDate = new BigDecimal(
							puBillInquiryResponse.getPuBillInquiry().getPuBillInquiryData().getVoucherAmount());
					amountInDueDate = amountInDueDate.setScale(2, RoundingMode.UP);

					txnAmount = new BigDecimal(request.getTxnInfo().getTranAmount());

					///// Check due date conditions///

					if (Double.valueOf(request.getTxnInfo().getTranAmount())
							.compareTo(Double.valueOf(amountInDueDateRes)) != 0) {
						infoPay = new InfoPay(Constants.ResponseCodes.AMMOUNT_MISMATCH,
								Constants.ResponseDescription.AMMOUNT_MISMATCH, rrn, stan);
						response = new BillPaymentResponse(infoPay, null, null);
						return response;
					}

					//// M-Pay call to Payment

					paymentParams.add(Constants.MPAY_REQUEST_METHODS.PU_BILL_PAYMENT);
					paymentParams.add(request.getTxnInfo().getBillNumber().trim());
					paymentParams.add(rrn);
					paymentParams.add(request.getTxnInfo().getTranAmount());
					paymentParams.add(puChannel);
					paymentParams.add(rrn);
					paymentParams.add(stan);

					puBillPaymentResponse = serviceCaller.get(paymentParams, PuBillPaymentResponse.class, rrn,
							Constants.ACTIVITY.BillPayment, BillerConstant.PU.PU);

					if (puBillPaymentResponse != null) {

						if (puBillPaymentResponse.getPuBillPayment() == null) {

							infoPay = new InfoPay(Constants.ResponseCodes.SERVICE_FAIL,
									Constants.ResponseDescription.SERVICE_FAIL, rrn, stan);

							response = new BillPaymentResponse(infoPay, null, null);

							transactionStatus = Constants.Status.Fail;

							return response;

						}

						else if (puBillPaymentResponse.getPuBillPayment().getResponseCode()
								.equalsIgnoreCase(Constants.ResponseCodes.OK)) {

							infoPay = new InfoPay(Constants.ResponseCodes.OK,
									Constants.ResponseDescription.OPERATION_SUCCESSFULL, rrn, stan);

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

							billStatus = Constants.BILL_STATUS.BILL_PAID;

							return response;

						}

						else {

							infoPay = new InfoPay(Constants.ResponseCodes.UNKNOWN_ERROR,
									Constants.ResponseDescription.UNKNOWN_ERROR, rrn, stan);
							response = new BillPaymentResponse(infoPay, null, null);
							transactionStatus = Constants.Status.Fail;
						}

					} else {

						infoPay = new InfoPay(Constants.ResponseCodes.SERVICE_FAIL,
								Constants.ResponseDescription.SERVICE_FAIL, rrn, stan);

						response = new BillPaymentResponse(infoPay, null, null);

						transactionStatus = Constants.Status.Fail;

						return response;

					}

				}

				else {

					infoPay = new InfoPay(Constants.ResponseCodes.UNKNOWN_ERROR,
							Constants.ResponseDescription.UNKNOWN_ERROR, rrn, stan);

					response = new BillPaymentResponse(infoPay, null, null);

					transactionStatus = Constants.Status.Fail;

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

		catch (

		Exception e) {

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
						response.getInfo().getResponseCode(), response.getInfo().getResponseDesc(), billerName,
						request.getTxnInfo().getBillNumber(), request.getTxnInfo().getBillerId(), amountInDueDate,
						amountAfterDueDate, Constants.ACTIVITY.BillPayment, transactionStatus, channel, billStatus,
						request.getTxnInfo().getTranDate(), request.getTxnInfo().getTranTime(), transAuthId,
						new BigDecimal(request.getTxnInfo().getTranAmount()), dueDate, billingMonth, paymentRefrence,
						bankName, bankCode, branchName, branchCode, "", username, "");

				LOG.info(" --- Bill Payment Method End --- ");

			} catch (Exception ex) {
				LOG.error("{Exception payment Logs}", ex);
			}

		}
		return response;
	}

}
