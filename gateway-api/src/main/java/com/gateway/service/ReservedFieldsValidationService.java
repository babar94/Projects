package com.gateway.service;

import com.gateway.request.billpayment.BillPaymentRequest;

public interface ReservedFieldsValidationService {

	boolean validateReservedFields(BillPaymentRequest billPaymentRequest, String parentId);
}
