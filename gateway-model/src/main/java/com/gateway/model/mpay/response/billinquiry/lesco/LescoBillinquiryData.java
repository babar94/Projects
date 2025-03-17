package com.gateway.model.mpay.response.billinquiry.lesco;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class LescoBillinquiryData {

	@JsonProperty("data_wrapper")
	private List<LescoBillData> dataWrapper;

}
