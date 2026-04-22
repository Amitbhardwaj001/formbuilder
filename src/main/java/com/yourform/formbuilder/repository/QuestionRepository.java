package com.yourform.formbuilder.repository;

//package com.example.formbuilder.repository;

import com.yourform.formbuilder.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByFormId(Long formId);
}