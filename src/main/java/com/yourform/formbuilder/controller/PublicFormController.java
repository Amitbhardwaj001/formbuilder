package com.yourform.formbuilder.controller;

import com.yourform.formbuilder.dto.SubmitRequest;
import com.yourform.formbuilder.model.Form;
import com.yourform.formbuilder.service.FormService;
import org.springframework.web.bind.annotation.*;
//import com.yourform.formbuilder.dto.SubmitRequest;

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