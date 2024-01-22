package com.gateway.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "api_gw_credentials")
@Data
@NoArgsConstructor

public class Credential {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@Column(name = "username")
	private String username;

	@Column(name = "password")
	private String password;

	@Column(name = "channel_name")
	private String channelName;

	@Column(name = "is_enable")
	private boolean isEnable;

	@Column(name = "consumer_number")
	private String consumerNumber;

	@Column(name = "bank_mnemonic")
	private String bankMnemonic;

	@Column(name = "reserved_field")
	private String reserved;

	@Column(name = "remaining_count")
	private Integer remainingCount;

	@Column(name = "description")
	private String description;

}