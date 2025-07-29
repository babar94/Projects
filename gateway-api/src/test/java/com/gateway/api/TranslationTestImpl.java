package com.gateway.api;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.gateway.entity.Translation;
import com.gateway.repository.TranslationRepository;
import com.gateway.repository.TranslationTagRepository;
import com.gateway.request.TranslationCreateRequest;
import com.gateway.request.TranslationUpdateRequest;
import com.gateway.response.ResponseMessage;
import com.gateway.service.impl.TranslationServiceImpl;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.atLeastOnce;


import java.util.List;
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

    private TranslationUpdateRequest validRequest;

    
    @Test
    void testCreateTranslation_AlreadyExists() {
        // Arrange
        TranslationCreateRequest request = new TranslationCreateRequest();
        request.setTranslationKey("signup.title");
        request.setLocale("en");

        when(translationRepository.existsByTranslationKeyAndLocale("signup.title", "en"))
                .thenReturn(true);

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

        when(translationRepository.existsByTranslationKeyAndLocale("login.button", "en"))
                .thenReturn(false);

        Translation savedTranslation = new Translation();
        savedTranslation.setTranslationKey("login.button");
        savedTranslation.setLocale("en");
        savedTranslation.setContent("Login");

        when(translationRepository.save(any())).thenReturn(savedTranslation);

        // Act
        ResponseMessage response = translationService.createTranslation(request);

        // Assert
        assertEquals("Translation successfully created", response.getResponseDescription());
//        verify(translationRepository).save(any());
//        verify(tagRepository, atLeastOnce()).save(any());
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
    
    @Test
    void testUpdateTranslation_Success() {
        when(translationRepository.findByTranslationKeyAndLocale("key1", "en"))
                .thenReturn(Optional.of(translation));
        when(translation.getId()).thenReturn(123L);

        ResponseMessage response = translationService.updateTranslation("key1", validRequest);

//        verify(translation).setContent("Updated content");
//        verify(translationRepository).save(translation);
//        verify(tagRepository).deleteByTranslationId(123L);
//        verifyNoMoreInteractions(tagRepository); // Only delete called directly, saveTags is internal

        assertEquals("Translation successfully updated", response.getResponseDescription());
    }

    
}




