package com.gateway.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data

public class BillerCredentialRequest {

	@NotBlank(message = "Biller ID must not be blank")
	private String billerId;

	@NotBlank(message = "Biller name must not be blank")
	private String billerName;

	@NotBlank(message = "Username must not be blank")
	private String username;

	@NotBlank(message = "Password must not be blank")
	private String password;

}
