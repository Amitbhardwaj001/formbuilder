package com.yourform.formbuilder.service;
import com.yourform.formbuilder.dto.QuestionResponseDto;
import com.yourform.formbuilder.dto.SubmitRequest;
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
    private final ResponseRepository responseRepo;   // ✅ added
    private final AnswerRepository answerRepo; 
    private final OptionRepository optionRepo;      // ✅ added

    public FormService(FormRepository formRepo,
                   QuestionRepository questionRepo,
                   AnswerRepository answerRepo,
                   AiService aiService,
                ResponseRepository responseRepo,
                OptionRepository optionRepo) {

        this.formRepo = formRepo;
        this.questionRepo = questionRepo;
        //this.responseRepo = responseRepo;   // ✅ added
        this.answerRepo = answerRepo;
        this.aiService = aiService; 
        this.responseRepo = responseRepo; 
        this.optionRepo = optionRepo;  // ✅ added
    }

    public Form createForm(Form form) {

    form.setShareToken(
       UUID.randomUUID()
           .toString()
           .substring(0,8)
    );

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
        dto.setRequired(q.isRequired());
        dto.setOptions(optionRepo.findByQuestionId(q.getId()));

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
public Form getFormByToken(
     String token){

 return formRepo
   .findByShareToken(token)
   .orElseThrow(
    ()->new RuntimeException(
      "Form not found"
    )
   );
}
public String submitPublicForm(
        String token,
        SubmitRequest request) {

    Form form =
            formRepo.findByShareToken(token)
                    .orElseThrow(() ->
                        new RuntimeException(
                           "Form not found"
                        )
                    );

    // ✅ Required question validation
    for (Question q :
            questionRepo.findByFormId(
                form.getId()
            )) {

        if (q.isRequired()) {

            boolean answered =
                    request.getAnswers()
                           .stream()
                           .anyMatch(a ->
                              a.getQuestionId()
                               .equals(q.getId())
                              &&
                              a.getAnswerText() != null
                              &&
                              !a.getAnswerText()
                               .isBlank()
                           );

            if (!answered) {
                throw new RuntimeException(
                   q.getText() +
                   " is required"
                );
            }
        }
    }

    // save response only after validation passes
    Response response =
            new Response();

    response.setForm(form);

    responseRepo.save(response);


    for (SubmitRequest.AnswerDto a
            : request.getAnswers()) {

        Question q =
            questionRepo.findById(
                a.getQuestionId()
            ).orElseThrow();

        Answer ans =
             new Answer();

        ans.setQuestion(q);
        ans.setResponse(response);
        ans.setAnswerText(
             a.getAnswerText()
        );

        answerRepo.save(ans);
    }

    return "Public response submitted";
}
public String addOption(
      Long questionId,
      String text){

   Question q =
      questionRepo.findById(
        questionId
      ).orElseThrow();

   Option option =
      new Option();

   option.setText(text); // because yours is text
   option.setQuestion(q);

   optionRepo.save(option);

   return "Option added";
}
public List<Option> getOptions(
        Long questionId){

    return optionRepo
           .findByQuestionId(
              questionId
           );
}
public Form cloneForm(Long formId){

 Form original =
    formRepo.findById(formId)
       .orElseThrow();

 Form copy = new Form();

 copy.setTitle(
    original.getTitle()
    + " Copy"
 );

 copy.setDescription(
    original.getDescription()
 );

 copy.setShareToken(
   UUID.randomUUID()
      .toString()
      .substring(0,8)
 );

 formRepo.save(copy);

 List<Question> questions =
      questionRepo.findByFormId(
         formId
      );

 for(Question q: questions){

   Question newQ=
      new Question();

   newQ.setText(
      q.getText()
   );

   newQ.setType(
      q.getType()
   );

   newQ.setRequired(
      q.isRequired()
   );

   newQ.setForm(copy);

   questionRepo.save(newQ);

   List<Option> options=
      optionRepo.findByQuestionId(
         q.getId()
      );

   for(Option op: options){

      Option newOp=
         new Option();

      newOp.setText(
          op.getText()
      );

      newOp.setQuestion(
         newQ
      );

      optionRepo.save(
         newOp
      );
   }

 }

 return copy;
}

public void updateOrder(
Long id,
Integer order){

 Question q=
   questionRepo.findById(id)
    .orElseThrow();

 q.setDisplayOrder(order);

 questionRepo.save(q);
}

public Form createTemplate(
      String type){

   Form form =
      new Form();

   if(type.equalsIgnoreCase(
       "feedback")){

      form.setTitle(
         "Customer Feedback"
      );

      form.setDescription(
        "Feedback form template"
      );

      form.setShareToken(
         UUID.randomUUID()
          .toString()
          .substring(0,8)
      );

      formRepo.save(form);


      Question q1 =
         new Question();

      q1.setText(
        "Are you satisfied?"
      );

      q1.setType("MCQ");

      q1.setRequired(true);

      q1.setForm(form);

      questionRepo.save(q1);


      Option o1=
          new Option();

      o1.setText("Yes");
      o1.setQuestion(q1);

      optionRepo.save(o1);


      Option o2=
         new Option();

      o2.setText("No");
      o2.setQuestion(q1);

      optionRepo.save(o2);


      Question q2=
         new Question();

      q2.setText(
        "Suggestions?"
      );

      q2.setType("TEXT");

      q2.setForm(form);

      questionRepo.save(q2);

   }

   return form;
}
public List<Form> getForms(){
    return formRepo.findAll();
}
public Question updateQuestion(
Long id,
Question updated){

 Question q=
 questionRepo.findById(id)
 .orElseThrow();

 q.setText(
  updated.getText()
 );

 q.setType(
  updated.getType()
 );

 q.setRequired(
  updated.isRequired()
 );

 return questionRepo.save(q);
}
public void deleteQuestion(
Long id){

 questionRepo.deleteById(id);

}

}