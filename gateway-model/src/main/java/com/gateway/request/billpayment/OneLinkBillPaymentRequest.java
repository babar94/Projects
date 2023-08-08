package com.gateway.request.billpayment;

import java.io.Serializable;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.hibernate.validator.constraints.Length;

import com.fasterxml.jackson.annotation.JsonProperty;
//import com.gateway.request.billinquiry.AdditionalInfoRequest;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class OneLinkBillPaymentRequest implements Serializable {

	private static final long serialVersionUID = 1L;

	@NotEmpty(message = "Consumer number must not be empty")
	@Size(min = 5, max = 24, message = "Consumer number must be between 5 and 24 characters")
	@JsonProperty(value = "consumer_number")
	private String consumerNumber;

	@Size(min = 6, max = 6, message = "Transaction authorization ID must be exactly 6 characters")
	@JsonProperty(value = "tran_auth_id")
	private String tranAuthId;

	@JsonProperty(value = "transaction_amount")
	private String transactionAmount;

	@Pattern(regexp = "\\d{8}", message = "Transaction date must be an 8-digit string (YYYYMMDD)")
	@JsonProperty(value = "tran_date")
	private String tranDate;

	@Pattern(regexp = "\\d{6}", message = "Transaction time must be a 6-digit string (HHMMSS)")
	@JsonProperty(value = "tran_time")
	private String tranTime;

	//@Length(min = 3, message = "Bank mnemonic code must have at least 3 characters")
	@Size(min = 3, max = 8, message = "Bank mnemonic/Utility Company Id code must have at most 8 characters")
	@JsonProperty(value = "bank_mnemonic")
	private String bankMnemonic;

	@Size(max = 400, message = "Reserved field must be up to 400 characters")
	@JsonProperty(value = "reserved")
	private String reserved;

}
