package com.gateway.model.mpay.response.billinquiry;

import java.io.Serializable;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.gateway.model.mpay.response.billinquiry.Response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@XmlRootElement(name = "response")
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
@ToString
public class GetVoucherResponse implements Serializable {
	private static final long serialVersionUID = 8396436785949897705L;
	private Response response;
	
	

	
	
	
	
}
