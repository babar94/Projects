package com.gateway.model.mpay.response.billinquiry.fbr;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

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
public class AiouGetVoucherResponse implements Serializable {
	private static final long serialVersionUID = 8396436785949897705L;

    // The actual response data
    private Response response;

}
