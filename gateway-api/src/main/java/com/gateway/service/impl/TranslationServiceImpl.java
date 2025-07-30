package com.gateway.service.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gateway.entity.Translation;
import com.gateway.entity.TranslationTag;
import com.gateway.repository.TranslationRepository;
import com.gateway.repository.TranslationTagRepository;
import com.gateway.request.TranslationCreateRequest;
import com.gateway.request.TranslationUpdateRequest;
import com.gateway.response.ResponseMessage;
import com.gateway.response.TranslationResponseDTO;
import com.gateway.service.TranslationService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TranslationServiceImpl implements TranslationService {
	private static final Logger LOG = LoggerFactory.getLogger(TranslationServiceImpl.class);

	private final TranslationRepository translationRepository;
	private final TranslationTagRepository tagRepository;

	@Override
	public ResponseMessage createTranslation(TranslationCreateRequest request) {

		ResponseMessage responseMessage = new ResponseMessage();

		try {
			if (translationRepository.existsByTranslationKeyAndLocale(request.getTranslationKey(),
					request.getLocale())) {
				responseMessage.setResponseDescription("Translation already created");
				return responseMessage;
			}

			Translation translation = new Translation();
			translation.setTranslationKey(request.getTranslationKey());
			translation.setLocale(request.getLocale());
			translation.setContent(request.getContent());
			translation = translationRepository.save(translation);

			saveTags(request.getTags(), translation);

			responseMessage.setResponseDescription("Translation successfully created");

		} catch (Exception e) {
			LOG.info("Exception occur in translation created" + e.getMessage());
			responseMessage.setResponseDescription("Exception occur while creating translation");

		}

		return responseMessage;
	}

	@Transactional
	@Override
	public ResponseMessage updateTranslation(String translationKey, TranslationUpdateRequest request) {
		ResponseMessage responseMessage = new ResponseMessage();

		try {
			Optional<Translation> translation = translationRepository.findByTranslationKeyAndLocale(translationKey,
					request.getLocale());
			if (translation.isPresent()) {

				translation.get().setContent(request.getContent());
				translationRepository.save(translation.get());
				tagRepository.deleteByTranslationId(translation.get().getId());
				saveTags(request.getTags(), translation.get());
				responseMessage.setResponseDescription("Translation successfully updated");
			}

			else {

				responseMessage.setResponseDescription("Translation not found");
				return responseMessage;

			}

		} catch (Exception e) {
			LOG.info("Exception occur in translation updated" + e.getMessage());
			responseMessage.setResponseDescription("Exception occur while updating translation");

		}
		return responseMessage;
	}

	@Override
	public List<TranslationResponseDTO> getTranslationsWithTags(String locale) {

		try {

			List<Translation> translations = translationRepository.findAllWithTagsByLocale(locale);
			return translations.stream().map(t -> {
				List<String> tags = t.getTags().stream().map(TranslationTag::getTag).collect(Collectors.toList());
				return new TranslationResponseDTO(t.getTranslationKey(), t.getLocale(), t.getContent(), tags);
			}).collect(Collectors.toList());

		}

		catch (Exception e) {
			LOG.info("Exception occur in GetTranslationsWithTags updated" + e.getMessage());
			return Collections.emptyList();

		}
	}

	@Override
	public Map<String, Map<String, String>> getAllTranslationsAsJson() {

		try {

			List<Translation> translations = translationRepository.findAll();

			Map<String, Map<String, String>> translationMap = new HashMap<>();

			for (Translation translation : translations) {
				String key = translation.getTranslationKey();

				translationMap.computeIfAbsent(key, k -> new HashMap<>()).put(translation.getLocale(),
						translation.getContent());
			}

			return translationMap;

		}

		catch (Exception e) {
			LOG.info("Exception occur in GetAllTranslationsAsJson updated" + e.getMessage());
			return Collections.emptyMap();

		}

	}

	private void saveTags(List<String> tags, Translation translation) {
		if (tags != null) {
			List<TranslationTag> tagEntities = tags.stream().map(tag -> new TranslationTag(translation, tag))
					.collect(Collectors.toList());
			tagRepository.saveAll(tagEntities);
		}
	}
}
