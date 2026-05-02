package com.yourform.formbuilder.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.*;

@Entity
@Data
public class Form {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;
    private String shareToken;
    private boolean collectEmail;
    private boolean limitOneResponsePerEmail;
    private LocalDateTime closeAt;
    private Integer timeLimitMinutes;
    @JsonIgnore
    private String ownerUsername;
}
