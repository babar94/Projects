package com.gateway.model.mpay.response.billinquiry.vms;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VmsOfflineBillerGetvoucher {

	@JsonProperty(value = "getvoucher")
	private VmsGetvoucher getvoucher;

	
	
}
