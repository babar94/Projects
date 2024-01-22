package com.gateway.request.billinquiry;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
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
public class OneLinkBillInquiryRequest implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8396436785949897705L;

	@NotEmpty(message = "Consumer number must not be empty")
	//@Size(min = 5	, max = 24, message = "Consumer number must be between 5 and 24 characters")
	@JsonProperty(value = "consumer_number")
	@NotEmpty(message = "Consumer number must not be empty")
	@Size(min = 5, max = 24, message = "Consumer number must be between 5 and 24 characters")
	private String consumerNumber;

	@Size(min = 3, max = 8, message = "Bank mnemonic/Utility Company Id code must have at most 8 characters")
	@JsonProperty(value = "bank_mnemonic")
	private String bankMnemonicCode;


	@Size(max = 400, message = "Reserved field must be up to 400 characters")
	private String reserved;



}
