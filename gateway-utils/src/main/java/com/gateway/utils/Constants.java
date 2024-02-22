package com.gateway.utils;

public class Constants {
	public static final String NBP_IMD = "979898";
	public static final String PAN = "6014921111111111111";

	public static class ResponseCodes {

		public static final String OK = "00";
		public static final String CONSUMER_NUMBER_NOT_EXISTS = "01";

		public static final String CONSUMER_NUMBER_BLOCK = "02";
		public static final String UNKNOWN_ERROR = "03";
		public static final String INVALID_DATA = "04";
		public static final String BILLER_NOT_FOUND_DISABLED = "04";
		public static final String INVALID_BILLER_ID = "04";
		public static final String BILLER_DISABLED = "04";
		public static final String SERVICE_FAIL = "05";
		public static final String BILL_ALREADY_PAID = "06";
		public static final String BAD_TRANSACTION = "07";
		public static final String DUPLICATE_TRANSACTION = "08";
		public static final String PAYMENT_NOT_FOUND = "09";
		public static final String UNABLE_TO_PROCESS = "10";
		public static final String AMMOUNT_MISMATCH = "11";
		public static final String DUPLICATE_TRANSACTION_AUTH_ID = "12";
		public static final String TRANSACTION_CAN_NOT_BE_PROCESSED = "13";

		public static final String DISABLED_EXCEPTION = "400";
		public static final String UNAUTHORISED = "401";
		public static final String INVALID_VALIDATION = "400";
		public static final String NOT_FOUND = "404";
		public static final String OFFLINE_SERVICE_FAIL = "99";
		public static final String INTERNAL_SERVER_ERROR = "500";

	}

	public static class ResponseDescription {
		public static final String OK = "SUCCESS";
		public static final String CONSUMER_NUMBER_NOT_EXISTS = "Consumer Number does not exist.";
		public static final String CONSUMER_NUMBER_BLOCK = "Consumer Number Block.";
		public static final String UNKNOWN_ERROR = "Unknown Error / Bad Transaction.";
		public static final String INVALID_DATA = "Invalid Data.";
		// public static final String INVALID_BILLER_DATA = "Invalid Biller Id.";
		public static final String SERVICE_FAIL = "Processing/Service Failed";
		public static final String BILL_ALREADY_PAID = "Bill Already Paid";
		public static final String BAD_TRANSACTION = "Unknown Error / Bad Transaction";
		public static final String DUPLICATE_TRANSACTION = "Duplicate Transaction";
		public static final String PAYMENT_NOT_FOUND = "Payment not found.";
		public static final String UNABLE_TO_PROCESS = "Unable to process at this time, please try again later.";
		public static final String AMMOUNT_MISMATCH = "Amount mismatch.";
		public static final String DUPLICATE_TRANSACTION_AUTH_ID = "Invalid Tran-Auth Id.";

		public static final String DISABLED_EXCEPTION = "Disabled Exception";
		public static final String UNAUTHORISED_WRONG_CREDENTIALS = "Unauthorized – Wrong Credentials";
		public static final String UNAUTHORISED_WRONG_CHANNEL = "Unauthorized – Wrong Username Or Channel is disabled";
		public static final String INVALID_INPUT_DATA = "Invalid input data";
		public static final String OFFLINE_SERVICE_FAIL = "Processing Failed";
		public static final String Biller_Disabled = "Biller Disabled";
		public static final String BILLER_NOT_FOUND_DISABLED = "Biller not found";
		public static final String INVALID_BILLER_ID = "Invalid billerId";
		public static final String CONSUMER_NUMBER_Expired = "Consumer Number Expired";
		public static final String INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR";
		public static final String TRANSACTION_CAN_NOT_BE_PROCESSED = "Your transaction cannot be processed. Please contact NBP Helpline 021-111627627";

	}

	public static class ACTIVITY {
		public static final String BillInquiry = "BillInquiry";
		public static final String BillPayment = "BillPayment";
		public static final String PaymentInquiry = "PaymentInquiry";
		public static final String GetBillerList = "GetBillerList";

	}

	public static class Channel {
		public static final String E_SAHULAT = "E_Sahulat";
	}

	public static class Status {
		public static final String Success = "Success";
		public static final String Fail = "Fail";
		public static final String Processing = "Processing";
		public static final String Pending = "Pending";
		public static final String VOUCHER_UPDATED = "1";
	}

	public static class MPAY_REQUEST_METHODS {

		public static final String BEOE_BILL_INQUIRY = "GetVoucher"; // Nadra Call for voucher inquiry
		public static final String BEOE_BILL_PAYMENT = "UpdateVoucher"; // Nadra Call for voucher payment
		public static final String PRAL_BILL_INQUIRY = "PAGetPaymentByPSID"; // Nadra Call for voucher inquiry
		public static final String PRAL_BILL_PAYMENT = "PAUpdateBillPaymentStatus"; // Nadra Call for voucher payment
		public static final String OFFLINE_BILLER_INQUIRY = "offline-biller-getvoucher";
		public static final String OFFLINE_BILLER_PAYMENT = "offline-biller-updatevoucher";
		public static final String PTA_BILL_INQUIRY = "pta-getvoucher";
		public static final String PTA_BILL_PAYMENT = "pta-updatevoucher";
		public static final String PRAL_FBR_BILL_INQUIRY = "pral-fbr-getvoucher";
		public static final String PRAL_FBR_BILL_PAYMENT = "pral-fbr-updatevoucher";
		public static final String AIOU_BILL_INQUIRY = "aiou-getvoucher";
		public static final String AIOU_BILL_PAYMENT = "aiou-updatevoucher";

	}

	public static class BILL_STATUS {

		public static final String BILL_PAID = "Paid"; // Nadra Call for voucher inquiry
		public static final String BILL_UNPAID = "Unpaid"; // Nadra Call for voucher payment
		public static final String BILL_BLOCK = "Block"; // Nadra Call for voucher payment
		public static final String BILL_EXPIRED = "Expired";

	}

	public static class BillerType {

		public static final String OFFLINE_BILLER = "offline";
		public static final String ONLINE_BILLER = "online";

	}

	public static class BankMnemonic {
		public static final String NBP = "NBP";
		public static final String ABL = "ABL";

	}

}
