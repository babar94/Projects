package com.gateway.entity;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "nbp_pgw_api_gw_transaction_params")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
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

	
}
