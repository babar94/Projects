package com.gateway.model.mpay.response.billinquiry.offline;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OfflineBillerGetvoucher {

	private Getvoucher getvoucher;

	
	
}
