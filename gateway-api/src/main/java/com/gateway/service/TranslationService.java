package com.gateway.service;

import java.util.List;
import java.util.Map;

import com.gateway.request.TranslationCreateRequest;
import com.gateway.request.TranslationUpdateRequest;
import com.gateway.response.ResponseMessage;
import com.gateway.response.TranslationResponseDTO;

public interface TranslationService {

	public ResponseMessage createTranslation(TranslationCreateRequest request);
    public ResponseMessage updateTranslation(String translationKey, TranslationUpdateRequest request);
    public Map<String, Map<String, String>> getAllTranslationsAsJson();
    public List<TranslationResponseDTO> getTranslationsWithTags(String locale);

}
