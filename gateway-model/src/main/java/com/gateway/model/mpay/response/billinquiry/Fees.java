package com.gateway.model.mpay.response.billinquiry;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Fees implements Serializable{
	private static final long serialVersionUID = 8396436785949897705L;

	private String amount;
	private String fee_head;
}
