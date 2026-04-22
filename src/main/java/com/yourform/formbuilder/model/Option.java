package com.yourform.formbuilder.model;

//package com.example.formbuilder.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
public class Option {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String text;

    @ManyToOne
    @JoinColumn(name = "question_id")
    private Question question;
}