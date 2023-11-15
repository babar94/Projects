package com.gateway.entity;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "nbp_pgw_sub_billers")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class SubBillersList implements Serializable {

	private static final long serialVersionUID = -2516248315446279127L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false)
	private Long id;

	@Column(name = "sub_biller_id")
	private String subBillerId;

	@Column(name = "sub_biller_name")
	private String subBillerName;

	@Column(name = "sub_biller_address", length = 100)
	private String subBillerAddress;

	@Column(name = "settlement_account")
	private String settlementAccount;

	@ManyToOne
	@JoinColumn(name = "biller_id")
	private BillerList biller;
	
}