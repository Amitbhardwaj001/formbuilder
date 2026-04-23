package com.yourform.formbuilder.repository;

import com.yourform.formbuilder.model.Answer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnswerRepository extends JpaRepository<Answer, Long> {}