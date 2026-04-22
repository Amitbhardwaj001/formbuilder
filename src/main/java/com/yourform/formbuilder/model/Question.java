package com.yourform.formbuilder.model;

//package com.example.formbuilder.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String text;
    private String type; // TEXT or MCQ

    @ManyToOne
    @JoinColumn(name = "form_id")
    private Form form;
}
