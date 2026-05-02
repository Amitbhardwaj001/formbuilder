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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
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

    public Form createForm(Form form, String ownerUsername) {

    form.setShareToken(
       UUID.randomUUID()
           .toString()
           .substring(0,8)
    );
    form.setOwnerUsername(ownerUsername);

    return formRepo.save(form);
}

public void assertFormOwner(Long formId, String ownerUsername) {
   Form form = formRepo.findById(formId).orElseThrow();

   if (form.getOwnerUsername() == null || !form.getOwnerUsername().equals(ownerUsername)) {
      throw new ResponseStatusException(
         HttpStatus.FORBIDDEN,
         "You do not have access to this form"
      );
   }
}

public void assertQuestionOwner(Long questionId, String ownerUsername) {
   Question question = questionRepo.findById(questionId).orElseThrow();
   assertFormOwner(question.getForm().getId(), ownerUsername);
}

public void assertOptionOwner(Long optionId, String ownerUsername) {
   Option option = optionRepo.findById(optionId).orElseThrow();
   assertFormOwner(option.getQuestion().getForm().getId(), ownerUsername);
}

public Form updateForm(
      Long id,
      Form updated){

   Form form =
      formRepo.findById(id)
        .orElseThrow();

   form.setTitle(
      updated.getTitle()
   );

   form.setDescription(
      updated.getDescription()
   );

   form.setCollectEmail(updated.isCollectEmail());
   form.setLimitOneResponsePerEmail(updated.isLimitOneResponsePerEmail());
   form.setCloseAt(updated.getCloseAt());
   form.setTimeLimitMinutes(updated.getTimeLimitMinutes());

   return formRepo.save(form);
}

@Transactional
public void deleteForm(Long id){
   List<Response> responses =
      responseRepo.findByFormId(id);

   for(Response response: responses){
      answerRepo.deleteByResponseId(
         response.getId()
      );
   }

   responseRepo.deleteAll(
      responses
   );

   List<Question> questions =
      questionRepo.findByFormId(id);

   for(Question question: questions){
      answerRepo.deleteByQuestionId(
         question.getId()
      );
      optionRepo.deleteByQuestionId(
         question.getId()
      );
   }

   questionRepo.deleteAll(
      questions
   );

   formRepo.deleteById(id);
}

    public Question addQuestion(
        Question question) {

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
        dto.setConditionQuestionId(
            q.getConditionQuestionId()
        );
        dto.setConditionValue(
            q.getConditionValue()
        );
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
      "Respondent Email,Submitted At,Question,Answer\n"
    );

    for(Answer a: answers){

      if(a.getQuestion()
         .getForm()
         .getId()
         .equals(formId)){

        csv.append(a.getResponse().getRespondentEmail() == null ? "" : a.getResponse().getRespondentEmail());
        csv.append(",");
        csv.append(a.getResponse().getSubmittedAt());
        csv.append(",");
        csv.append(a.getQuestion().getText());
        csv.append(",");
        csv.append(a.getAnswerText());
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

    if (form.getCloseAt() != null && LocalDateTime.now().isAfter(form.getCloseAt())) {
        throw new ResponseStatusException(HttpStatus.GONE, "This form is closed");
    }

    String respondentEmail = request.getRespondentEmail();

    if ((form.isCollectEmail() || form.isLimitOneResponsePerEmail())
            && (respondentEmail == null || respondentEmail.isBlank())) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
    }

    if (form.isLimitOneResponsePerEmail()
            && responseRepo.existsByFormIdAndRespondentEmail(form.getId(), respondentEmail)) {
        throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "This email has already submitted a response"
        );
    }

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
    response.setRespondentEmail(respondentEmail);

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
public Option addOption(
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

   return option;
}
public List<Option> getOptions(
        Long questionId){

    return optionRepo
           .findByQuestionId(
              questionId
           );
}

public Option updateOption(
      Long optionId,
      Option updated){

   Option option =
      optionRepo.findById(optionId)
        .orElseThrow();

   option.setText(
      updated.getText()
   );

   return optionRepo.save(option);
}

public Form cloneForm(Long formId, String ownerUsername){

 Form original =
    formRepo.findById(formId)
       .orElseThrow();
 assertFormOwner(formId, ownerUsername);

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
 copy.setOwnerUsername(ownerUsername);

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
      String type,
      String ownerUsername){

   String templateType =
      type == null
      ? "feedback"
      : type.toLowerCase();

   Form form =
      new Form();

   form.setShareToken(
      UUID.randomUUID()
         .toString()
         .substring(0,8)
   );
   form.setOwnerUsername(ownerUsername);

   switch(templateType){
      case "job_application":
         form.setTitle("Job Application");
         form.setDescription("Collect candidate details, resume uploads, and role preferences.");
         formRepo.save(form);
         addTemplateQuestion(form, "Full name", "TEXT", true);
         addTemplateQuestion(form, "Email address", "TEXT", true);
         addTemplateQuestion(form, "Phone number", "TEXT", true);
         addTemplateQuestion(form, "Date of birth", "DATE", false);
         addTemplateQuestion(form, "Position applied for", "DROPDOWN", true, "Frontend Developer", "Backend Developer", "Designer", "Marketing", "Other");
         addTemplateQuestion(form, "Upload your resume", "FILE", true);
         addTemplateQuestion(form, "Why should we hire you?", "TEXT", false);
         break;

      case "event_registration":
         form.setTitle("Event Registration");
         form.setDescription("Register attendees and capture session preferences.");
         formRepo.save(form);
         addTemplateQuestion(form, "Full name", "TEXT", true);
         addTemplateQuestion(form, "Email address", "TEXT", true);
         addTemplateQuestion(form, "Ticket type", "DROPDOWN", true, "General", "VIP", "Student", "Speaker");
         addTemplateQuestion(form, "Meal preference", "DROPDOWN", false, "Vegetarian", "Non-vegetarian", "Vegan", "No meal");
         addTemplateQuestion(form, "Will you attend in person?", "MCQ", true, "Yes", "No");
         addTemplateQuestion(form, "Any accessibility needs?", "TEXT", false);
         break;

      case "contact":
         form.setTitle("Contact Form");
         form.setDescription("Let visitors send enquiries or messages.");
         formRepo.save(form);
         addTemplateQuestion(form, "Name", "TEXT", true);
         addTemplateQuestion(form, "Email", "TEXT", true);
         addTemplateQuestion(form, "Subject", "TEXT", true);
         addTemplateQuestion(form, "Reason for contact", "DROPDOWN", false, "Sales", "Support", "Partnership", "General");
         addTemplateQuestion(form, "Message", "TEXT", true);
         break;

      case "student_admission":
         form.setTitle("Student Admission");
         form.setDescription("Collect student and guardian details for admissions.");
         formRepo.save(form);
         addTemplateQuestion(form, "Student full name", "TEXT", true);
         addTemplateQuestion(form, "Date of birth", "DATE", true);
         addTemplateQuestion(form, "Class applying for", "DROPDOWN", true, "Grade 1", "Grade 2", "Grade 3", "Grade 4", "Grade 5", "Other");
         addTemplateQuestion(form, "Guardian name", "TEXT", true);
         addTemplateQuestion(form, "Guardian phone number", "TEXT", true);
         addTemplateQuestion(form, "Previous school", "TEXT", false);
         break;

      case "appointment":
         form.setTitle("Appointment Request");
         form.setDescription("Let people request a consultation or meeting slot.");
         formRepo.save(form);
         addTemplateQuestion(form, "Full name", "TEXT", true);
         addTemplateQuestion(form, "Email or phone", "TEXT", true);
         addTemplateQuestion(form, "Preferred date", "DATE", true);
         addTemplateQuestion(form, "Appointment type", "DROPDOWN", true, "Consultation", "Follow-up", "Demo", "Support");
         addTemplateQuestion(form, "Preferred time", "TEXT", false);
         addTemplateQuestion(form, "Notes", "TEXT", false);
         break;

      case "support_ticket":
         form.setTitle("Support Ticket");
         form.setDescription("Track customer issues and support requests.");
         formRepo.save(form);
         addTemplateQuestion(form, "Name", "TEXT", true);
         addTemplateQuestion(form, "Email", "TEXT", true);
         addTemplateQuestion(form, "Issue category", "DROPDOWN", true, "Bug", "Billing", "Account", "Feature request", "Other");
         addTemplateQuestion(form, "Priority", "DROPDOWN", true, "Low", "Medium", "High", "Urgent");
         addTemplateQuestion(form, "Describe the issue", "TEXT", true);
         addTemplateQuestion(form, "Attach screenshot or file", "FILE", false);
         break;

      case "product_order":
         form.setTitle("Product Order");
         form.setDescription("Collect simple product order requests.");
         formRepo.save(form);
         addTemplateQuestion(form, "Customer name", "TEXT", true);
         addTemplateQuestion(form, "Phone number", "TEXT", true);
         addTemplateQuestion(form, "Product", "TEXT", true);
         addTemplateQuestion(form, "Quantity", "TEXT", true);
         addTemplateQuestion(form, "Delivery address", "TEXT", true);
         addTemplateQuestion(form, "Payment preference", "DROPDOWN", false, "Cash", "UPI", "Card", "Bank transfer");
         break;

      case "employee_onboarding":
         form.setTitle("Employee Onboarding");
         form.setDescription("Gather employee details and onboarding documents.");
         formRepo.save(form);
         addTemplateQuestion(form, "Employee full name", "TEXT", true);
         addTemplateQuestion(form, "Date of joining", "DATE", true);
         addTemplateQuestion(form, "Department", "DROPDOWN", true, "Engineering", "Sales", "HR", "Finance", "Operations");
         addTemplateQuestion(form, "Emergency contact", "TEXT", true);
         addTemplateQuestion(form, "Upload ID proof", "FILE", true);
         addTemplateQuestion(form, "Laptop required?", "MCQ", false, "Yes", "No");
         break;

      case "leave_request":
         form.setTitle("Leave Request");
         form.setDescription("Let employees request and explain leave.");
         formRepo.save(form);
         addTemplateQuestion(form, "Employee name", "TEXT", true);
         addTemplateQuestion(form, "Leave type", "DROPDOWN", true, "Casual", "Sick", "Earned", "Work from home", "Other");
         addTemplateQuestion(form, "Start date", "DATE", true);
         addTemplateQuestion(form, "End date", "DATE", true);
         addTemplateQuestion(form, "Reason", "TEXT", true);
         addTemplateQuestion(form, "Manager name", "TEXT", false);
         break;

      case "customer_survey":
         form.setTitle("Customer Survey");
         form.setDescription("Understand customer satisfaction and preferences.");
         formRepo.save(form);
         addTemplateQuestion(form, "How satisfied are you?", "DROPDOWN", true, "Very satisfied", "Satisfied", "Neutral", "Unsatisfied");
         addTemplateQuestion(form, "How did you hear about us?", "DROPDOWN", false, "Search", "Social media", "Friend", "Advertisement", "Other");
         addTemplateQuestion(form, "What did you like most?", "TEXT", false);
         addTemplateQuestion(form, "What can we improve?", "TEXT", false);
         addTemplateQuestion(form, "May we contact you?", "MCQ", false, "Yes", "No");
         break;

      case "newsletter":
         form.setTitle("Newsletter Signup");
         form.setDescription("Collect newsletter subscriptions and topic interests.");
         formRepo.save(form);
         addTemplateQuestion(form, "Name", "TEXT", false);
         addTemplateQuestion(form, "Email address", "TEXT", true);
         addTemplateQuestion(form, "Topics you like", "CHECKBOX", false, "Product updates", "Tutorials", "Events", "Offers");
         addTemplateQuestion(form, "Preferred frequency", "DROPDOWN", false, "Weekly", "Monthly", "Only important updates");
         break;

      case "volunteer":
         form.setTitle("Volunteer Registration");
         form.setDescription("Register volunteers and understand availability.");
         formRepo.save(form);
         addTemplateQuestion(form, "Full name", "TEXT", true);
         addTemplateQuestion(form, "Email or phone", "TEXT", true);
         addTemplateQuestion(form, "Available days", "CHECKBOX", true, "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Weekend");
         addTemplateQuestion(form, "Area of interest", "DROPDOWN", false, "Teaching", "Operations", "Events", "Fundraising", "Other");
         addTemplateQuestion(form, "Previous experience", "TEXT", false);
         break;

      case "rental_application":
         form.setTitle("Rental Application");
         form.setDescription("Collect tenant details for property rentals.");
         formRepo.save(form);
         addTemplateQuestion(form, "Applicant name", "TEXT", true);
         addTemplateQuestion(form, "Phone number", "TEXT", true);
         addTemplateQuestion(form, "Move-in date", "DATE", true);
         addTemplateQuestion(form, "Employment status", "DROPDOWN", true, "Employed", "Self-employed", "Student", "Other");
         addTemplateQuestion(form, "Monthly income range", "DROPDOWN", false, "Below 25k", "25k-50k", "50k-1L", "Above 1L");
         addTemplateQuestion(form, "Upload proof document", "FILE", false);
         break;

      case "project_request":
         form.setTitle("Project Request");
         form.setDescription("Capture project requirements from clients or teams.");
         formRepo.save(form);
         addTemplateQuestion(form, "Requester name", "TEXT", true);
         addTemplateQuestion(form, "Project title", "TEXT", true);
         addTemplateQuestion(form, "Project type", "DROPDOWN", true, "Website", "Mobile app", "Branding", "Automation", "Other");
         addTemplateQuestion(form, "Expected deadline", "DATE", false);
         addTemplateQuestion(form, "Budget range", "DROPDOWN", false, "Small", "Medium", "Large", "Not sure");
         addTemplateQuestion(form, "Describe the project", "TEXT", true);
         break;

      default:
         form.setTitle("Customer Feedback");
         form.setDescription("Collect satisfaction, comments, and improvement ideas.");
         formRepo.save(form);
         addTemplateQuestion(form, "Are you satisfied?", "MCQ", true, "Yes", "No");
         addTemplateQuestion(form, "Rate your experience", "DROPDOWN", true, "Excellent", "Good", "Average", "Poor");
         addTemplateQuestion(form, "What did you like?", "TEXT", false);
         addTemplateQuestion(form, "Suggestions?", "TEXT", false);
         break;
   }

   ensureMinimumTemplateQuestions(form, templateType);

   return form;
}

private void ensureMinimumTemplateQuestions(Form form, String templateType) {
   int count = questionRepo.findByFormId(form.getId()).size();
   List<String[]> extras = getTemplateExtraQuestions(templateType);

   for (String[] extra : extras) {
      if (count >= 10) {
         return;
      }

      addTemplateQuestion(form, extra[0], extra[1], Boolean.parseBoolean(extra[2]),
            Arrays.copyOfRange(extra, 3, extra.length));
      count++;
   }
}

private List<String[]> getTemplateExtraQuestions(String templateType) {
   switch (templateType) {
      case "job_application":
         return List.of(
            new String[]{"Current city", "TEXT", "false"},
            new String[]{"Highest qualification", "DROPDOWN", "true", "High school", "Bachelor's", "Master's", "Diploma", "Other"},
            new String[]{"Years of experience", "DROPDOWN", "true", "0-1", "2-4", "5-8", "9+"},
            new String[]{"Notice period", "DROPDOWN", "false", "Immediate", "15 days", "30 days", "60+ days"},
            new String[]{"LinkedIn or portfolio URL", "TEXT", "false"}
         );
      case "event_registration":
         return List.of(
            new String[]{"Organization", "TEXT", "false"},
            new String[]{"Job title", "TEXT", "false"},
            new String[]{"Sessions interested in", "CHECKBOX", "false", "Keynote", "Workshop", "Networking", "Panel"},
            new String[]{"Need parking?", "MCQ", "false", "Yes", "No"},
            new String[]{"Emergency contact", "TEXT", "false"}
         );
      case "contact":
         return List.of(
            new String[]{"Phone number", "TEXT", "false"},
            new String[]{"Company", "TEXT", "false"},
            new String[]{"Preferred reply method", "DROPDOWN", "false", "Email", "Phone", "WhatsApp"},
            new String[]{"How urgent is this?", "DROPDOWN", "false", "Low", "Medium", "High"},
            new String[]{"Best time to contact", "TEXT", "false"}
         );
      case "student_admission":
         return List.of(
            new String[]{"Student email", "TEXT", "false"},
            new String[]{"Address", "TEXT", "true"},
            new String[]{"Gender", "DROPDOWN", "false", "Female", "Male", "Prefer not to say"},
            new String[]{"Upload previous report card", "FILE", "false"},
            new String[]{"Medical notes", "TEXT", "false"}
         );
      case "appointment":
         return List.of(
            new String[]{"Preferred mode", "DROPDOWN", "true", "In person", "Phone", "Video call"},
            new String[]{"Alternate date", "DATE", "false"},
            new String[]{"Location", "TEXT", "false"},
            new String[]{"Have you visited before?", "MCQ", "false", "Yes", "No"},
            new String[]{"Attach related document", "FILE", "false"}
         );
      case "support_ticket":
         return List.of(
            new String[]{"Account ID", "TEXT", "false"},
            new String[]{"Device or browser", "TEXT", "false"},
            new String[]{"When did it start?", "DATE", "false"},
            new String[]{"Can we contact you?", "MCQ", "false", "Yes", "No"},
            new String[]{"Steps to reproduce", "TEXT", "false"}
         );
      case "product_order":
         return List.of(
            new String[]{"Email address", "TEXT", "true"},
            new String[]{"Preferred delivery date", "DATE", "false"},
            new String[]{"Color or variant", "TEXT", "false"},
            new String[]{"Need gift wrapping?", "MCQ", "false", "Yes", "No"},
            new String[]{"Order notes", "TEXT", "false"}
         );
      case "employee_onboarding":
         return List.of(
            new String[]{"Personal email", "TEXT", "true"},
            new String[]{"Date of birth", "DATE", "false"},
            new String[]{"Address", "TEXT", "true"},
            new String[]{"Bank account details", "TEXT", "false"},
            new String[]{"Upload signed offer letter", "FILE", "false"}
         );
      case "leave_request":
         return List.of(
            new String[]{"Employee ID", "TEXT", "true"},
            new String[]{"Department", "TEXT", "false"},
            new String[]{"Contact during leave", "TEXT", "false"},
            new String[]{"Handover person", "TEXT", "false"},
            new String[]{"Attach medical proof", "FILE", "false"}
         );
      case "customer_survey":
         return List.of(
            new String[]{"Your name", "TEXT", "false"},
            new String[]{"Email", "TEXT", "false"},
            new String[]{"Would you recommend us?", "MCQ", "true", "Yes", "No"},
            new String[]{"Product used", "TEXT", "false"},
            new String[]{"Permission to follow up?", "MCQ", "false", "Yes", "No"}
         );
      case "newsletter":
         return List.of(
            new String[]{"Company", "TEXT", "false"},
            new String[]{"Role", "TEXT", "false"},
            new String[]{"Country", "TEXT", "false"},
            new String[]{"Consent to receive emails", "MCQ", "true", "Yes", "No"},
            new String[]{"How did you find us?", "DROPDOWN", "false", "Search", "Social media", "Referral", "Other"},
            new String[]{"Birthday", "DATE", "false"}
         );
      case "volunteer":
         return List.of(
            new String[]{"Age group", "DROPDOWN", "false", "Under 18", "18-25", "26-40", "41+"},
            new String[]{"City", "TEXT", "true"},
            new String[]{"Emergency contact", "TEXT", "true"},
            new String[]{"Can travel?", "MCQ", "false", "Yes", "No"},
            new String[]{"Upload ID proof", "FILE", "false"}
         );
      case "rental_application":
         return List.of(
            new String[]{"Email", "TEXT", "true"},
            new String[]{"Current address", "TEXT", "true"},
            new String[]{"Number of occupants", "TEXT", "true"},
            new String[]{"Pets?", "MCQ", "false", "Yes", "No"},
            new String[]{"Reference contact", "TEXT", "false"}
         );
      case "project_request":
         return List.of(
            new String[]{"Email", "TEXT", "true"},
            new String[]{"Company", "TEXT", "false"},
            new String[]{"Priority", "DROPDOWN", "false", "Low", "Medium", "High"},
            new String[]{"Upload reference file", "FILE", "false"},
            new String[]{"Decision maker", "TEXT", "false"}
         );
      default:
         return List.of(
            new String[]{"Name", "TEXT", "false"},
            new String[]{"Email", "TEXT", "false"},
            new String[]{"Would you recommend us?", "MCQ", "false", "Yes", "No"},
            new String[]{"How often do you use our service?", "DROPDOWN", "false", "Daily", "Weekly", "Monthly", "Rarely"},
            new String[]{"May we contact you?", "MCQ", "false", "Yes", "No"},
            new String[]{"Upload supporting file", "FILE", "false"}
         );
   }
}

private Question addTemplateQuestion(
      Form form,
      String text,
      String type,
      boolean required,
      String... options){

   Question question =
      new Question();

   question.setText(text);
   question.setType(type);
   question.setRequired(required);
   question.setForm(form);
   questionRepo.save(question);

   for(String optionText: options){
      Option option =
         new Option();

      option.setText(optionText);
      option.setQuestion(question);
      optionRepo.save(option);
   }

   return question;
}
public List<Form> getForms(String ownerUsername){
    return formRepo.findByOwnerUsername(ownerUsername);
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

 q.setConditionQuestionId(
  updated.getConditionQuestionId()
 );

 q.setConditionValue(
  updated.getConditionValue()
 );

 return questionRepo.save(q);
}
public void deleteQuestion(
Long id){

 questionRepo.deleteById(id);

}

}
