package com.gateway.service.impl;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.gateway.entity.ReservedFieldsMapping;
import com.gateway.repository.ReservedFieldMappingRepository;
import com.gateway.request.billpayment.BillPaymentRequest;
import com.gateway.service.ReservedFieldsValidationService;

@Service
public class ReservedFieldsValidationServiceImpl implements ReservedFieldsValidationService {

	private static final Logger logger = LoggerFactory.getLogger(ReservedFieldsValidationServiceImpl.class);

	@Autowired
	private ReservedFieldMappingRepository reservedFieldMappingRepository;

	@Override
	public boolean validateReservedFields(BillPaymentRequest billPaymentRequest, String parentId) {

		List<ReservedFieldsMapping> reservedFieldsMappings = reservedFieldMappingRepository
				.findByBillerConfigurationBillerId(parentId);

		Map<String, String> reservedFieldsMap = reservedFieldsMappings.stream()
				.collect(Collectors.toMap(ReservedFieldsMapping::getReservedField, ReservedFieldsMapping::getRegex));

		boolean allFieldsValid = reservedFieldsMap.entrySet().stream().allMatch(entry -> {
			String reservedField = entry.getKey();
			String requestValue = getRequestValue(billPaymentRequest, reservedField);
			String regex = entry.getValue();

			if (regex != null) {
				// Check if the field matches the regex pattern
				boolean isValid = Pattern.matches(regex, requestValue);
				if (!isValid) {
					logger.error("Validation failed for field: {}", reservedField);
				}
				logger.error("Validation matches for field: {}", reservedField);
				return isValid;
			} else {
				// If regex is null, just check if the field is not null or empty
				boolean isValid = requestValue != null && !requestValue.trim().isEmpty();
				if (!isValid) {
					logger.error("Validation failed for field: {}", reservedField);
				}
				return isValid;
			}
		});

		return allFieldsValid;
	}

	// Helper method to get the value for a reserved field from BillPaymentRequest
	private String getRequestValue(BillPaymentRequest billPaymentRequest, String reservedField) {
		switch (reservedField) {
		case "reserveField1":
			return billPaymentRequest.getAdditionalInfo().getReserveField1();
		case "reserveField2":
			return billPaymentRequest.getAdditionalInfo().getReserveField2();
		case "reserveField3":
			return billPaymentRequest.getAdditionalInfo().getReserveField3();
		case "reserveField4":
			return billPaymentRequest.getAdditionalInfo().getReserveField4();
		case "reserveField5":
			return billPaymentRequest.getAdditionalInfo().getReserveField5();
		case "reserveField6":
			return billPaymentRequest.getAdditionalInfo().getReserveField6();
		case "reserveField7":
			return billPaymentRequest.getAdditionalInfo().getReserveField7();
		case "reserveField8":
			return billPaymentRequest.getAdditionalInfo().getReserveField8();
		case "reserveField9":
			return billPaymentRequest.getAdditionalInfo().getReserveField9();
		case "reserveField10":
			return billPaymentRequest.getAdditionalInfo().getReserveField10();
		default:
			return null;
		}

	}

}