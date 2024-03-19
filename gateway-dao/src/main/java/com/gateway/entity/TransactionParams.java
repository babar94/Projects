package com.gateway.entity;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "api_gw_transaction_params")
//@Table(name = "nbp_pgw_api_gw_transaction_params npagtp")
@Data
@NoArgsConstructor

public class TransactionParams implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2516248315446279127L;

	@Id
	@GeneratedValue
	private Long ID;

	@Column(name = "param_name", length = 50)
	private String paramName;

	@Column(name = "param_type", length = 255)
	private String paramType;

	@Column(name = "regex", length = 255)
	private String regex;

	@Column(name = "is_encrypted")
	private boolean isEncrypted;

	@Column(name = "is_required")
	private boolean isRequired;

	@Column(name = "is_escape_special_char_Exists")
	private boolean isEscapeSpecialCharExists;

}
