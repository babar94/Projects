package com.gateway.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.gateway.entity.Translation;

public interface TranslationRepository extends JpaRepository<Translation, Long> {

	boolean existsByTranslationKeyAndLocale(String translationKey, String locale);
    Optional<Translation> findByTranslationKeyAndLocale(String translationKey, String locale);
    @Query("SELECT t FROM Translation t JOIN FETCH t.tags WHERE t.locale = :locale")
    List<Translation> findAllWithTagsByLocale(@Param("locale") String locale);

}
	

