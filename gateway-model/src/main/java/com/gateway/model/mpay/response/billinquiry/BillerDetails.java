package com.gateway.model.mpay.response.billinquiry;

public interface BillerDetails {

	String getItemDetail();

	String getFees();

	default double getFeesAsDouble() {
		String fees = getFees();
		return fees != null ? Double.parseDouble(fees) : 0.0;
	}
}
