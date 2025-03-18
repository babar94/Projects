package com.gateway.utils;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.StringTokenizer;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.gateway.entity.MPAYLog;
import com.gateway.model.mpay.response.billinquiry.bppra.ChallanFee;
import com.gateway.model.mpay.response.billinquiry.dls.DlsGetVoucherResponse;
import com.gateway.model.mpay.response.billinquiry.dls.FeeTypeListWrapper;
import com.gateway.repository.MPAYLogRepository;
import com.gateway.utils.BillerConstant.Aiou;

import jakarta.servlet.http.HttpServletRequest;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;
import kong.unirest.json.JSONObject;

@Component
public class UtilMethods {

	private static final Logger LOG = LoggerFactory.getLogger(UtilMethods.class);

	@Autowired
	private MPAYLogRepository mpayLogRepository;

	private static int counter = (new Random()).nextInt(100);
	private static String sRRNformat = "ddHHmms";
	private static final DecimalFormat df = new DecimalFormat("0.00");

	final static int RETRIEVAL_REFERENCE_LENGTH = 12;

	public static void generalLog(String text, Logger LOG) {
		LOG.info(text);
	}

	public synchronized String getRRN() {
		if (counter < 9999) {
			counter++;
		} else {
			counter = 0;
		}
		return StringUtils.leftPad((getDateTime() + String.valueOf(counter)), RETRIEVAL_REFERENCE_LENGTH, '0');
	}

	public synchronized String getStan() {
		if (counter < 9999) {
			counter++;
		} else {
			counter = 0;
		}
		return getRRN().substring(6);
	}

	public String getDateTime() {
		DateFormat dateFormat = new SimpleDateFormat(sRRNformat);
		Date date = new Date();
		return dateFormat.format(date);
	}

	public long getEpoochDateTime(Date date) {
		return date.getTime();
	}

	public String transactionDateFormater(String dob) {
		String strDate = "";
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		try {
			Date varDate = dateFormat.parse(dob);
			dateFormat = new SimpleDateFormat("yyyyMMdd");
			strDate = dateFormat.format(varDate);
		} catch (Exception e) {
			strDate = dob;
		}

		return strDate;

	}

	// Muhamamd Sajid

	public String formatDateString(String inputDate) {
		LocalDate date = LocalDate.parse(inputDate, DateTimeFormatter.BASIC_ISO_DATE);
		String formattedDate = date.format(DateTimeFormatter.ofPattern("yyMM"));
		return formattedDate;
	}

	public String formatStringDate(String inputDate) {
		LocalDate date = LocalDate.parse(inputDate, DateTimeFormatter.BASIC_ISO_DATE);
		String formattedDate = date.format(DateTimeFormatter.ofPattern("yy-MM-dd"));
		return formattedDate;
	}

	public String formatDateFormat(String inputDate) {
		try {
			// Try parsing as LocalDateTime
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
			LocalDateTime dateTime = LocalDateTime.parse(inputDate, formatter);

			// Extract only date part
			LocalDate date = dateTime.toLocalDate();

			// Convert to YYMM format
			return date.format(DateTimeFormatter.ofPattern("yyMM"));
		} catch (DateTimeParseException e) {

			LOG.error("Date Formatting error for input: {} with pattern {}: {}", inputDate, e.getMessage());
			return "Invalid Date";
		}

	}

	public String getDueDate(String tranDate) {
		String strDate = "";
		Date today = null;
		try {
			today = new SimpleDateFormat("yyyyMMdd").parse(tranDate);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			LOG.info("UtilsMethod-DueDateError :" + e);
		}
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(today);

		calendar.add(Calendar.MONTH, 1);
		calendar.set(Calendar.DAY_OF_MONTH, 1);
		calendar.add(Calendar.DATE, -1);

		Date lastDayOfMonth = calendar.getTime();

		DateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		strDate = sdf.format(lastDayOfMonth);

		return strDate;

	}

	// Muhammad sajid
	public boolean isValidInput(String input) {
		return input != null && !input.isEmpty();
	}

	// Muhammad Sajids
	// Utility method to parse the due date
	public LocalDate parseDueDate(String dueDateStr) {
		return LocalDate.parse(dueDateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
	}
	// Muhammad Sajids

	public LocalDate parseDueDateWithoutDashes(String dueDateStr) {
		return LocalDate.parse(dueDateStr, DateTimeFormatter.ofPattern("yyyyMMdd"));
	}

	public Date parseDate(String dateString) throws ParseException {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		return dateFormat.parse(dateString);
	}

	/// formate date

	public String formatDueDate(String dueDate) {

		DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("dd-MM-yy");
		DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
		String formattedDate = "";
		try {
			LocalDate date = LocalDate.parse(dueDate, inputFormatter);
			formattedDate = date.format(outputFormatter);
			System.out.println("Formatted date: " + formattedDate);
		} catch (DateTimeParseException e) {
			System.out.println("Error parsing date: " + e.getMessage());
		}

		return formattedDate;
	}
	
	
	public String DueDate(String inputDate) {
		
        LocalDate date = Instant.parse(inputDate).atZone(ZoneId.of("UTC")).toLocalDate();
        
        // Format to yyyyMMdd
        String formattedDate = date.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        System.out.println(formattedDate);
		
        return formattedDate;
	}
	
     public String getFormattedBillingMonth(String billingmonth) {
    	 
    	 try {
             DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM-yy", Locale.ENGLISH);
             YearMonth yearMonth = YearMonth.parse(billingmonth, formatter);

             return yearMonth.format(DateTimeFormatter.ofPattern("yyMM"));

         } catch (DateTimeParseException e) {
             return "Invalid date format";
         }
    
     }
    	 
        	 	 
     public String getFormattedDueDate(String dueDate) {
    	 
    	 try {
             // Define the input format (DDMMYYYY)
             DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("MMddyyyy");
             LocalDate date = LocalDate.parse(dueDate, inputFormatter);

             // Define the output format (YYYYMMDD)
             DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
             return date.format(outputFormatter);
         } catch (DateTimeParseException e) {
             return "Invalid date format";
         }
    	 
    	 
     }
	

	// Muhammad Said
	// Utility method to check if the payment is within the due date
	public boolean isPaymentWithinDueDate(LocalDate currentDate, LocalDate dueDate) {
		return currentDate.isEqual(dueDate) || currentDate.isBefore(dueDate);
	}

	// Muhammad Said
	public static String formatString(String value) {
		return value != null && !value.isEmpty() ? String.format("%-30s", value) : "Please Fill";
	}

	// Ahmed Ashraf
	public static String padRight(String value, int n, boolean defaultvalue, String Tag) {

		if (defaultvalue && Tag.equalsIgnoreCase(Aiou.AIOU))
			return (value != null && !value.isEmpty() ? String.format("%-" + n + "s", value) : "Please Fill");
		else
			return String.format("%-" + n + "s", value);
	}

	public static String padLeft(String value, int n, boolean defaultvalue, String Tag) {

		if (defaultvalue && Tag.equalsIgnoreCase(Aiou.AIOU))
			return (value != null && !value.isEmpty() ? String.format("%" + n + "s", value) : "Please Fill");
		else
			return String.format("%" + n + "s", value);
	}

	public String getClientIp(HttpServletRequest request) {

		String remoteAddr = "";
		// X-FORWARDED-FOR
		if (request != null) {
			remoteAddr = request.getHeader("X-FORWARDED-FOR");
			if (remoteAddr == null || "".equals(remoteAddr)) {
				remoteAddr = request.getRemoteAddr();
			}
		}

		LOG.info("IPADDRESS(before): '{}'", remoteAddr);

		if (remoteAddr.contains(":") && remoteAddr.contains(",")) {
			remoteAddr = new StringTokenizer(remoteAddr, ",").nextToken().trim();
			remoteAddr = remoteAddr.substring(0, remoteAddr.indexOf(":"));
		}

		LOG.info("IPADDRESS(after): '{}'", remoteAddr);

		return remoteAddr;
	}

	public String leftPad(int length, String text, char with) {

		while (text.length() < length) {
			text = with + text;
		}
		return text;

	}

	@Async
	public void insertMpayLog(String type, Date date, String userName, String rrn, String reqRes, String billername) {
		MPAYLog temp = new MPAYLog();
		temp.setType(type);
		temp.setStampDate(date);
		temp.setReqRes(reqRes);
		temp.setRrn(rrn);
		temp.setUserName(userName);
		temp.setBillername(billername);
		mpayLogRepository.save(temp);
	}

	public static String getAggregatorId(String consumerNumber) {
		String aggregatorId = consumerNumber.substring(0, 4);
		return aggregatorId;
	}

	public static String getBillerNumber(String consumerNumber) {
		String billerId = consumerNumber.substring(4);
		return billerId;
	}

	// muhamamad Sajid updated IsoFormatTo 14 digit format
	public String convertAmountToISOFormat(String amount) {
		StringBuilder resultAmount = new StringBuilder();
		int decimalPlaces = amount.length() - amount.indexOf(".") - 1;

		if (decimalPlaces < 2) {
			amount = amount.replace(".", "0");
		} else {
			amount = amount.replace(".", "");
		}

		int additionalZerosLength = 14 - amount.length() - 1; // Adjusted to account for the "+" sign

		try {
			if (amount.startsWith("-")) {
				resultAmount.append("-");
				additionalZerosLength -= 1; // Adjust for the negative sign
			} else {
				resultAmount.append("+");
			}

			for (int j = 0; j < additionalZerosLength; j++) {
				resultAmount.append("0");
			}

			resultAmount.append(amount.substring(amount.indexOf("+") + 1)); // Exclude the leading zeros and the "+"
																			// sign
		} catch (Exception ex) {
			LOG.info("UtilsMethod-convertAmountToISOFormat" + ex);
		}

		return resultAmount.toString();
	}

	public String convertAmountToISOFormatWithoutPlusSign(String amount) {
		StringBuilder resultAmount = new StringBuilder();
		int decimalPlaces = amount.length() - amount.indexOf(".") - 1;

		if (decimalPlaces < 2) {
			amount = amount.replace(".", "0");
		} else {
			amount = amount.replace(".", "");
		}

		int additionalZerosLength = 14 - amount.length(); // Adjusted to account for the sign

		try {
			if (amount.startsWith("-")) {
				resultAmount.append("-");
				additionalZerosLength -= 1; // Adjust for the negative sign
			}

			for (int j = 0; j < additionalZerosLength; j++) {
				resultAmount.append("0");
			}

			resultAmount.append(amount.charAt(0)); // Append the sign
			resultAmount.append(amount.substring(1)); // Exclude the leading zeros and the sign
		} catch (Exception ex) {
			LOG.info("UtilsMethod-convertAmountToISOFormatWithoutPlusSign" + ex);

		}

		return resultAmount.toString();
	}

	// muhammad updated on 23-02-24 for AIOU in 13 digit format
	public String formatAmountAn13(double amount) {
		// Step 1: Remove the decimal point and currency symbol
		long amountInMinorUnits = (long) (amount * 100);

		// Step 2: Left-pad with zeros to make it 13 digits
		String formattedAmount = String.format("%013d", amountInMinorUnits);

		return formattedAmount;
	}

	public String convertISOFormatAmount(String amount) {
		Double doubleAmount = 0d;
		String resultAmount = "";
		try {
			amount = amount.replace("+0", "");
			doubleAmount = Double.parseDouble(amount) / 100;
			// resultAmount = doubleAmount.toString();
			resultAmount = df.format(doubleAmount);

		} catch (Exception ex) {
			LOG.info("UtilsMethod-convertISOFormatAmount : " + ex);

		}
		return resultAmount;
	}

	// muhammad Sajid

	public String removeHyphen(String dateBillingMonth) {

		String result = dateBillingMonth.replace("-", "");
		return result;
	}

	public String formatAmountIso(double amount, int n) {
		// Step 1: Remove the decimal point and currency symbol
		long amountInMinorUnits = (long) (amount * 100);

		// Step 2: Left-pad with zeros to make it 12 digits
		String formattedAmount = String.format("%0" + n + "d", amountInMinorUnits);

		return formattedAmount;
	}

	// muhammad Sajid

	// muhammad sajid

	public BigDecimal FormatStringToBigDecimal(String amountStr) {
		if (!amountStr.isEmpty()) {
			// Remove leading zeros and convert to BigDecimal
			BigDecimal amount = new BigDecimal(amountStr.replaceFirst("^\\+?0+", ""));
			amount = amount.divide(BigDecimal.valueOf(100));

			// Set scale to 2 and round up
			return amount.setScale(2, RoundingMode.UP);
		}
		return null;
	}

	public double bigDecimalToDouble(BigDecimal decimal) {
		// Format BigDecimal as a string without scientific notation
		return decimal.doubleValue();
	}

	public String formatAmount(BigDecimal amount) {
		// Format as a currency with two decimal places
		DecimalFormat decimalFormat = new DecimalFormat("0.00");
		return decimalFormat.format(amount);
	}

	public String formatAmount(BigDecimal amount, int length) {
		// Scale the BigDecimal to two decimal places
		BigDecimal scaledAmount = amount.setScale(2, RoundingMode.HALF_UP);

		// Extract the integer part and the last two digits
		int integerPart = scaledAmount.intValue();
		int lastTwoDigits = integerPart % 100;

		// Ensure that the last two digits are minimized within the specified range
		lastTwoDigits = Math.min(lastTwoDigits, 99); // Minimize to two digits

		// Construct the final formatted string with left-padding and zeros at the end
		String formattedString = String.format("%0" + (length - 4) + "d%02d00", integerPart / 100, lastTwoDigits);

		return formattedString.substring(formattedString.length() - length);

	}

	public boolean isJSON(String data) {
		try {
			new JSONObject(data);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	//////// Dls

	public String formatFeeTypeList(DlsGetVoucherResponse dlsGetVoucherResponse) {
		StringBuilder formattedResponse = new StringBuilder();
		List<FeeTypeListWrapper> feeTypeList = dlsGetVoucherResponse.getResponse().getDlsgetvoucher()
				.getFeeTypesList_wrapper();

		for (FeeTypeListWrapper feeType : feeTypeList) {
			formattedResponse.append("feesType: ").append(feeType.getFeesType()).append(", ");
			formattedResponse.append("typeDetail: ").append(feeType.getTypeDetail()).append(", ");
			formattedResponse.append("fees: ").append(feeType.getFees()).append(" || ");

		}

		// Remove the last " || " from the formatted response
		if (formattedResponse.length() >= 4) {
			formattedResponse.setLength(formattedResponse.length() - 4);
		}

		return formattedResponse.toString();
	}

	public HttpResponse<String> authRequest(String bppraAuthticateCall, String bppraClientSecret) {
		try {
			LOG.info("---- Auth token initiated ----");
			return Unirest.post(bppraAuthticateCall).header("clientSecret", bppraClientSecret).asString();
		} catch (UnirestException e) {
			LOG.info("---- Auth api failure ----");
			return null; // Indicate request failure
		}
	}

	public HttpResponse<String> tokenInquiryRequest(String bppraInitiateInquiryCall, String authToken,
			String requestFormAuth, String publicKey) {
		try {
			LOG.info("---- Token inquiry initiated ----");
			return Unirest.post(bppraInitiateInquiryCall).header("Authorization", authToken)
					.header("requestfrom", requestFormAuth).header("RSApublicKey", publicKey).asString();
		} catch (UnirestException e) {
			LOG.info("---- Initiate Token Inquiry api failure ----");
			return null; // Indicate request failure
		}
	}

	public HttpResponse<String> tenderChallanInquiryRequest(String bppraTenderChallanInquiryCall, String billNumber,
			String requestFormChallanEnquire, String jwt) {

		try {

			LOG.info("---- Tender challan inquiry initiated ----");
			return Unirest.get(bppraTenderChallanInquiryCall).queryString("challancode", billNumber)
					.header("requestfrom", requestFormChallanEnquire).header("Authorization", "Bearer " + jwt)
					.asString();
		} catch (UnirestException e) {
			LOG.info("---- Initiate Challan Inquiry api failure ----");
			return null; // Indicate request failure
		}
	}

	public HttpResponse<String> supplierChallanInquiryRequest(String bppraSupplierChallanInquiryCall, String billNumber,
			String requestFormChallanEnquire, String jwt) {

		try {
			LOG.info("---- Supplier challan inquiry initiated -----");

			return Unirest.get(bppraSupplierChallanInquiryCall).queryString("challancode", billNumber)
					.header("requestfrom", requestFormChallanEnquire).header("Authorization", "Bearer " + jwt)
					.asString();
		} catch (UnirestException e) {
			LOG.info("---- Supplier Challan Inquiry api failure ----");
			return null; // Indicate request failure
		}
	}

	public HttpResponse<String> tenderMarkChallanPaidRequest(String bppraTenderChallanPaid, String billNumber,
			String requestFormChallanEnquire, String jwt) {

		try {
			
			LOG.info("---- Tender mark challan paid initiated -----");

			return Unirest.post(bppraTenderChallanPaid).queryString("challanCode", billNumber).queryString("paid", true)
					.header("requestfrom", requestFormChallanEnquire).header("Authorization", "Bearer " + jwt)
					.asString();
		} catch (UnirestException e) {
			LOG.info("---- Tender Mark Challan Paid api failure ----");
			return null; // Indicate request failure
		}
	}

	public HttpResponse<String> supplierMarkChallanPaidRequest(String bppraSupplierChallanPaid, String billNumber,
			String requestFormChallanEnquire, String jwt) {

		try {
			
			LOG.info("---- Supplier mark challan paid initiated -----");

			return Unirest.post(bppraSupplierChallanPaid).queryString("challanCode", billNumber)
					.queryString("paid", true).header("requestfrom", requestFormChallanEnquire)
					.header("Authorization", "Bearer " + jwt).asString();
		} catch (UnirestException e) {
			LOG.info("---- Suppplier Mark Challan Paid api failure ----");
			return null; // Indicate request failure
		}
	}

	public HttpResponse<String> completeInquiryRequest(String bppraCompleteInquiryCall, String authToken,
			String requestFormAuth, String jwt, String branchcode, String personName) {

		try {
			
			LOG.info("---- Completed inquiry initiated ----");

			return Unirest.post(bppraCompleteInquiryCall).header("Authorization", authToken)
					.header("requestfrom", requestFormAuth).field("EnqueryToken", jwt).field("branchCode", branchcode)
					.field("personName", personName).asString();
		} catch (UnirestException e) {
			LOG.info("---- Complete Inquiry api failure ----");
			return null; // Indicate request failure
		}
	}

	public String DecodeJwt(String jwt) {

		String[] parts = jwt.split("\\.");
		String payload = parts[1];
		byte[] decodedBytes = Base64.getDecoder().decode(payload);
		String jwtkey = new String(decodedBytes);
		JSONObject jsonObject = new JSONObject(jwtkey);
		String subValue = jsonObject.getString("sub");
		System.out.println(subValue);
		return subValue;
	}

	public String aesPackedAlgorithm(String keyAndIv, String encryptedData) {

		String result = "";
		try {

			System.out.println("encryptedDataValue :" + encryptedData);
			String encryptedDataValue = encryptedData.replace("\"", "");

			JSONObject jsonObject = new JSONObject(keyAndIv);

			// Extract Key and Iv values
			String keyData = jsonObject.getString("Key");
			String ivData = jsonObject.getString("Iv");

			// Print results
			System.out.println("Key: " + keyData.trim());
			System.out.println("Iv: " + ivData.trim());

			// Decode Base64 encoded key, IV, and encrypted data
			byte[] decodedKey = Base64.getDecoder().decode(keyData);
			byte[] decodedIV = Base64.getDecoder().decode(ivData);
			byte[] decodedData = Base64.getDecoder().decode(encryptedDataValue);
			// Initialize cipher with AES in CBC mode
			SecretKeySpec secretKey = new SecretKeySpec(decodedKey, "AES");
			IvParameterSpec ivSpec = new IvParameterSpec(decodedIV);

			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);

			// Perform decryption
			byte[] decryptedData = cipher.doFinal(decodedData);

			// Convert decrypted data to string
			result = new String(decryptedData, "UTF-8");
			System.out.println("Decrypted Data: " + result);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return result;

	}

	public BigDecimal getTotalTenderFeeAmount(List<ChallanFee> challanFee) {
		return challanFee.stream().filter(fee -> "Total Tender Fee".equalsIgnoreCase(fee.getTariffTitle()))
				.map(fee -> BigDecimal.valueOf(fee.getAmount())) // Convert int to BigDecimal
				.findFirst().orElse(BigDecimal.ZERO); // Returns 0 if not found
	}

	public String ReadPublicPemFile(String publicKeyPemFilePath) {

		String singleLinePem = "";

		try {

			// Read file content as a string
			String pemContent = new String(Files.readAllBytes(Paths.get(publicKeyPemFilePath)));
			singleLinePem = pemContent.replace("\n", " ").replace("\r", " ");
			// Print the content
			System.out.println(singleLinePem);
		} catch (IOException e) {
			System.err.println("Error reading the PEM file: " + e.getMessage());
		}
		return singleLinePem;
	}

	public String ReadPrivatePemFile(String privateKeyPemFilePath) {

		String singleLinePem = "";

		try {

			// Read file content as a string
			String pemContent = new String(Files.readAllBytes(Paths.get(privateKeyPemFilePath)));
			singleLinePem = pemContent.replace("\n", " ").replace("\r", " ");
			// Print the content
			System.out.println(singleLinePem);
		} catch (IOException e) {
			System.err.println("Error reading the PEM file: " + e.getMessage());
		}
		return singleLinePem;
	}

}
