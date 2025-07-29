package com.gateway.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.gateway.entity.TranslationTag;

@Repository
public interface TranslationTagRepository extends JpaRepository<TranslationTag, Long> {
   
	@Modifying
	@Query("DELETE FROM TranslationTag t WHERE t.translation.id = :translationId")
	void deleteByTranslationId(@Param("translationId") Long translationId);


}
