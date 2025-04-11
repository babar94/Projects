package com.gateway.entity;

import java.io.Serializable;
import java.sql.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name = "biller_configuration")
@NoArgsConstructor

public class BillerConfiguration implements Serializable {

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

	@Column(name = "is_active")
	private Boolean isActive;

	@Column(name = "created_date")
	private Date createdDate;

	@Column(name = "modified_date")
	private Date modifiedDate;

	@Column(name = "user_id")
	private String userId;

	@Column(name = "bill_generate")
	private String billGenerate;

	@Column(name = "custom_file_format")
	private String customFileFormat;

	@Column(name = "type")
	private String type;

}
