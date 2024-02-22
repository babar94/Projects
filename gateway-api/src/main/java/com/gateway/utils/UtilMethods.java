package com.gateway.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;
import java.util.StringTokenizer;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.gateway.entity.MPAYLog;
import com.gateway.repository.MPAYLogRepository;

import jakarta.servlet.http.HttpServletRequest;

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

	public String getDueDate(String tranDate) {
		String strDate = "";
		Date today = null;
		try {
			today = new SimpleDateFormat("yyyyMMdd").parse(tranDate);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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

	// Muhammad Said
	// Utility method to check if the payment is within the due date
	public boolean isPaymentWithinDueDate(LocalDate currentDate, LocalDate dueDate) {
		return currentDate.isEqual(dueDate) || currentDate.isBefore(dueDate);
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
	public void insertMpayLog(String type, Date date, String userName, String rrn, String reqRes) {
		MPAYLog temp = new MPAYLog();
		temp.setType(type);
		temp.setStampDate(date);
		temp.setReqRes(reqRes);
		temp.setRrn(rrn);
		temp.setUserName(userName);
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

//		public String convertAmountToISOFormat(String amount) {
//			StringBuilder resultAmount = new StringBuilder();
//			int decimalPlaces = amount.length() - amount.indexOf(".") - 1;
//			if(decimalPlaces<2) {
//				amount = amount.replace(".", "0");
//			}else {
//				amount = amount.replace(".", "");
//			}
//			
//			int additionalZerosLenght = 14 - amount.length();
//			try {
//				
//				resultAmount.append("+");
//
//				for (int j = 0; j < additionalZerosLenght; j++) {
//					resultAmount.append("0");
//				}
//
//				resultAmount = resultAmount.append(amount);
//			} catch (Exception ex) {
//				ex.printStackTrace();
//			}
//			return resultAmount.toString();
//		}

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
			ex.printStackTrace();
		}

		return resultAmount.toString();
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
			ex.printStackTrace();
		}
		return resultAmount;
	}

	// muhammad Sajid

	public String removeHyphen(String dateBillingMonth) {

		String result = dateBillingMonth.replace("-", "");
		return result;
	}

	// muhammad Sajid

//	public double bigDecimalToDouble(BigDecimal decimal) {
////			DecimalFormat df = new DecimalFormat("0.00");
////			double value;
////			value= Double.parseDouble(df.format(decimal));
////		    
////		    String formatted = String.format("%.2f", value);
////		    double parsedValue = Double.parseDouble(formatted);
////		   
////		    return parsedValue;
//		DecimalFormat df = new DecimalFormat("0.00");
//		return Double.parseDouble(df.format(decimal));
//	}
//	public double bigDecimalToDouble(BigDecimal decimal) {
//		// Format BigDecimal as a string without scientific notation
//		return decimal.doubleValue();
//	}

	// muhammad sajid

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

}
