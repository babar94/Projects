package com.gateway.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gateway.api.ApiController;
import com.gateway.request.TranslationCreateRequest;
import com.gateway.request.TranslationUpdateRequest;
import com.gateway.response.ResponseMessage;
import com.gateway.response.TranslationResponseDTO;
import com.gateway.service.TranslationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(path = ApiController.API_PATH)
@RequiredArgsConstructor
public class TranslationController {

	private final TranslationService translationService;

	@PostMapping("/create")
	public ResponseMessage createTranslation(@Valid @RequestBody TranslationCreateRequest request) {
		ResponseMessage responseMessage = translationService.createTranslation(request);
		return responseMessage;
	}

	@PutMapping("/update/{translationKey}")
	public ResponseMessage updateTranslation(@PathVariable String translationKey,
			@Valid @RequestBody TranslationUpdateRequest request) {
		ResponseMessage responseMessage = translationService.updateTranslation(translationKey, request);
		return responseMessage;
	}

	@GetMapping("/api/translations")
	public ResponseEntity<List<TranslationResponseDTO>> getTranslations(@RequestParam String locale) {
		return ResponseEntity.ok(translationService.getTranslationsWithTags(locale));
	}

	@GetMapping("/export")
	public ResponseEntity<Map<String, Map<String, String>>> exportTranslations() {
		Map<String, Map<String, String>> response = translationService.getAllTranslationsAsJson();
		return ResponseEntity.ok(response);
	}
	
	

}
