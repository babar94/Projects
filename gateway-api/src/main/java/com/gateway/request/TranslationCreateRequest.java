package com.gateway.request;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TranslationCreateRequest {
    @NotBlank
    private String translationKey;

    @NotBlank
    private String locale;

    @NotBlank
    private String content;

    private List<String> tags;
}

