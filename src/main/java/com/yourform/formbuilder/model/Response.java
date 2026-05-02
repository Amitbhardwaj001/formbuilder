package com.yourform.formbuilder.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
public class Response {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime submittedAt = LocalDateTime.now();
    private String respondentEmail;

    @ManyToOne
    @JoinColumn(name = "form_id")
    private Form form;
}
