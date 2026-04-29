package com.yourform.formbuilder.controller;

import com.yourform.formbuilder.dto.AnalyticsDto;
import com.yourform.formbuilder.dto.ChartDto;
import com.yourform.formbuilder.dto.FilterRequest;
import com.yourform.formbuilder.dto.InsightDto;
import com.yourform.formbuilder.dto.QuestionResponseDto;
//import com.yourform.formbuilder.dto.QuestionResponseDto;

//package com.example.formbuilder.controller;

import com.yourform.formbuilder.model.Form;
import com.yourform.formbuilder.model.Question;
import com.yourform.formbuilder.service.FormService;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/forms")
public class FormController {

    private final FormService service;

    public FormController(FormService service) {
        this.service = service;
    }

    @PostMapping
    public Form createForm(@RequestBody Form form) {
        return service.createForm(form);
    }

    @PostMapping("/question")
public Question addQuestion(
        @Valid @RequestBody Question question) {

    return service.addQuestion(question);
}

    @GetMapping("/{id}")
    public Form getForm(@PathVariable Long id) {
        return service.getForm(id);
    }

    @GetMapping("/{id}/questions")
public List<QuestionResponseDto> getQuestions(
        @PathVariable Long id) {

    return service.getQuestions(id);
}

    @PostMapping("/filtered")
    public List<Question> getFiltered(@RequestBody FilterRequest request) {
    return service.getFilteredQuestions(request);
}
    @GetMapping("/analytics/{formId}")
    public AnalyticsDto analytics(@PathVariable Long formId) {
    return service.getAnalytics(formId);
}
    @GetMapping("/insights/{formId}")
    public InsightDto insights(@PathVariable Long formId) {
    return service.generateInsights(formId);
}
    @GetMapping("/ai-summary/{formId}")
    public String aiSummary(@PathVariable Long formId) {
    return service.generateAiSummary(formId);
}
    @GetMapping("/ai-questions")
    public String aiQuestions(@RequestParam String topic) {
    return service.generateAIQuestions(topic);
}
@GetMapping("/export/{id}")
public String exportCsv(
      @PathVariable Long id){

   return service.exportCsv(id);
}
@GetMapping("/chart/{id}")
public ChartDto chart(
   @PathVariable Long id){

 return service.getChartData(id);
}

}