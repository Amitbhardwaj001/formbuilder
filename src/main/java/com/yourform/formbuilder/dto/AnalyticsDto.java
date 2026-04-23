package com.yourform.formbuilder.dto;

import lombok.Data;
import java.util.Map;
import java.util.List;

@Data
public class AnalyticsDto {

    private Long totalResponses;
    private List<QuestionAnalytics> questions;

    @Data
    public static class QuestionAnalytics {
        private String questionText;
        private Map<String, Long> answerCount;
    }
}