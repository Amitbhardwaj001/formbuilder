package com.yourform.formbuilder.model;

//package com.example.formbuilder.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Data
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Question text cannot be empty")
    private String text;

    @NotNull(message = "Question type is required")
    private String type;// TEXT or MCQ
    
    private Long conditionQuestionId;
    private String conditionValue;
    private boolean required;
    private Integer displayOrder;

    @ManyToOne
    @JoinColumn(name = "form_id")
    private Form form;
}


