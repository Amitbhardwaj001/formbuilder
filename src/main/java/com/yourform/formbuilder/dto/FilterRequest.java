package com.yourform.formbuilder.dto;

import lombok.Data;
import java.util.Map;

@Data
public class FilterRequest {

    private Long formId;

    // questionId -> answer
    private Map<Long, String> answers;
}