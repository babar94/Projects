package com.gateway.entity;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "api_gw_province_transactions")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class ProvinceTransaction implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2516248315446279127L;

	@Id
	@GeneratedValue
	private int ID;

	@Column(name = "province_code", length = 50)
	private String provinceCode;

	@Column(name = "province", length = 50)
	private String province;

	@Column(name = "fed_tax_percent")
	private double fedTaxPercent;

	@Column(name = "transaction_fees")
	private double transactionFees;

	@Column(name = "late_fees")
	private double lateFees;

}
