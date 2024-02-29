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

        long nonEmptyFieldsCount = reservedFieldsMap.entrySet().stream()
                .filter(entry -> {
                    String reservedField = entry.getKey();
                    String requestValue = getRequestValue(billPaymentRequest, reservedField);
                    return requestValue != null && !requestValue.trim().isEmpty();
                })
                .count();

        boolean validationPassed = nonEmptyFieldsCount >= 6;

        if (!validationPassed) {
            logger.error("Validation failed. At least six reserved fields must have non-empty values.");
        } else {
            logger.info("Validation passed. At least six reserved fields have non-empty values.");
        }

        return validationPassed;
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
