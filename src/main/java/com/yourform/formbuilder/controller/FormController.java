package com.yourform.formbuilder.controller;

//package com.example.formbuilder.controller;

import com.yourform.formbuilder.model.Form;
import com.yourform.formbuilder.model.Question;
import com.yourform.formbuilder.service.FormService;
import org.springframework.web.bind.annotation.*;

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
    public Question addQuestion(@RequestBody Question question) {
        return service.addQuestion(question);
    }

    @GetMapping("/{id}")
    public Form getForm(@PathVariable Long id) {
        return service.getForm(id);
    }

    @GetMapping("/{id}/questions")
    public List<Question> getQuestions(@PathVariable Long id) {
        return service.getQuestions(id);
    }
}