package com.gateway.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "nbp_pgw_credentials")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
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
	
	@Column(name="reserved_field")
	private String reserved;

}