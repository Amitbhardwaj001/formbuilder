package com.yourform.formbuilder.controller;

import com.yourform.formbuilder.dto.SubmitRequest;
import com.yourform.formbuilder.dto.QuestionResponseDto;
import com.yourform.formbuilder.model.Form;
import com.yourform.formbuilder.service.FormService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
//import com.yourform.formbuilder.dto.SubmitRequest;

@CrossOrigin(origins="http://localhost:3000")
@RestController
@RequestMapping("/public/forms")
public class PublicFormController {

 private final FormService service;

 public PublicFormController(
      FormService service){
   this.service=service;
 }

 @GetMapping("/{token}")
 public Form getSharedForm(
    @PathVariable String token){

   return service.getFormByToken(
        token
   );
 }

 @GetMapping("/{token}/questions")
 public List<QuestionResponseDto> getSharedQuestions(
    @PathVariable String token){

   Form form = service.getFormByToken(token);
   return service.getQuestions(form.getId());
 }

 @PostMapping("/{token}/submit")
public String submitPublicResponse(
      @PathVariable String token,
      @RequestBody SubmitRequest request){

    return service.submitPublicForm(
            token,
            request
    );
}
}
