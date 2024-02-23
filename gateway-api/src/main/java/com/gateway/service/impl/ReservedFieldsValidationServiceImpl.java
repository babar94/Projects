package com.gateway.service.impl;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.gateway.entity.ReservedFieldsMapping;
import com.gateway.repository.ReservedFieldMappingRepository;
import com.gateway.request.billpayment.BillPaymentRequest;
import com.gateway.service.ReservedFieldsValidationService;

@Service
public class ReservedFieldsValidationServiceImpl implements ReservedFieldsValidationService {

	@Autowired
	private ReservedFieldMappingRepository reservedFieldMappingRepository;

	@Override
	public boolean validateReservedFields(BillPaymentRequest billPaymentRequest, String parentId) {

//		List<ReservedFieldsMapping> reservedFieldsMappings = reservedFieldMappingRepository
//				.findByBillerConfigurationBillerId(parentId);
//
//		Map<String, String> reservedFieldsMap = reservedFieldsMappings.stream().collect(
//				Collectors.toMap(ReservedFieldsMapping::getReservedField, ReservedFieldsMapping::getActualField));
//
//		// Validate each reserved field in the request against the actual fields in the
//		// database
//		return validateReservedField(billPaymentRequest.getAdditionalInfo().getReserveField1(),
//				reservedFieldsMap.get("reserveField1"))
//				&& validateReservedField(billPaymentRequest.getAdditionalInfo().getReserveField2(),
//						reservedFieldsMap.get("reserveField2"))
//				&& validateReservedField(billPaymentRequest.getAdditionalInfo().getReserveField3(),
//						reservedFieldsMap.get("reserveField3"))
//				&& validateReservedField(billPaymentRequest.getAdditionalInfo().getReserveField4(),
//						reservedFieldsMap.get("reserveField4"))
//				&& validateReservedField(billPaymentRequest.getAdditionalInfo().getReserveField5(),
//						reservedFieldsMap.get("reserveField5"))
//				&& validateReservedField(billPaymentRequest.getAdditionalInfo().getReserveField6(),
//						reservedFieldsMap.get("reserveField6"));
		return true;
	}

	private boolean validateReservedField(String requestField, String actualField) {
		return requestField.equals(actualField);
	}

}
