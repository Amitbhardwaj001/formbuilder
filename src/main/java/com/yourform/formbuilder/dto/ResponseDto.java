package com.yourform.formbuilder.dto;

import lombok.Data;
import java.util.List;

@Data
public class ResponseDto {

    private Long formId;
    private List<AnswerDto> answers;

    @Data
    public static class AnswerDto {
        private Long questionId;
        private String answerText;
    }
}