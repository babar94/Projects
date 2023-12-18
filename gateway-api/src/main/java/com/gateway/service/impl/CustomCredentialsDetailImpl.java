package com.gateway.service.impl;

import java.util.Collection;
import java.util.Collections;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.gateway.entity.Credential;

public class CustomCredentialsDetailImpl implements UserDetails{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Credential credential;

	public CustomCredentialsDetailImpl(Credential credential) {
		this.credential = credential;

	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		// TODO Auto-generated method stub
		return Collections.emptyList();
	}

	@Override
	public String getPassword() {

		return credential.getPassword();
	}

	@Override
	public String getUsername() {
		// TODO Auto-generated method stub
		return credential.getUsername();
	}

	@Override
	public boolean isAccountNonExpired() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean isEnabled() {
		// TODO Auto-generated method stub
		return true;
	}

}
