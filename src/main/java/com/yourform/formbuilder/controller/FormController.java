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
import com.yourform.formbuilder.security.JwtUtil;
import com.yourform.formbuilder.service.FormService;
import io.jsonwebtoken.JwtException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
//import java.util.List;
import com.yourform.formbuilder.model.Option;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
@CrossOrigin(origins="http://localhost:3000")
@RestController
@RequestMapping("/api/forms")
public class FormController {

    private final FormService service;
    private final JwtUtil jwtUtil;

    public FormController(FormService service, JwtUtil jwtUtil) {
        this.service = service;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping
    public Form createForm(
        @RequestHeader("Authorization") String authorization,
        @RequestBody Form form) {

        return service.createForm(form, currentUsername(authorization));
    }

    @PutMapping("/{id}")
    public Form updateForm(
        @RequestHeader("Authorization") String authorization,
        @PathVariable Long id,
        @RequestBody Form form) {

        service.assertFormOwner(id, currentUsername(authorization));
        return service.updateForm(
            id,
            form
        );
    }

    @DeleteMapping("/{id}")
    public void deleteForm(
        @RequestHeader("Authorization") String authorization,
        @PathVariable Long id) {

        service.assertFormOwner(id, currentUsername(authorization));
        service.deleteForm(id);
    }

    @PostMapping("/question")
public Question addQuestion(
        @RequestHeader("Authorization") String authorization,
        @Valid @RequestBody Question question) {

    service.assertFormOwner(question.getForm().getId(), currentUsername(authorization));
    return service.addQuestion(question);
}

    @GetMapping("/{id}")
    public Form getForm(
        @RequestHeader("Authorization") String authorization,
        @PathVariable Long id) {
        service.assertFormOwner(id, currentUsername(authorization));
        return service.getForm(id);
    }

    @GetMapping("/{id}/questions")
public List<QuestionResponseDto> getQuestions(
        @RequestHeader("Authorization") String authorization,
        @PathVariable Long id) {

    service.assertFormOwner(id, currentUsername(authorization));
    return service.getQuestions(id);
}

    @PostMapping("/filtered")
    public List<Question> getFiltered(@RequestBody FilterRequest request) {
    return service.getFilteredQuestions(request);
}
    @GetMapping("/analytics/{formId}")
    public AnalyticsDto analytics(
    @RequestHeader("Authorization") String authorization,
    @PathVariable Long formId) {
    service.assertFormOwner(formId, currentUsername(authorization));
    return service.getAnalytics(formId);
}
    @GetMapping("/insights/{formId}")
    public InsightDto insights(
    @RequestHeader("Authorization") String authorization,
    @PathVariable Long formId) {
    service.assertFormOwner(formId, currentUsername(authorization));
    return service.generateInsights(formId);
}
    @GetMapping("/ai-summary/{formId}")
    public String aiSummary(
    @RequestHeader("Authorization") String authorization,
    @PathVariable Long formId) {
    service.assertFormOwner(formId, currentUsername(authorization));
    return service.generateAiSummary(formId);
}
    @GetMapping("/ai-questions")
    public String aiQuestions(@RequestParam String topic) {
    return service.generateAIQuestions(topic);
}
@GetMapping("/export/{id}")
public String exportCsv(
      @RequestHeader("Authorization") String authorization,
      @PathVariable Long id){

   service.assertFormOwner(id, currentUsername(authorization));
   return service.exportCsv(id);
}
@GetMapping("/chart/{id}")
public ChartDto chart(
   @RequestHeader("Authorization") String authorization,
   @PathVariable Long id){

 service.assertFormOwner(id, currentUsername(authorization));
 return service.getChartData(id);
}
@PostMapping(
 "/questions/{id}/option"
)
public Option addOption(
  @RequestHeader("Authorization") String authorization,
  @PathVariable Long id,

  @RequestParam String text){

 service.assertQuestionOwner(id, currentUsername(authorization));
 return service.addOption(
      id,
      text
 );
}

@GetMapping("/questions/{id}/options")
public List<Option> getOptions(
        @RequestHeader("Authorization") String authorization,
        @PathVariable Long id){

    service.assertQuestionOwner(id, currentUsername(authorization));
    return service.getOptions(id);
}

@PutMapping("/options/{id}")
public Option updateOption(
    @RequestHeader("Authorization") String authorization,
    @PathVariable Long id,
    @RequestBody Option option){

    service.assertOptionOwner(id, currentUsername(authorization));
    return service.updateOption(
        id,
        option
    );
}

@PostMapping("/{id}/clone")
public Form cloneForm(
   @RequestHeader("Authorization") String authorization,
   @PathVariable Long id){

   return service.cloneForm(id, currentUsername(authorization));
}
@PostMapping("/template")
public Form createTemplate(
 @RequestHeader("Authorization") String authorization,
 @RequestParam String type){

 return service
       .createTemplate(
           type,
           currentUsername(authorization)
       );
}
@GetMapping
public List<Form> getForms(
   @RequestHeader("Authorization") String authorization){
   return service.getForms(currentUsername(authorization));
}
@PostMapping("/upload")
public String upload(
@RequestParam MultipartFile file
) throws IOException {
 if (file.isEmpty()) {
    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is required");
 }

 Path uploadDir = Path.of("uploads");
 Files.createDirectories(uploadDir);

 String submittedName = file.getOriginalFilename();
 String originalName = submittedName == null || submittedName.isBlank()
    ? "uploaded-file"
    : Path.of(submittedName).getFileName().toString();
 String storedName = UUID.randomUUID() + "-" + originalName;
 Path destination = uploadDir.resolve(storedName);

 file.transferTo(destination);
 return storedName;
}
@PutMapping("/question/{id}")
public Question updateQuestion(
@RequestHeader("Authorization") String authorization,
@PathVariable Long id,
@RequestBody Question updated){

 service.assertQuestionOwner(id, currentUsername(authorization));
 return service.updateQuestion(
      id,
      updated
 );
}
@DeleteMapping("/question/{id}")
public void deleteQuestion(
@RequestHeader("Authorization") String authorization,
@PathVariable Long id){

 service.assertQuestionOwner(id, currentUsername(authorization));
 service.deleteQuestion(id);
}

@PutMapping("/question/{id}/order")
public void updateOrder(
@RequestHeader("Authorization") String authorization,
@PathVariable Long id,
@RequestBody OrderRequest req){

 service.assertQuestionOwner(id, currentUsername(authorization));
 service.updateOrder(
   id,
   req.getDisplayOrder()
 );
}

private String currentUsername(String authorization) {
    if (authorization == null || !authorization.startsWith("Bearer ")) {
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing auth token");
    }

    try {
        return jwtUtil.extractUsername(authorization.substring(7));
    } catch (JwtException ex) {
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid auth token");
    }
}

}
