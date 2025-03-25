package com.gateway.model.mpay.response.billinquiry.bisekohat;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class BisekohatBillinquiry {

	@JsonProperty("data")
    @JsonDeserialize(using = NullStringToNullDeserializer.class) // Apply custom deserializer
	private BiseKohatBillinquiryData biseKohatBillinquiryData;

}
