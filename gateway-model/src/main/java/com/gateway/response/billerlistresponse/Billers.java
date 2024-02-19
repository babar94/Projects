package com.gateway.response.billerlistresponse;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;


@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class Billers implements Serializable{
	private static final long serialVersionUID = 8396436785949897705L;

	private String billerId;
	private String billerName;
	
	
}
