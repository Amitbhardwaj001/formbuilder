package com.yourform.formbuilder.service;

import com.yourform.formbuilder.dto.AnalyticsDto;
import com.yourform.formbuilder.dto.FilterRequest;
//import com.yourform.formbuilder.dto.FilterRequest;
import com.yourform.formbuilder.model.*;
import com.yourform.formbuilder.repository.*;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class FormService {

    private final FormRepository formRepo;
    private final QuestionRepository questionRepo;
    //private final ResponseRepository responseRepo;   // ✅ added
    private final AnswerRepository answerRepo;       // ✅ added

    public FormService(FormRepository formRepo,
                       QuestionRepository questionRepo,
                       AnswerRepository answerRepo) {

        this.formRepo = formRepo;
        this.questionRepo = questionRepo;
        //this.responseRepo = responseRepo;   // ✅ added
        this.answerRepo = answerRepo;       // ✅ added
    }

    public Form createForm(Form form) {
        return formRepo.save(form);
    }

    public Question addQuestion(Question question) {
        return questionRepo.save(question);
    }

    public List<Question> getQuestions(Long formId) {
        return questionRepo.findByFormId(formId);
    }

    public Form getForm(Long id) {
        return formRepo.findById(id).orElseThrow();
    }

    // 🔥 FILTER LOGIC
    // 🔥 ANALYTICS LOGIC (FIXED + FILTERED)
public AnalyticsDto getAnalytics(Long formId) {

    List<Answer> answers = answerRepo.findAll();

    AnalyticsDto dto = new AnalyticsDto();

    Map<Long, Map<String, Long>> data = new HashMap<>();
    Set<Long> responseIds = new HashSet<>();

    for (Answer a : answers) {

        // ✅ Filter only this form's data
        if (!a.getQuestion().getForm().getId().equals(formId)) {
            continue;
        }

        // count unique responses
        responseIds.add(a.getResponse().getId());

        Long qId = a.getQuestion().getId();
        String ans = a.getAnswerText();

        data.putIfAbsent(qId, new HashMap<>());
        Map<String, Long> countMap = data.get(qId);

        countMap.put(ans, countMap.getOrDefault(ans, 0L) + 1);
    }

    // ✅ correct total responses (not answers count)
    dto.setTotalResponses((long) responseIds.size());

    List<AnalyticsDto.QuestionAnalytics> result = new ArrayList<>();

    for (Map.Entry<Long, Map<String, Long>> entry : data.entrySet()) {

        Question q = questionRepo.findById(entry.getKey())
                .orElseThrow(() -> new RuntimeException("Question not found"));

        AnalyticsDto.QuestionAnalytics qa = new AnalyticsDto.QuestionAnalytics();
        qa.setQuestionText(q.getText());
        qa.setAnswerCount(entry.getValue());

        result.add(qa);
    }

    dto.setQuestions(result);

    return dto;
}
public List<Question> getFilteredQuestions(FilterRequest request) {

    List<Question> allQuestions = questionRepo.findByFormId(request.getFormId());
    List<Question> result = new ArrayList<>();

    for (Question q : allQuestions) {

        if (q.getConditionQuestionId() == null) {
            result.add(q);
        } else {
            String userAnswer = request.getAnswers()
                    .get(q.getConditionQuestionId());

            if (userAnswer != null &&
                userAnswer.equalsIgnoreCase(q.getConditionValue())) {

                result.add(q);
            }
        }
    }

    return result;
}

}