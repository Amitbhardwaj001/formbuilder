package com.yourform.formbuilder.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
public class Form {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;
}