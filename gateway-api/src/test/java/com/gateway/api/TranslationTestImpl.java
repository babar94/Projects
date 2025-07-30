package com.gateway.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.gateway.entity.Translation;
import com.gateway.entity.TranslationTag;
import com.gateway.repository.TranslationRepository;
import com.gateway.repository.TranslationTagRepository;
import com.gateway.request.TranslationCreateRequest;
import com.gateway.request.TranslationUpdateRequest;
import com.gateway.response.ResponseMessage;
import com.gateway.response.TranslationResponseDTO;
import com.gateway.service.impl.TranslationServiceImpl;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.atLeastOnce;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class TranslationServiceImplTest {

	@Mock
	private TranslationRepository translationRepository;

	@Mock
	private TranslationTagRepository tagRepository;

	@InjectMocks
	private TranslationServiceImpl translationService;

	@Mock
	private Translation translation;

	///// Translation creation

	@Test
	void testCreateTranslation_AlreadyExists() {
		// Arrange
		TranslationCreateRequest request = new TranslationCreateRequest();
		request.setTranslationKey("signup.title");
		request.setLocale("en");

		when(translationRepository.existsByTranslationKeyAndLocale("signup.title", "en")).thenReturn(true);

		// Act
		ResponseMessage response = translationService.createTranslation(request);

		// Assert
		assertEquals("Translation already created", response.getResponseDescription());
		verify(translationRepository, never()).save(any());
	}

	@Test
	void testCreateTranslation_SuccessfulCreation() {
		// Arrange
		TranslationCreateRequest request = new TranslationCreateRequest();
		request.setTranslationKey("login.button");
		request.setLocale("en");
		request.setContent("Login");
		request.setTags(List.of("auth", "button"));

		when(translationRepository.existsByTranslationKeyAndLocale("login.button", "en")).thenReturn(false);

		Translation savedTranslation = new Translation();
		savedTranslation.setTranslationKey("login.button");
		savedTranslation.setLocale("en");
		savedTranslation.setContent("Login");

		when(translationRepository.save(any())).thenReturn(savedTranslation);

		// Act
		ResponseMessage response = translationService.createTranslation(request);

		// Assert
		assertEquals("Translation successfully created", response.getResponseDescription());
		verify(translationRepository).save(any());
	}

	@Test
	void testCreateTranslation_ExceptionHandling() {
		// Arrange
		TranslationCreateRequest request = new TranslationCreateRequest();
		request.setTranslationKey("home.header");
		request.setLocale("en");
		request.setContent("Welcome");

		when(translationRepository.existsByTranslationKeyAndLocale(any(), any()))
				.thenThrow(new RuntimeException("DB down"));

		// Act
		ResponseMessage response = translationService.createTranslation(request);

		// Assert
		assertEquals("Exception occur while creating translation", response.getResponseDescription());
	}

	////// update translation

	@Test
	void testUpdateTranslation_successfulUpdate() {
		String key = "signup.title";
		TranslationUpdateRequest request = new TranslationUpdateRequest();
		request.setLocale("en");
		request.setContent("Updated Content");
		request.setTags(List.of("auth", "signup"));

		Translation translation = new Translation();
		translation.setId(1L);
		translation.setContent("Old Content");

		when(translationRepository.findByTranslationKeyAndLocale(key, "en")).thenReturn(Optional.of(translation));

		ResponseMessage response = translationService.updateTranslation(key, request);

		assertEquals("Translation successfully updated", response.getResponseDescription());
		verify(translationRepository).save(translation);
		verify(tagRepository).deleteByTranslationId(1L);
	}

	@Test
	void testUpdateTranslation_translationNotFound() {
		String key = "invalid.key";
		TranslationUpdateRequest request = new TranslationUpdateRequest();
		request.setLocale("en");

		when(translationRepository.findByTranslationKeyAndLocale(key, "en")).thenReturn(Optional.empty());

		ResponseMessage response = translationService.updateTranslation(key, request);

		assertEquals("Translation not found", response.getResponseDescription());
		verify(translationRepository, never()).save(any());
		verify(tagRepository, never()).deleteByTranslationId(any());
	}

	@Test
	void testUpdateTranslation_exceptionOccurs() {
		String key = "signup.title";
		TranslationUpdateRequest request = new TranslationUpdateRequest();
		request.setLocale("en");

		when(translationRepository.findByTranslationKeyAndLocale(any(), any()))
				.thenThrow(new RuntimeException("Database error"));

		ResponseMessage response = translationService.updateTranslation(key, request);

		assertEquals("Exception occur while updating translation", response.getResponseDescription());
	}

	///// GetTranslationWithTags

	@Test
	void testGetTranslationsWithTags_success() {
		// Arrange
		TranslationTag tag1 = new TranslationTag();
		tag1.setTag("greeting");
		TranslationTag tag2 = new TranslationTag();
		tag2.setTag("home");

		Translation translation = new Translation();
		translation.setTranslationKey("welcome.message");
		translation.setLocale("en");
		translation.setContent("Welcome!");
		translation.setTags(Arrays.asList(tag1, tag2));

		when(translationRepository.findAllWithTagsByLocale("en")).thenReturn(Collections.singletonList(translation));

		// Act
		List<TranslationResponseDTO> result = translationService.getTranslationsWithTags("en");

		// Assert
		assertEquals(1, result.size());
		TranslationResponseDTO dto = result.get(0);
		assertEquals("welcome.message", dto.getTranslationKey());
		assertEquals("en", dto.getLocale());
		assertEquals("Welcome!", dto.getContent());
		assertTrue(dto.getTags().contains("greeting"));
		assertTrue(dto.getTags().contains("home"));
	}

	@Test
    void testGetTranslationsWithTags_emptyList() {
        when(translationRepository.findAllWithTagsByLocale("fr"))
                .thenReturn(Collections.emptyList());

        List<TranslationResponseDTO> result = translationService.getTranslationsWithTags("fr");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

	@Test
    void testGetTranslationsWithTags_exceptionThrown() {
        when(translationRepository.findAllWithTagsByLocale("en"))
                .thenThrow(new RuntimeException("DB error"));

        List<TranslationResponseDTO> result = translationService.getTranslationsWithTags("en");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

	@Test
	void getTranslationsWithTags_ShouldReturnMappedDTOs() {
		// Arrange
		TranslationTag tag1 = new TranslationTag();
		tag1.setTag("greeting");

		TranslationTag tag2 = new TranslationTag();
		tag2.setTag("intro");
		Translation translation = new Translation();
		translation.setTranslationKey("home.title");
		translation.setLocale("en");
		translation.setContent("Welcome Home");
		translation.setTags(Arrays.asList(tag1, tag2));

		when(translationRepository.findAllWithTagsByLocale("en")).thenReturn(List.of(translation));

		// Act
		List<TranslationResponseDTO> result = translationService.getTranslationsWithTags("en");

		// Assert
		assertEquals(1, result.size());
		assertEquals("home.title", result.get(0).getTranslationKey());
		assertEquals(List.of("greeting", "intro"), result.get(0).getTags());
	}

	@Test
    void getTranslationsWithTags_WhenException_ShouldReturnEmptyList() {
        when(translationRepository.findAllWithTagsByLocale("en")).thenThrow(new RuntimeException("DB Error"));

        List<TranslationResponseDTO> result = translationService.getTranslationsWithTags("en");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

	/////// Export All data

	@Test
	void getAllTranslationsAsJson_ShouldReturnCorrectMap() {
		// Arrange
		Translation t1 = new Translation();
		t1.setTranslationKey("key1");
		t1.setLocale("en");
		t1.setContent("Hello");

		Translation t2 = new Translation();
		t2.setTranslationKey("key1");
		t2.setLocale("fr");
		t2.setContent("Bonjour");

		when(translationRepository.findAll()).thenReturn(List.of(t1, t2));

		// Act
		Map<String, Map<String, String>> result = translationService.getAllTranslationsAsJson();

		// Assert
		assertEquals(1, result.size());
		assertTrue(result.containsKey("key1"));
		assertEquals("Hello", result.get("key1").get("en"));
		assertEquals("Bonjour", result.get("key1").get("fr"));
	}

	@Test
    void getAllTranslationsAsJson_WhenException_ShouldReturnEmptyMap() {
        when(translationRepository.findAll()).thenThrow(new RuntimeException("DB Error"));

        Map<String, Map<String, String>> result = translationService.getAllTranslationsAsJson();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
