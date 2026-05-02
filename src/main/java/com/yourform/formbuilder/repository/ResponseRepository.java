package com.yourform.formbuilder.repository;

import com.yourform.formbuilder.model.Response;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResponseRepository extends JpaRepository<Response, Long> {
    List<Response> findByFormId(Long formId);
    boolean existsByFormIdAndRespondentEmail(Long formId, String respondentEmail);
}
