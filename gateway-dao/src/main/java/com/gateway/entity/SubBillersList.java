package com.gateway.entity;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "sub_billers")
@Data
@NoArgsConstructor
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
	@Column(name = "one_bill_enable")
	private String oneBillEnable;
	@Column(name = "is_active")
	private Boolean isActive;

	@Column(name = "contact_number")
	private String contactNumber;
	// @JsonBackReference
	@ManyToOne
	@JoinColumn(name = "biller_id")
	private BillerConfiguration billerConfiguration;

}
