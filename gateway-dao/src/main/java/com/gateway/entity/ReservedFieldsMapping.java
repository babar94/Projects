package com.gateway.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "api_gw_reserved_fields_mapping")
public class ReservedFieldsMapping {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "biller_id", referencedColumnName = "id")
	private BillerConfiguration billerConfiguration;

	@Column(name = "reserved_field")
	private String reservedField;

	@Column(name = "actual_field")
	private String actualField;

	@Column(name = "regex")
	private String regex;

	@Column(name = "param_type")
	private String param_type;

	@Column(name = "is_required")
	private String isRequired;

}
