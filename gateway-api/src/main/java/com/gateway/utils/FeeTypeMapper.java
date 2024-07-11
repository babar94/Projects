package com.gateway.utils;

import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gateway.entity.FeeType;
import com.gateway.model.mpay.response.billinquiry.dls.FeeTypeListWrapper;

@Component
public class FeeTypeMapper {

	private final ModelMapper modelMapper;

	@Autowired
	public FeeTypeMapper(ModelMapper modelMapper) {
		this.modelMapper = modelMapper;
	}

	public FeeType[] mapFeeTypeListToArray(List<FeeTypeListWrapper> feeTypesList) {
		return feeTypesList.stream().map(this::mapToFeeType).toArray(FeeType[]::new);
	}

	public FeeType mapToFeeType(FeeTypeListWrapper feeTypeListWrapper) {
		return modelMapper.map(feeTypeListWrapper, FeeType.class);
	}
}
