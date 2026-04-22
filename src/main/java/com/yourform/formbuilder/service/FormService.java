package com.yourform.formbuilder.service;

//package com.example.formbuilder.service;

import com.yourform.formbuilder.model.Form;
import com.yourform.formbuilder.model.Question;
import com.yourform.formbuilder.repository.FormRepository;
import com.yourform.formbuilder.repository.QuestionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FormService {

    private final FormRepository formRepo;
    private final QuestionRepository questionRepo;

    public FormService(FormRepository formRepo, QuestionRepository questionRepo) {
        this.formRepo = formRepo;
        this.questionRepo = questionRepo;
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
}