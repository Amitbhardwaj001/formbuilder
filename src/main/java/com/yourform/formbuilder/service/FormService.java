package com.yourform.formbuilder.service;
import com.yourform.formbuilder.dto.QuestionResponseDto;

import com.yourform.formbuilder.dto.AnalyticsDto;
import com.yourform.formbuilder.dto.ChartDto;
import com.yourform.formbuilder.dto.FilterRequest;
import com.yourform.formbuilder.dto.InsightDto;
//import com.yourform.formbuilder.dto.QuestionResponseDto;
//import com.yourform.formbuilder.dto.FilterRequest;
import com.yourform.formbuilder.model.*;
import com.yourform.formbuilder.repository.*;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class FormService {

    private final FormRepository formRepo;
    private final QuestionRepository questionRepo;
    private final AiService aiService;
    //private final ResponseRepository responseRepo;   // ✅ added
    private final AnswerRepository answerRepo;       // ✅ added

    public FormService(FormRepository formRepo,
                   QuestionRepository questionRepo,
                   AnswerRepository answerRepo,
                   AiService aiService) {

        this.formRepo = formRepo;
        this.questionRepo = questionRepo;
        //this.responseRepo = responseRepo;   // ✅ added
        this.answerRepo = answerRepo;
        this.aiService = aiService;       // ✅ added
    }

    public Form createForm(Form form) {
        return formRepo.save(form);
    }

    public Question addQuestion(
        Question question) {

    if(questionRepo.existsByText(
            question.getText())) {

        throw new RuntimeException(
            "Question already exists");
    }

    return questionRepo.save(question);
}

    public List<QuestionResponseDto> getQuestions(Long formId) {

    List<Question> questions =
            questionRepo.findByFormId(formId);

    List<QuestionResponseDto> result =
            new ArrayList<>();

    for (Question q : questions) {

        QuestionResponseDto dto =
                new QuestionResponseDto();

        dto.setId(q.getId());
        dto.setText(q.getText());
        dto.setType(q.getType());

        result.add(dto);
    }

    return result;
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
public InsightDto generateInsights(Long formId) {

    AnalyticsDto analytics = getAnalytics(formId);

    List<String> insights = new ArrayList<>();

    for (AnalyticsDto.QuestionAnalytics q : analytics.getQuestions()) {

        Map<String, Long> answers = q.getAnswerCount();

        long total = 0;
        String topAnswer = null;
        long max = 0;

        long positive = 0;
        long negative = 0;

        for (Map.Entry<String, Long> entry : answers.entrySet()) {

            total += entry.getValue();

            if (entry.getValue() > max) {
                max = entry.getValue();
                topAnswer = entry.getKey();
            }

            String key = entry.getKey().toLowerCase();
            long count = entry.getValue();

            // ✅ sentiment classification
            if (key.equals("yes") || key.equals("good") || key.equals("excellent")) {
                positive += count;
            } else if (key.equals("no") || key.equals("bad") || key.equals("poor")) {
                negative += count;
            }
        }

        if (topAnswer != null && total > 0) {

            double percentage = (max * 100.0) / total;

            String insight;

            if (percentage > 70) {
                insight = q.getQuestionText() + " → Strong trend: " + topAnswer + " (" + (int) percentage + "%)";
            } else if (percentage > 40) {
                insight = q.getQuestionText() + " → Moderate trend: " + topAnswer;
            } else {
                insight = q.getQuestionText() + " → Responses are diverse";
            }

            // ✅ improved sentiment logic
            if (negative > positive) {
                insight += " ⚠️ Overall negative feedback";
            } else if (positive > negative) {
                insight += " 😊 Overall positive feedback";
            } else {
                insight += " 😐 Mixed feedback";
            }

            insights.add(insight);
        }
    }

    InsightDto dto = new InsightDto();
    dto.setInsights(insights);

    return dto;

}

public String generateAiSummary(Long formId) {

    AnalyticsDto analytics = getAnalytics(formId);

    String data = analytics.toString();

    return aiService.generateSummary(data);
}

    public String generateAIQuestions(String topic) {
    String prompt = "Generate 5 short survey questions for: " + topic;
    return aiService.generateSummary(prompt);
}
public String exportCsv(Long formId){

    List<Answer> answers =
         answerRepo.findAll();

    StringBuilder csv =
         new StringBuilder();

    csv.append(
      "Question,Answer\n"
    );

    for(Answer a: answers){

      if(a.getQuestion()
         .getForm()
         .getId()
         .equals(formId)){

        csv.append(
          a.getQuestion().getText()
        );

        csv.append(",");

        csv.append(
          a.getAnswerText()
        );

        csv.append("\n");
      }
    }

   return csv.toString();
}
public ChartDto getChartData(
      Long formId){

 Map<String,Long> counts=
      new HashMap<>();

 for(Answer a :
      answerRepo.findAll()){

   if(a.getQuestion()
      .getForm()
      .getId()
      .equals(formId)){

      String ans=
        a.getAnswerText();

      counts.put(
       ans,
       counts.getOrDefault(
          ans,0L
       )+1
      );
   }
 }

 ChartDto dto=
     new ChartDto();

 dto.setLabels(
    new ArrayList<>(
      counts.keySet()
    )
 );

 dto.setValues(
    new ArrayList<>(
      counts.values()
    )
 );

 return dto;
}

}