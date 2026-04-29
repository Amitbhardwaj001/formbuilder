package com.yourform.formbuilder.controller;

import com.yourform.formbuilder.dto.AnalyticsDto;
import com.yourform.formbuilder.dto.ChartDto;
import com.yourform.formbuilder.dto.FilterRequest;
import com.yourform.formbuilder.dto.InsightDto;
import com.yourform.formbuilder.dto.OrderRequest;
import com.yourform.formbuilder.dto.QuestionResponseDto;
//import com.yourform.formbuilder.dto.QuestionResponseDto;

//package com.example.formbuilder.controller;

import com.yourform.formbuilder.model.Form;
import com.yourform.formbuilder.model.Question;
import com.yourform.formbuilder.service.FormService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
//import java.util.List;
import com.yourform.formbuilder.model.Option;

import java.util.List;
@CrossOrigin(origins="http://localhost:3000")
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
@PostMapping(
 "/questions/{id}/option"
)
public String addOption(
 @PathVariable Long id,

 @RequestParam String text){

 return service.addOption(
      id,
      text
 );
}

@GetMapping("/questions/{id}/options")
public List<Option> getOptions(
        @PathVariable Long id){

    return service.getOptions(id);
}
@PostMapping("/{id}/clone")
public Form cloneForm(
   @PathVariable Long id){

   return service.cloneForm(id);
}
@PostMapping("/template")
public Form createTemplate(
 @RequestParam String type){

 return service
       .createTemplate(
           type
       );
}
@GetMapping
public List<Form> getForms(){
   return service.getForms();
}
@PostMapping("/upload")
public String upload(
@RequestParam MultipartFile file
){
 return file.getOriginalFilename();
}
@PutMapping("/question/{id}")
public Question updateQuestion(
@PathVariable Long id,
@RequestBody Question updated){

 return service.updateQuestion(
      id,
      updated
 );
}
@DeleteMapping("/question/{id}")
public void deleteQuestion(
@PathVariable Long id){

 service.deleteQuestion(id);
}

@PutMapping("/question/{id}/order")
public void updateOrder(
@PathVariable Long id,
@RequestBody OrderRequest req){

 service.updateOrder(
   id,
   req.getDisplayOrder()
 );
}

}