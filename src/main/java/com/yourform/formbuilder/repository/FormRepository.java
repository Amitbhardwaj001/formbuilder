package com.yourform.formbuilder.repository;

//package com.example.formbuilder.repository;

import com.yourform.formbuilder.model.Form;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FormRepository extends JpaRepository<Form, Long> {
}