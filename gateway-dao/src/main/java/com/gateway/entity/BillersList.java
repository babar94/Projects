package com.gateway.entity;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "nbp_pgw_biller")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class BillersList implements Serializable {

	private static final long serialVersionUID = -2516248315446279127L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false)
	private Long id;

	@Column(name = "biller_id")
	private String billerId;

	@Column(name = "biller_name")
	private String billerName;

	@Column(name = "biller_address", length = 100)
	private String billerAddress;

	@Column(name = "settlement_account")
	private String settlementAccount;

}